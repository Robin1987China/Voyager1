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

package io.voyager1.build;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.util.ZipUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.CompressUtil;
import io.voyager1.util.Archiver;
import io.voyager1.Voyager1Application;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.RepositoryModel;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.Charset;
import java.util.function.BiFunction;

/**
 * 构建工具类
 *
 * @since 2019/7/19
 */
public class BuildUtil {

    public static Long buildCacheSize = 0L;

    public static final String USE_TAR_GZ = "USE_TAR_GZ";

    /**
     * 刷新存储文件大小
     */
    public static void reloadCacheSize() {
        File buildDataDir = BuildUtil.getBuildDataDir();
        BuildUtil.buildCacheSize = FileUtil.size(buildDataDir);
        //
    }

    public static File getBuildDataFile(String id) {
        return FileUtil.file(getBuildDataDir(), id);
    }

//	/**
//	 * 获取代码路径
//	 *
//	 * @param buildModel 实体
//	 * @return file
//	 * @see BuildUtil#getSourceById
//	 */
//	@Deprecated
//	public static File getSource(BuildModel buildModel) {
//		return FileUtil.file(BuildUtil.getBuildDataFile(buildModel.getId()), "source");
//	}

    /**
     * @param id 构建ID
     * @return file
     * 新版本获取代码路径
     * @since 2021-08-22
     */
    public static File getSourceById(String id) {
        return FileUtil.file(BuildUtil.getBuildDataFile(id), "source");
    }

    public static File getBuildDataDir() {
        return FileUtil.file(Voyager1Application.getInstance().getDataPath(), "build");
    }

    /**
     * 获取构建产物存放路径
     *
     * @param buildModelId 构建实体
     * @param buildId      id
     * @param resultFile   结果目录
     * @return file
     */
    public static File getHistoryPackageFile(String buildModelId, int buildId, String resultFile) {
        if (buildId <= 0) {
            // 没有 0 号构建id，避免生成 #0 文件夹
            return null;
        }
        if ((buildModelId == null || buildModelId.isEmpty()) || (resultFile == null || resultFile.isEmpty())) {
            return null;
        }
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultFile);
        ResultDirFileAction.Type type = resultDirFileAction.getType();
        if (type == ResultDirFileAction.Type.ANT_PATH) {
            // ANT 模式 不能直接获取，避免提前创建文件夹
            return null;
        }
        File result = FileUtil.file(getBuildDataFile(buildModelId), "history", BuildInfoModel.getBuildIdStr(buildId), "result");
        return FileUtil.file(result, resultFile);
    }

    /**
     * 插件构建产物存放路径
     *
     * @param buildModelId 构建实体
     * @param buildId      id
     */
    public static void mkdirHistoryPackageFile(String buildModelId, int buildId) {
        File result = FileUtil.file(getBuildDataFile(buildModelId), "history", BuildInfoModel.getBuildIdStr(buildId), "result");
        FileUtil.mkdir(result);
    }

    /**
     * 获取构建产物存放路径
     *
     * @param buildModelId 构建实体
     * @param buildId      id
     * @return file
     */
    public static File getHistoryPackageZipFile(String buildModelId, int buildId) {
        return FileUtil.file(getBuildDataFile(buildModelId),
            "history",
            BuildInfoModel.getBuildIdStr(buildId),
            "zip");
    }

    /**
     * 获取日志记录文件
     *
     * @param buildModelId buildModelId
     * @param buildId      构建编号
     * @return file
     */
    public static File getLogFile(String buildModelId, int buildId) {
        if ((buildModelId == null || buildModelId.isEmpty())) {
            return null;
        }
        return FileUtil.file(getBuildDataFile(buildModelId),
            "history",
            BuildInfoModel.getBuildIdStr(buildId),
            "info.log");
    }

    /**
     * 如果为文件夹自动打包为zip ,反之返回null
     *
     * @param file file
     * @return 压缩包文件
     */
    private static File isDirPackage(String id, int buildNumberId, File file, boolean tarGz) {
        Assert.state(file != null && file.exists(), "产物文件不存在");
        if (file.isFile()) {
            return null;
        }
        Assert.state(!FileUtil.isDirEmpty(file), "文件夹为空,不能打包 #" + buildNumberId);
        String name = FileUtil.getName(file);
        // 如果产物配置 / 时无法获取文件名，采用 result
        name = (name == null || name.isEmpty() ? "result" : name);
        // 保存目录存放值 history 路径
        File packageFile = BuildUtil.getHistoryPackageZipFile(id, buildNumberId);
        File zipFile = tarGz ? FileUtil.file(packageFile, name + ".tar.gz") : FileUtil.file(packageFile, name + ".zip");
        // 不存在则打包
        if (tarGz) {
            try (Archiver archiver = CompressUtil.createArchiver(Charset.defaultCharset(), "tar.gz", zipFile)) {
                archiver.add(file);
            }
        } else {
            ZipUtil.zip(file.getAbsolutePath(), zipFile.getAbsolutePath());
        }
        return zipFile;
    }

    /**
     * 如果为文件夹自动打包为zip ,反之返回null
     *
     * @param file          file
     * @param id            构建Id
     * @param buildNumberId 构建序号
     * @param tarGz         是否打包 为 tar
     * @param consumer      文件回调
     * @return 执行结果
     */
    public static <T> T loadDirPackage(String id, int buildNumberId, File file, boolean tarGz, BiFunction<Boolean, File, T> consumer) {
        File dirPackage = isDirPackage(id, buildNumberId, file, tarGz);
        if (dirPackage == null) {
            return consumer.apply(false, file);
        } else {
            return consumer.apply(true, dirPackage);
        }
    }


    /**
     * get rsa file
     *
     * @param path 文件名
     * @return file
     */
    public static File getRepositoryRsaFile(String path) {
        File sshDir = FileUtil.file(Voyager1Application.getInstance().getDataPath(), ServerConst.SSH_KEY);
        return FileUtil.file(sshDir, path);
    }

    /**
     * get rsa file
     *
     * @param repositoryModel 仓库
     * @return 文件
     */
    public static File getRepositoryRsaFile(RepositoryModel repositoryModel) {
        if ((repositoryModel.getRsaPrv() == null || repositoryModel.getRsaPrv().isEmpty())) {
            return null;
        }
        // ssh
        File rsaFile;
        if (StrUtil.startWith(repositoryModel.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
            String rsaPath = StrUtil.removePrefix(repositoryModel.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
            rsaFile = FileUtil.file(rsaPath);
        } else {
            if ((repositoryModel.getId() == null || repositoryModel.getId().isEmpty())) {
                rsaFile = FileUtil.file(Voyager1Application.getInstance().getTempPath(), ServerConst.SSH_KEY, DigestUtil.sha1(repositoryModel.getGitUrl()) + ServerConst.ID_RSA);
            } else {
                rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel.getId() + ServerConst.ID_RSA);
            }
            // 写入
            FileUtil.writeUtf8String(repositoryModel.getRsaPrv(), rsaFile);
        }
        Assert.state(FileUtil.isFile(rsaFile), "仓库密钥文件不存在或者异常,请检查后操作");
        return rsaFile;
    }
}
