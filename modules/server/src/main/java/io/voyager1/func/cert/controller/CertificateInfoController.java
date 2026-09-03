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

package io.voyager1.func.cert.controller;

import io.voyager1.util.FileUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.ZipUtil;
import io.voyager1.util.KeyUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.controller.outgiving.OutGivingWhitelistService;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.cert.model.CertificateInfoModel;
import io.voyager1.func.cert.service.CertificateInfoService;
import io.voyager1.func.files.service.FileReleaseTaskService;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * @since 2023/3/22
 */
@RestController
@RequestMapping(value = "/certificate/")
@Feature(cls = ClassFeature.CERTIFICATE_INFO)
@Slf4j
public class CertificateInfoController extends BaseServerController {


    private final ServerConfig serverConfig;
    private final MachineDockerServer machineDockerServer;
    private final CertificateInfoService certificateInfoService;
    private final FileReleaseTaskService fileReleaseTaskService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final FileStorageService fileStorageService;

    public CertificateInfoController(ServerConfig serverConfig,
                                     MachineDockerServer machineDockerServer,
                                     CertificateInfoService certificateInfoService,
                                     FileReleaseTaskService fileReleaseTaskService,
                                     OutGivingWhitelistService outGivingWhitelistService,
                                     FileStorageService fileStorageService) {
        this.serverConfig = serverConfig;
        this.machineDockerServer = machineDockerServer;
        this.certificateInfoService = certificateInfoService;
        this.fileReleaseTaskService = fileReleaseTaskService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<CertificateInfoModel>> list(HttpServletRequest request) {
        //
        PageResultDto<CertificateInfoModel> listPage = certificateInfoService.listPage(request);
        listPage.each(certificateInfoModel -> {
            File file = certificateInfoService.getFilePath(certificateInfoModel);
            certificateInfoModel.setFileExists(!FileUtil.isEmpty(file));
        });
        return ApiResult.success("", listPage);
    }

    /**
     * 查询所有分页列表
     *
     * @return json
     */
    @PostMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    @SystemPermission
    public ApiResult<PageResultDto<CertificateInfoModel>> listAll(HttpServletRequest request) {
        //
        PageResultDto<CertificateInfoModel> listPage = certificateInfoService.listPageAll(request);
        listPage.each(certificateInfoModel -> {
            File file = certificateInfoService.getFilePath(certificateInfoModel);
            certificateInfoModel.setFileExists(!FileUtil.isEmpty(file));
        });
        return ApiResult.success("", listPage);
    }

    @PostMapping(value = "import-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<String> importFile(MultipartFile file, @ValidatorItem String type, String password) {
        Assert.notNull(file, "没有上传文件");
        String filename = file.getOriginalFilename();
        Assert.notNull(filename, "没有文件名");
        File tempPath = FileUtil.file(serverConfig.getUserTempPath(), "cert", java.util.UUID.randomUUID().toString().replace("-", ""));
        CertificateInfoModel certificateInfoModel;
        try {
            switch (type) {
                case KeyUtil.KEY_TYPE_PKCS12:
                case KeyUtil.KEY_TYPE_JKS:
                    certificateInfoModel = this.resolvePkcs12OrJks(file, password, tempPath, type);
                    break;
                case KeyUtil.CERT_TYPE_X509:
                    certificateInfoModel = this.resolveX509(file, tempPath);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的模式：" + type);
            }
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        } finally {
            FileUtil.file(tempPath);
        }
        certificateInfoService.insert(certificateInfoModel);
        return ApiResult.success("上传成功");
    }


    /**
     * 解析 x509 证书
     *
     * @param multipartFile 上传的文件
     * @param tempPath      临时保存目录
     * @return 证书对象
     * @throws IOException io
     */
    private CertificateInfoModel resolveX509(MultipartFile multipartFile,
                                             File tempPath) throws IOException {
        FileUtil.mkdir(tempPath);
        String filename = multipartFile.getOriginalFilename();
        String extName = FileUtil.extName(filename);
        Assert.state((extName != null && extName.toLowerCase().contains("zip".toLowerCase())), "上传的文件不是 zip");
        File saveFile = FileUtil.file(tempPath, filename);
        multipartFile.transferTo(saveFile);
        ZipUtil.unzip(saveFile, tempPath);
        return certificateInfoService.resolveX509(tempPath, true);
    }

    /**
     * 解析 pfx /jks 证书
     *
     * @param multipartFile 上传的文件
     * @param password      密码
     * @param tempPath      临时保存目录
     * @return 证书对象
     * @throws IOException io
     */
    private CertificateInfoModel resolvePkcs12OrJks(MultipartFile multipartFile,
                                                    String password, File tempPath,
                                                    String type) throws IOException {
        String suffix;
        switch (type) {
            case KeyUtil.KEY_TYPE_JKS:
                suffix = "jks";
                break;
            case KeyUtil.KEY_TYPE_PKCS12:
            default:
                suffix = "pfx";
                break;
        }
        FileUtil.mkdir(tempPath);
        String filename = multipartFile.getOriginalFilename();
        String extName = FileUtil.extName(filename);
        File saveFile = FileUtil.file(tempPath, filename);
        File pfxFile = null;
        String newPassword = password;
        FileUtil.del(saveFile);
        if ((extName != null && extName.equalsIgnoreCase(suffix))) {
            multipartFile.transferTo(saveFile);
            pfxFile = saveFile;
        } else if ((extName != null && extName.equalsIgnoreCase("zip"))) {
            multipartFile.transferTo(saveFile);
            ZipUtil.unzip(saveFile, tempPath);
            // 找到 suffix
            File[] files = tempPath.listFiles();
            Assert.notEmpty(files, "压缩包里没有任何文件");
            for (File file1 : files) {
                String extName2 = FileUtil.extName(file1);
                if (pfxFile == null && (extName2 != null && extName2.equalsIgnoreCase(suffix))) {
                    pfxFile = file1;
                }
                if ((newPassword == null || newPassword.isEmpty()) && (extName2 != null && extName2.equalsIgnoreCase("txt"))) {
                    newPassword = FileUtil.readString(file1, StandardCharsets.UTF_8);
                }
            }
        } else {
            throw new IllegalArgumentException("不支持的文件格式");
        }
        Assert.notNull(pfxFile, String.format("没有找到 %s 文件", suffix));
        try {
            char[] passwordChars = (newPassword == null || newPassword.isEmpty() ? "" : newPassword).toCharArray();
            KeyStore keyStore = java.util.Objects.equals(suffix, "jks") ? KeyUtil.readJKSKeyStore(pfxFile, passwordChars) : KeyUtil.readPKCS12KeyStore(pfxFile, passwordChars);
            Enumeration<String> aliases = keyStore.aliases();
            // we are readin just one certificate.
            if (aliases.hasMoreElements()) {
                //
                String keyAlias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(keyAlias);
                PrivateKey prikey = (PrivateKey) keyStore.getKey(keyAlias, passwordChars);
                PublicKey pubkey = certificate.getPublicKey();
                certificateInfoService.testKey(pubkey, prikey);
                //
                X509Certificate cert = certificateInfoService.getInstance(certificate.getEncoded());
                // 填充
                CertificateInfoModel certificateInfoModel = certificateInfoService.filling(cert);
                certificateInfoModel.setKeyType(keyStore.getType());
                certificateInfoModel.setKeyAlias(keyAlias);
                // 判断是否存在
                Assert.state(!certificateInfoService.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType()),
                    "当前证书已经存在啦(系统全局范围内)");
                //certificateInfoService.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType());

                certificateInfoModel.setCertPassword(newPassword);
                //
                File file1 = certificateInfoService.getFilePath(certificateInfoModel);
                FileUtil.mkdir(file1);
                // 避免文件夹已经存在
                FileUtil.clean(file1);
                FileUtil.move(pfxFile, file1, true);
                return certificateInfoModel;
            } else {
                throw new IllegalStateException("证书没有任何：aliases");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw Lombok.sneakyThrow(e);
        } catch (Exception e) {
            log.error("解析证书异常", e);
            throw new IllegalStateException("解析证书发生未知错误：" + e.getMessage());
        }
    }


    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(@ValidatorItem String id, HttpServletRequest request) throws IOException {
        CertificateInfoModel model = certificateInfoService.getByKeyAndGlobal(id, request);
        // 判断是否被 docker 使用
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setCertInfo(model.getSerialNumberStr() + ":" + model.getKeyType());
        long count = machineDockerServer.count(machineDockerModel);
        Assert.state(count == 0, "当前证书被 docker 关联中,不能直接删除");
        //
        File file = certificateInfoService.getFilePath(model);
        FileUtil.del(file);
        if (FileUtil.isEmpty(file.getParentFile())) {
            // 一并删除避免保留空文件夹
            FileUtil.del(file.getParentFile());
        }
        //
        certificateInfoService.delByKey(id);
        return ApiResult.success("删除成功");
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> edit(@ValidatorItem String id,
                                     String description,
                                     HttpServletRequest request) throws IOException {
        // 验证权限
        certificateInfoService.getByKeyAndGlobal(id, request);

        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setId(id);
        certificateInfoModel.setDescription(description);
        //
        certificateInfoModel.setWorkspaceId(certificateInfoService.covertGlobalWorkspace(request));
        certificateInfoService.updateById(certificateInfoModel);
        return ApiResult.success("修改成功");
    }

    /**
     * 导出证书
     *
     * @param id 项目id
     */
    @GetMapping(value = "export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void export(@ValidatorItem String id, HttpServletRequest request, HttpServletResponse response) {
        CertificateInfoModel model = certificateInfoService.getByKeyAndGlobal(id, request);
        File file = certificateInfoService.getFilePath(model);
        Assert.state(!FileUtil.isEmpty(file), "证书文件丢失");

        File userTempPath = serverConfig.getUserTempPath();
        File tempSave = FileUtil.file(userTempPath, java.util.UUID.randomUUID().toString().replace("-", ""));
        try {
            FileUtil.mkdir(tempSave);
            String absolutePath = FileUtil.file(tempSave, model.getSerialNumberStr() + ".zip").getAbsolutePath();
            File zip = ZipUtil.zip(file.getAbsolutePath(), absolutePath, false);
            JakartaServletUtil.write(response, zip);
        } finally {
            FileUtil.del(tempSave);
        }
    }

    @PostMapping(value = "deploy", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> addTask(@ValidatorItem String id,
                                        @ValidatorItem String name,
                                        @ValidatorItem(value = ValidatorRule.NUMBERS) int taskType,
                                        @ValidatorItem String taskDataIds,
                                        @ValidatorItem String releasePathParent,
                                        @ValidatorItem String releasePathSecondary,
                                        String beforeScript,
                                        String afterScript,
                                        HttpServletRequest request) {
        // 判断参数
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, releasePathParent), "请选择正确的项目路径,或者还没有配置授权");
        Assert.hasText(releasePathSecondary, "请填写发布文件的二级目录");
        // 判断证书是否存在
        CertificateInfoModel model = certificateInfoService.getByKeyAndGlobal(id, request);
        File file = certificateInfoService.getFilePath(model);
        Assert.state(!FileUtil.isEmpty(file), "证书文件丢失");
        File userTempPath = serverConfig.getUserTempPath();
        File tempSave = FileUtil.file(userTempPath, java.util.UUID.randomUUID().toString().replace("-", ""));
        try {
            // 压缩成 zip
            FileUtil.mkdir(tempSave);
            String absolutePath = FileUtil.file(tempSave, model.getSerialNumberStr() + ".zip").getAbsolutePath();
            File zip = ZipUtil.zip(file.getAbsolutePath(), absolutePath, false);
            // 添加到文件中心
            String description = model.getSerialNumberStr() + Optional.ofNullable(model.getDescription()).map(s -> "," + s).orElse("");
            String fileId = fileStorageService.addFile(zip, 3, certificateInfoService.getCheckUserWorkspace(request), description, null, 1);
            String releasePath = FileUtil.normalize(releasePathParent + "/" + releasePathSecondary);
            // 创建发布任务
            Map<String, String> env = new HashMap<>();
            env.put("CERT_SERIAL_NUMBER_STR", model.getSerialNumberStr());
            fileReleaseTaskService.addTask(fileId, 1, name, taskType, taskDataIds, releasePath, beforeScript, afterScript, env, request);
            return ApiResult.success("创建成功");
        } finally {
            FileUtil.del(tempSave);
        }
    }
}
