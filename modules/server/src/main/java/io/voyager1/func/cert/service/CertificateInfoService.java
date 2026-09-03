/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.func.cert.service;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.GlobalBouncyCastleProvider;
import io.voyager1.util.KeyUtil;
import io.voyager1.util.PemUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.ECIES;
import io.voyager1.util.KeyType;
import io.voyager1.util.RSA;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.cert.model.CertificateInfoModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.core.entity.CertificateInfoEntity;
import io.voyager1.core.jpa.JpaGlobalOrWorkspaceService;
import io.voyager1.core.repository.CertificateInfoRepository;
import io.voyager1.service.IStatusRecover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.security.cert.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 2023/3/22
 */
@Service
@Slf4j
public class CertificateInfoService extends JpaGlobalOrWorkspaceService<CertificateInfoModel, CertificateInfoEntity> implements IStatusRecover {

    static {
        GlobalBouncyCastleProvider.setUseBouncyCastle(false);
    }

    private final Voyager1Application voyager1Application;
    private final MachineDockerServer machineDockerServer;
    private final CertificateInfoRepository certificateInfoRepository;

    public CertificateInfoService(Voyager1Application voyager1Application,
                                  MachineDockerServer machineDockerServer,
                                  CertificateInfoRepository certificateInfoRepository) {
        this.voyager1Application = voyager1Application;
        this.machineDockerServer = machineDockerServer;
        this.certificateInfoRepository = certificateInfoRepository;
    }

    @Override
    protected JpaRepository<CertificateInfoEntity, String> repository() {
        return certificateInfoRepository;
    }

    @Override
    protected JpaSpecificationExecutor<CertificateInfoEntity> specExecutor() {
        return certificateInfoRepository;
    }

    @Override
    protected Class<CertificateInfoEntity> entityClass() {
        return CertificateInfoEntity.class;
    }

    @Override
    protected Class<CertificateInfoModel> modelClass() {
        return CertificateInfoModel.class;
    }

    @Override
    public int statusRecover() {
        Entity entity = Entity.create();
        entity.set("tlsVerify", true);
        entity.set("certInfo", null);
        List<MachineDockerModel> dockerModels = machineDockerServer.listByEntity(entity);
        if ((dockerModels == null || dockerModels.isEmpty())) {
            return 0;
        }
        for (MachineDockerModel dockerModel : dockerModels) {
            try {
                String generateCertPath = dockerModel.generateCertPath();
                File file = FileUtil.file(generateCertPath);
                CertificateInfoModel certificateInfoModel = this.resolveX509(file, false);
                if (!this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType())) {
                    certificateInfoModel.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
                    certificateInfoModel.setCreateUser(UserModel.SYSTEM_ADMIN);
                    certificateInfoModel.setModifyUser(UserModel.SYSTEM_ADMIN);
                    String description = String.format("docker[%s] 资产导入", dockerModel.getName());
                    certificateInfoModel.setDescription(description);
                    this.insert(certificateInfoModel);
                }
                // 更新
                MachineDockerModel update = new MachineDockerModel();
                update.setId(dockerModel.getId());
                update.setCertInfo(certificateInfoModel.getSerialNumberStr() + ":" + certificateInfoModel.getKeyType());
                machineDockerServer.updateById(update);
                log.info("docker[{}] 证书成功迁移到证书管理中", dockerModel.getName());
            } catch (Exception e) {
                log.error("迁移 docker[{}] 证书发生异常", dockerModel.getName(), e);
            }
        }
        return (dockerModels == null ? 0 : dockerModels.size());
    }

    /**
     * 解析 x509 证书
     *
     * @param dir         证书目录
     * @param checkRepeat 是否判断重复
     * @return 证书对象
     */
    public CertificateInfoModel resolveX509(File dir, boolean checkRepeat) {
        String[] keyNameSuffixes = new String[]{"key.pem", ".key"};
        String[] pemNameSuffixes = new String[]{".crt", ".cer", ".pem"};
        // 找到 对应的文件
        File[] files = dir.listFiles();
        Assert.notNull(files, "压缩包里没有任何文件");
        File keyFile = Arrays.stream(files).filter(file -> StrUtil.endWithAnyIgnoreCase(file.getName(), keyNameSuffixes)).findAny().orElse(null);
        Assert.notNull(keyFile, "压缩包里没有找到私钥文件");
        //
        try {
            List<File> fileList = Arrays.stream(files)
                .filter(file -> !FileUtil.equals(file, keyFile))
                .filter(file -> StrUtil.endWithAnyIgnoreCase(file.getName(), pemNameSuffixes))
                .collect(Collectors.toList());
            Assert.notEmpty(fileList, "没有找到任何证书文件");
            Assert.state(fileList.size() <= 2, "找到 2 个以上的证书文件");
            //
            List<Certificate> certificates = fileList.stream()
                .map(file -> {
                    try (BufferedInputStream inputStream = FileUtil.getInputStream(file)) {
                        return KeyUtil.readX509Certificate(inputStream);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                })
                .collect(Collectors.toList());
            Certificate certificate0 = certificates.get(0);
            Certificate certificate1 = (1 < certificates.size() ? certificates.get(1) : null);
            X509Certificate x509Certificate0 = getInstance(certificate0.getEncoded());
            X509Certificate x509Certificate1 = certificate1 != null ? getInstance(certificate1.getEncoded()) : null;
            Principal issuerDN = x509Certificate0.getIssuerDN();
            Principal subjectDN = x509Certificate0.getSubjectDN();
            Assert.state(issuerDN != null && subjectDN != null, "证书信息出现错误,未找到 issuerDN 或者 subjectDN");
            int rootIndex = java.util.Objects.equals(issuerDN.getName(), subjectDN.getName()) ? 0 : 1;
            //
            PrivateKey privateKey;
            try (BufferedInputStream inputStream = FileUtil.getInputStream(keyFile)) {
                privateKey = PemUtil.readPemPrivateKey(inputStream);
            }
            // 验证证书公钥和私钥
            PublicKey publicKey = Optional.ofNullable(rootIndex == 0 ? certificate1 : certificate0)
                .map(Certificate::getPublicKey)
                .orElse(null);
            this.testKey(publicKey, privateKey);
            //
            X509Certificate pubCert = (rootIndex == 0 ? x509Certificate1 : x509Certificate0);
            if (certificate1 != null) {
                // 验证证书链
                pubCert.verify((rootIndex == 0 ? certificate0 : certificate1).getPublicKey());
            }
            // 填充
            CertificateInfoModel certificateInfoModel = this.filling(pubCert);
            // 类型已经确定
            certificateInfoModel.setKeyType(certificate0.getType());
            // 判断是否存在
            if (checkRepeat) {
                //                this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType());
                Assert.state(!this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType()),
                    "当前证书已经存在啦(系统全局范围内)");
            }
            // 保存文件
            File file1 = this.getFilePath(certificateInfoModel);
            FileUtil.mkdir(file1);
            // 避免文件夹已经存在
            FileUtil.clean(file1);
            FileUtil.copyFile(keyFile, file1, StandardCopyOption.REPLACE_EXISTING);
            for (File file : fileList) {
                FileUtil.copyFile(file, file1, StandardCopyOption.REPLACE_EXISTING);
            }
            return certificateInfoModel;
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw Lombok.sneakyThrow(e);
        } catch (Exception e) {
            log.error("解析证书异常", e);
            throw new IllegalStateException("解析证书发生未知错误：" + e.getMessage());
        }
    }

    public X509Certificate getInstance(byte[]  bytes) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bytes));
    }

    public File getFilePath(CertificateInfoModel model) {
        File certificatePath = FileUtil.file(voyager1Application.getDataPath(), "certificate");
        return FileUtil.file(certificatePath, model.getSerialNumberStr(), model.getKeyType());
    }

    public File getFilePath(String certTag) {
        CertificateInfoModel byCertTag = this.getByCertTag(certTag);
        if (byCertTag == null) {
            return null;
        }
        return this.getFilePath(byCertTag);
    }

    /**
     * 判断证书是否存在
     *
     * @param serialNumber 证书编号
     * @param type         证书类型
     */
    public boolean checkRepeat(String serialNumber, String type) {
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setSerialNumberStr(serialNumber);
        certificateInfoModel.setKeyType(type);
        return this.exists(certificateInfoModel);
    }

    /**
     * 查询证书
     *
     * @param certTag 证书标记
     */
    public CertificateInfoModel getByCertTag(String certTag) {
        if ((certTag == null || certTag.isEmpty())) {
            return null;
        }
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(certTag, ":");
        String serialNumberStr = (0 < list.size() ? list.get(0) : null);
        String keyType = (1 < list.size() ? list.get(1) : null);
        Assert.hasText(serialNumberStr, "没有证书序列号");
        Assert.hasText(keyType, "没有证书类型");
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setSerialNumberStr(serialNumberStr);
        certificateInfoModel.setKeyType(keyType);
        List<CertificateInfoModel> infoModels = this.listByBean(certificateInfoModel);
        return (infoModels == null || infoModels.isEmpty() ? null : infoModels.get(0));
    }

    /**
     * 验证 公钥和私钥是否匹配
     *
     * @param pubkey     公钥
     * @param privateKey 私钥
     */
    public void testKey(PublicKey pubkey, PrivateKey privateKey) {
        Assert.state(pubkey != null && privateKey != null, "公钥或者私钥不存在");
        // 测试字符串
        String str = "您好，Voyager1";

        // 判断算法名称是否包含 “RSA” 或 “EC”
        String algorithm = pubkey.getAlgorithm();
        if (algorithm.contains(ServerConst.RSA)) {
            RSA rsa = new RSA(privateKey, pubkey);
            String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
            String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
            Assert.state(java.util.Objects.equals(str, decryptStr), "公钥和私钥不匹配");
        } else if (algorithm.contains(ServerConst.EC)) {
            ECIES ecies = new ECIES(privateKey, pubkey);
            String encryptStr = ecies.encryptBase64(str, KeyType.PublicKey);
            String decryptStr = StrUtil.utf8Str(ecies.decrypt(encryptStr, KeyType.PrivateKey));
            Assert.state(java.util.Objects.equals(str, decryptStr), "公钥和私钥不匹配");
        }
    }

    /**
     * 获取证书信息
     *
     * @param cert 证书公钥
     * @return data
     */
    public CertificateInfoModel filling(X509Certificate cert) throws CertificateEncodingException {

        //String algorithm = cert.getPublicKey().getAlgorithm();
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        Date notBefore = cert.getNotBefore();
        Date notAfter = cert.getNotAfter();
        Optional.ofNullable(notAfter).ifPresent(date -> certificateInfoModel.setExpirationTime(date.getTime()));
        Optional.ofNullable(notBefore).ifPresent(date -> certificateInfoModel.setEffectiveTime(date.getTime()));
        BigInteger serialNumber = cert.getSerialNumber();
        // 使用 16 进制
        certificateInfoModel.setSerialNumberStr(serialNumber.toString(16));
        byte[] encoded = cert.getEncoded();
        certificateInfoModel.setFingerprint(DigestUtil.sha1(encoded));
        //
        int version = cert.getVersion();
        certificateInfoModel.setCertVersion(version);
        Optional.ofNullable(cert.getSubjectDN()).ifPresent(principal -> certificateInfoModel.setSubjectDnName(principal.getName()));
        Optional.ofNullable(cert.getIssuerDN()).ifPresent(principal -> certificateInfoModel.setIssuerDnName(principal.getName()));
        String sigAlgOID = cert.getSigAlgOID();
        String sigAlgName = cert.getSigAlgName();
        certificateInfoModel.setSigAlgName(sigAlgName);
        certificateInfoModel.setSigAlgOid(sigAlgOID);
        return certificateInfoModel;
    }

    public PageResultDto<CertificateInfoModel> listPageAll(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        paramMap.remove("workspaceId");
        return super.listPage(paramMap);
    }
}
