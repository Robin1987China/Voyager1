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

package io.voyager1.func.files.service;

import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StreamProgress;
import org.springframework.util.unit.DataSize;
import io.voyager1.util.IdUtil;
import io.voyager1.util.NumberUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.HttpUtil;
import io.voyager1.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.core.entity.FileStorageEntity;
import io.voyager1.core.jpa.JpaGlobalOrWorkspaceService;
import io.voyager1.core.repository.FileStorageRepository;
import io.voyager1.func.files.model.FileStorageModel;
import io.voyager1.service.IStatusRecover;
import io.voyager1.service.ITriggerToken;
import io.voyager1.system.ServerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 2023/3/16
 */
@Service
@Slf4j
public class FileStorageService extends JpaGlobalOrWorkspaceService<FileStorageModel, FileStorageEntity> implements ISystemTask, IStatusRecover, ITriggerToken {

    private final ServerConfig serverConfig;
    private final Voyager1Application configBean;
    private final BuildExtConfig buildExtConfig;
    private final FileStorageRepository fileStorageRepository;

    public FileStorageService(ServerConfig serverConfig,
                              Voyager1Application configBean,
                              BuildExtConfig buildExtConfig,
                              FileStorageRepository fileStorageRepository) {
        this.serverConfig = serverConfig;
        this.configBean = configBean;
        this.buildExtConfig = buildExtConfig;
        this.fileStorageRepository = fileStorageRepository;
    }

    @Override
    protected JpaRepository<FileStorageEntity, String> repository() {
        return fileStorageRepository;
    }

    @Override
    protected JpaSpecificationExecutor<FileStorageEntity> specExecutor() {
        return fileStorageRepository;
    }

    @Override
    protected Class<FileStorageEntity> entityClass() {
        return FileStorageEntity.class;
    }

    @Override
    protected Class<FileStorageModel> modelClass() {
        return FileStorageModel.class;
    }

    /**
     * 远程下载
     *
     * @param url         url
     * @param workspaceId 工作空间id
     * @param description 描述
     * @param global      是否为全局共享
     * @param keepDay     保留天数
     */
    public void download(String url, Boolean global, String workspaceId, Integer keepDay, String description, String aliasCode) {
        FileStorageModel fileStorageModel = new FileStorageModel();
        // 临时使用 uuid，代替
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();
        {
            fileStorageModel.setId(uuid);
            fileStorageModel.setName("文件下载中");
            String empty = (description == null || description.isEmpty() ? "" : description);

            fileStorageModel.setDescription(String.format("%s 远程下载 url：%s", empty, url));
            String extName = "download";
            String path = String.format("/%s/%s.%s", DateTime.now().toString("yyyyMMdd"), uuid, extName);
            fileStorageModel.setExtName("download");
            fileStorageModel.setPath(path);
            fileStorageModel.setAliasCode(aliasCode);
            fileStorageModel.setSize(0L);
            fileStorageModel.setStatus(0);
            fileStorageModel.setSource(2);
            if (global != null && global) {
                fileStorageModel.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
            } else {
                fileStorageModel.setWorkspaceId(workspaceId);
            }
            fileStorageModel.validUntil(keepDay, null);
            this.insert(fileStorageModel);
        }
        // 异步下载
        I18nThreadUtil.execute(() -> {
            try {
                File tempPath = configBean.getTempPath();
                File file = FileUtil.file(tempPath, "file-storage-download", uuid);
                FileUtil.mkdir(file);
                StreamProgress streamProgress = this.createStreamProgress(uuid);
                File fileFromUrl = HttpUtil.downloadFileFromUrl(url, file, -1, streamProgress);
                String md5 = DigestUtil.md5(fileFromUrl);
                FileStorageModel storageModel = this.getByKey(md5);
                if (storageModel != null) {
                    this.updateError(uuid, "文件已经存在啦");
                    FileUtil.del(fileFromUrl);
                    return;
                }
                String extName = FileUtil.extName(fileFromUrl);
                // 避免跨天数据
                String path = String.format("/%s/%s.%s", new DateTime(startTime).toString("yyyyMMdd"), md5, extName);
                File storageSavePath = serverConfig.fileStorageSavePath();
                File fileStorageFile = FileUtil.file(storageSavePath, path);
                FileUtil.mkParentDirs(fileStorageFile);
                FileUtil.move(fileFromUrl, fileStorageFile, true);
                //
                FileStorageModel update = new FileStorageModel();
                // 需要将 id 更新为真实 id
                update.setId(md5);
                update.setName(fileFromUrl.getName());
                update.setExtName(extName);
                update.setModifyTimeMillis(System.currentTimeMillis());
                update.setPath(path);
                update.setStatus(1);
                update.setSize(FileUtil.size(fileStorageFile));
                Entity updateEntity = this.dataBeanToEntity(update);
                Entity id = Entity.create().set("id", uuid);
                this.update(updateEntity, id);
            } catch (Exception e) {
                log.error("下载文件失败", e);
                this.updateError(uuid, e.getMessage());
            }
        });
    }

    private StreamProgress createStreamProgress(String uuid) {
        int logReduceProgressRatio = buildExtConfig.getLogReduceProgressRatio();
        Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / logReduceProgressRatio));
        long bytes = DataSize.ofMegabytes(1).toBytes();
        return new StreamProgress() {
            @Override
            public void start() {

            }

            @Override
            public void progress(long total, long progressSize) {
                if (total > 0) {
                    double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                    String percent = NumberUtil.formatPercent((float) progressSize / total, 0);
                    int progressRange = (int) Math.floor(progressPercentage / logReduceProgressRatio);
                    // 存在文件总大小
                    if (progressRangeList.add(progressRange)) {
                        //  total, progressSize
                        updateProgress(uuid, percent, total, progressSize);
                    }
                } else {
                    // 不存在文件总大小
                    if (progressSize % bytes == 0) {
                        updateProgress(uuid, null, total, progressSize);
                    }
                }
            }

            @Override
            public void finish() {

            }
        };
    }

    private void updateProgress(String id, String desc, long total, long progressSize) {
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(id);
        String fileSize = FileUtil.readableFileSize(progressSize);
        desc = (desc == null || desc.isEmpty() ? fileSize : desc);
        fileStorageModel.setName("文件下载中：" + desc);
        fileStorageModel.setStatus(0);
        fileStorageModel.setSize(progressSize);

        fileStorageModel.setProgressDesc(String.format("当前进度：{}, 文件总大小：{}，已经下载：{}", desc, FileUtil.readableFileSize(total), fileSize));
        this.updateById(fileStorageModel);
    }

    /**
     * 更新进度
     *
     * @param id    数据id
     * @param error 错误信息
     */
    private void updateError(String id, String error) {
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(id);
        fileStorageModel.setName("文件下载失败：" + (error == null ? null : (error.length() <= 200 ? error : error.substring(0, 200))));
        fileStorageModel.setStatus(2);
        fileStorageModel.setProgressDesc(error);
        this.updateById(fileStorageModel);
    }

    /**
     * 添加文件
     *
     * @param source      文件来源
     * @param file        要文件的文件
     * @param description 描述
     * @param workspaceId 工作空间id
     * @param aliasCode   别名码
     * @return 返回成功的文件id
     */
    public String addFile(File file, int source, String workspaceId, String description, String aliasCode) {
        return addFile(file, source, workspaceId, description, aliasCode, null);
    }

    /**
     * 添加文件
     *
     * @param source      文件来源
     * @param file        要文件的文件
     * @param description 描述
     * @param workspaceId 工作空间id
     * @param aliasCode   别名码
     * @return 返回成功的文件id
     */
    public String addFile(File file, int source, String workspaceId, String description, String aliasCode, Integer keepDay) {
        String md5 = DigestUtil.md5(file);
        File storageSavePath = serverConfig.fileStorageSavePath();
        String extName = FileUtil.extName(file);
        String path = String.format("/%s/%s.%s", DateTime.now().toString("yyyyMMdd"), md5, extName);
        FileStorageModel storageModel = this.getByKey(md5);
        if (storageModel != null) {
            return null;
        }
        // 保存
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(md5);
        fileStorageModel.setAliasCode(aliasCode);
        fileStorageModel.setName(file.getName());
        fileStorageModel.setDescription(description);
        fileStorageModel.setExtName(extName);
        fileStorageModel.setPath(path);
        fileStorageModel.setSize(FileUtil.size(file));
        fileStorageModel.setSource(source);
        fileStorageModel.setWorkspaceId(workspaceId);
        fileStorageModel.validUntil(keepDay, null);
        this.insert(fileStorageModel);
        //
        File fileStorageFile = FileUtil.file(storageSavePath, path);
        FileUtil.mkParentDirs(fileStorageFile);
        FileUtil.copyFile(file, fileStorageFile, StandardCopyOption.REPLACE_EXISTING);
        return md5;
    }

    @Override
    public void executeTask() {
        // 定时删除文件
        Entity entity = Entity.create();
        entity.set("validUntil", " < " + System.currentTimeMillis());
        List<FileStorageModel> storageModels = this.listByEntity(entity);
        if ((storageModels == null || storageModels.isEmpty())) {
            return;
        }
        File storageSavePath = serverConfig.fileStorageSavePath();
        for (FileStorageModel storageModel : storageModels) {
            log.info("开始删除 {} 文件 {}", storageModel.getName(), storageModel.getPath());
            File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
            FileUtil.del(fileStorageFile);
            this.delByKey(storageModel.getId());
        }
    }

    @Override
    public int statusRecover() {
        FileStorageModel update = new FileStorageModel();
        update.setName("系统重启取消下载任务");
        update.setModifyTimeMillis(System.currentTimeMillis());
        update.setStatus(2);
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create()
            .set("source", 2)
            .set("status", 0);
        return this.update(updateEntity, where);
    }
}
