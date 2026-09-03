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

import io.voyager1.core.jpa.WorkspaceContext;
import io.voyager1.util.NumberUtil;
import io.voyager1.util.StrUtil;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Tuple;

import io.voyager1.util.CollectorUtil;
import io.voyager1.util.ThreadUtil;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.Tuple;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.JschUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.ServerConst;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.core.entity.FileReleaseTaskLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.FileReleaseTaskLogRepository;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.files.model.FileReleaseTaskLogModel;
import io.voyager1.func.files.model.FileStorageModel;
import io.voyager1.func.files.model.IFileStorage;
import io.voyager1.func.files.model.StaticFileStorageModel;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.plugins.JschUtils;
import io.voyager1.service.IStatusRecover;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.system.ServerConfig;
import io.voyager1.transport.*;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.MySftp;
import io.voyager1.util.StrictSyncFinisher;
import io.voyager1.util.SyncFinisherUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * @since 2023/3/18
 */
@Service
@Slf4j
public class FileReleaseTaskService extends JpaWorkspaceService<FileReleaseTaskLogModel, FileReleaseTaskLogEntity> implements IStatusRecover {

    private final SshService sshService;
    private final Voyager1Application voyager1Application;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final NodeService nodeService;
    private final BuildExtConfig buildExtConfig;
    private final ServerConfig serverConfig;
    private final FileStorageService fileStorageService;
    private final StaticFileStorageService staticFileStorageService;
    private final ScriptServer scriptServer;
    private final FileReleaseTaskTemplateService fileReleaseTaskTemplateService;
    private final FileReleaseTaskLogRepository fileReleaseTaskLogRepository;

    private final Map<String, String> cancelTag = new java.util.concurrent.ConcurrentHashMap<>();

    public FileReleaseTaskService(SshService sshService,
                                  Voyager1Application voyager1Application,
                                  WorkspaceEnvVarService workspaceEnvVarService,
                                  NodeService nodeService,
                                  BuildExtConfig buildExtConfig,
                                  ServerConfig serverConfig,
                                  FileStorageService fileStorageService,
                                  StaticFileStorageService staticFileStorageService,
                                  ScriptServer scriptServer,
                                  FileReleaseTaskTemplateService fileReleaseTaskTemplateService,
                                  FileReleaseTaskLogRepository fileReleaseTaskLogRepository) {
        this.sshService = sshService;
        this.voyager1Application = voyager1Application;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.nodeService = nodeService;
        this.buildExtConfig = buildExtConfig;
        this.serverConfig = serverConfig;
        this.fileStorageService = fileStorageService;
        this.staticFileStorageService = staticFileStorageService;
        this.scriptServer = scriptServer;
        this.fileReleaseTaskTemplateService = fileReleaseTaskTemplateService;
        this.fileReleaseTaskLogRepository = fileReleaseTaskLogRepository;
    }

    @Override
    protected JpaRepository<FileReleaseTaskLogEntity, String> repository() {
        return fileReleaseTaskLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<FileReleaseTaskLogEntity> specExecutor() {
        return fileReleaseTaskLogRepository;
    }

    @Override
    protected Class<FileReleaseTaskLogEntity> entityClass() {
        return FileReleaseTaskLogEntity.class;
    }

    @Override
    protected Class<FileReleaseTaskLogModel> modelClass() {
        return FileReleaseTaskLogModel.class;
    }

    /**
     * 获取任务记录（只查看主任务）
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<FileReleaseTaskLogModel> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        paramMap.put("workspaceId", workspaceId);
        paramMap.put("taskId", FileReleaseTaskLogModel.TASK_ROOT_ID);
        return super.listPage(paramMap);
    }

    /**
     * 获取文件中心的文件
     *
     * @param fileId  文件id
     * @param request 请求
     * @return tuple 0 文件 1 文件信息
     */
    private Tuple getFileStorage(String fileId, HttpServletRequest request) {
        FileStorageModel storageModel = fileStorageService.getByKey(fileId, request);
        Assert.notNull(storageModel, "不存在对应的文件");
        File storageSavePath = serverConfig.fileStorageSavePath();
        File file = FileUtil.file(storageSavePath, storageModel.getPath());
        Assert.state(FileUtil.isFile(file), "当前文件丢失不能执行发布任务");

        return new Tuple(file, storageModel);
    }


    /**
     * 获取静态文件中心的文件
     *
     * @param fileId  文件id
     * @param request 请求
     * @return tuple 0 文件 1 文件信息
     */
    private Tuple getStaticFileStorage(String fileId, HttpServletRequest request) {
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(fileId);
        String workspaceId = WorkspaceContext.getWorkspaceId(request);
        staticFileStorageService.checkStaticDir(storageModel, workspaceId);
        File file = FileUtil.file(storageModel.getAbsolutePath());
        Assert.state(FileUtil.isFile(file), "当前文件丢失不能执行发布任务");
        return new Tuple(file, storageModel);
    }

    /**
     * 创建任务
     *
     * @param fileId       文件id
     * @param name         名称
     * @param taskType     任务类型
     * @param taskDataIds  任务关联的数据id
     * @param releasePath  发布目录
     * @param beforeScript 发布前脚本
     * @param afterScript  发布后的脚本
     * @param request      请求
     * @return fileStorage
     */
    public IFileStorage addTask(String fileId,
                                Integer fileType,
                                String name,
                                int taskType,
                                String taskDataIds,
                                String releasePath,
                                String beforeScript,
                                String afterScript,
                                Map<String, String> env,
                                HttpServletRequest request) {
        Tuple tuple;
        switch (fileType) {
            case 1:
                tuple = this.getFileStorage(fileId, request);
                break;
            case 2:
                tuple = this.getStaticFileStorage(fileId, request);
                break;
            default:
                throw new IllegalArgumentException("不支持的类型：" + fileType);
        }
        File file = tuple.get(0);
        IFileStorage storageModel = tuple.get(1);
        //
        List<String> list;
        if (taskType == 0) {
            list = io.voyager1.util.ConvertUtil.splitTrim(taskDataIds, ",");
            list = list.stream().filter(s -> sshService.exists(new SshModel(s))).collect(Collectors.toList());
            Assert.notEmpty(list, "请选择正确的ssh");
        } else if (taskType == 1) {
            list = io.voyager1.util.ConvertUtil.splitTrim(taskDataIds, ",");
            list = list.stream().filter(s -> nodeService.exists(new NodeModel(s))).collect(Collectors.toList());
            Assert.notEmpty(list, "请选择正确的节点");
        } else {
            throw new IllegalArgumentException("不支持的方式");
        }
        // 生成任务id
        FileReleaseTaskLogModel taskRoot = new FileReleaseTaskLogModel();
        taskRoot.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        taskRoot.setTaskId(FileReleaseTaskLogModel.TASK_ROOT_ID);
        taskRoot.setTaskDataId(FileReleaseTaskLogModel.TASK_ROOT_ID);
        taskRoot.setName(name);
        taskRoot.setFileId(fileId);
        taskRoot.setFileType(fileType);
        taskRoot.setStatus(0);
        taskRoot.setTaskType(taskType);
        taskRoot.setReleasePath(releasePath);
        taskRoot.setAfterScript(afterScript);
        taskRoot.setBeforeScript(beforeScript);
        this.insert(taskRoot);
        // 子任务列表
        for (String dataId : list) {
            FileReleaseTaskLogModel releaseTaskLogModel = new FileReleaseTaskLogModel();
            releaseTaskLogModel.setTaskId(taskRoot.getId());
            releaseTaskLogModel.setTaskDataId(dataId);
            releaseTaskLogModel.setName(name);
            releaseTaskLogModel.setFileId(fileId);
            releaseTaskLogModel.setFileType(fileType);
            releaseTaskLogModel.setStatus(0);
            releaseTaskLogModel.setTaskType(taskType);
            releaseTaskLogModel.setReleasePath(taskRoot.getReleasePath());
            this.insert(releaseTaskLogModel);
        }
        this.startTask(taskRoot.getId(), file, env, storageModel);
        return storageModel;
    }

    /**
     * 开始任务d
     *
     * @param taskId          任务id
     * @param storageSaveFile 文件
     */
    private void startTask(String taskId, File storageSaveFile, Map<String, String> env, IFileStorage storageModel) {
        FileReleaseTaskLogModel taskRoot = this.getByKey(taskId);
        Assert.notNull(taskRoot, "没有找到父级任务");
        //
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskId);
        List<FileReleaseTaskLogModel> logModels = this.listByBean(fileReleaseTaskLogModel);
        Assert.notEmpty(logModels, "没有对应的任务");
        //
        EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(taskRoot.getWorkspaceId());
        Optional.ofNullable(env).ifPresent(environmentMapBuilder::putStr);
        environmentMapBuilder.put("TASK_ID", taskRoot.getTaskId());
        environmentMapBuilder.put("FILE_ID", taskRoot.getFileId());
        environmentMapBuilder.put("FILE_NAME", storageModel.getName());
        environmentMapBuilder.put("FILE_EXT_NAME", storageModel.getExtName());
        environmentMapBuilder.put("RELEASE_PATH", taskRoot.getReleasePath());
        //
        String syncFinisherId = "file-release:" + taskId;
        StrictSyncFinisher strictSyncFinisher = SyncFinisherUtil.create(syncFinisherId, logModels.size());
        Integer taskType = taskRoot.getTaskType();
        if (taskType == 0) {
            crateTaskSshWork(logModels, strictSyncFinisher, taskRoot, environmentMapBuilder, storageSaveFile);
        } else if (taskType == 1) {
            // 节点
            crateTaskNodeWork(logModels, strictSyncFinisher, taskRoot, environmentMapBuilder, storageSaveFile, storageModel);
        } else {
            throw new IllegalArgumentException("不支持的方式");
        }
        I18nThreadUtil.execute(() -> {
            try {
                strictSyncFinisher.start();
                if (cancelTag.containsKey(taskId)) {
                    // 任务来源被取消
                    this.cancelTaskUpdate(taskId);
                } else {
                    this.updateRootStatus(taskId, 2, "正常结束");
                }
            } catch (Exception e) {
                log.error("执行发布任务失败", e);
                updateRootStatus(taskId, 3, e.getMessage());
            } finally {
                SyncFinisherUtil.close(syncFinisherId);
                cancelTag.remove(taskId);
            }
        });
    }

    /**
     * 取消任务
     *
     * @param taskId 任务id
     */
    public void cancelTask(String taskId) {
        String syncFinisherId = "file-release:" + taskId;
        SyncFinisherUtil.cancel(syncFinisherId);
        // 异步线程无法标记 ,同步监听线程去操作
        cancelTag.put(taskId, taskId);
    }

    private void cancelTaskUpdate(String taskId) {
        // 将未完成的任务标记为取消
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setStatus(4);
        update.setStatusMsg("手动取消任务");
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create().set("taskId", taskId).set("status", new java.util.ArrayList<>(java.util.Arrays.asList(0, 1)));
        this.update(updateEntity, where);
        this.updateRootStatus(taskId, 4, "手动取消任务");
    }

    /**
     * 创建 节点 发布任务
     *
     * @param values                需要发布的任务列表
     * @param strictSyncFinisher    线程同步器
     * @param taskRoot              任务
     * @param environmentMapBuilder 环境变量
     * @param storageSaveFile       文件
     */
    private void crateTaskNodeWork(Collection<FileReleaseTaskLogModel> values,
                                   StrictSyncFinisher strictSyncFinisher,
                                   FileReleaseTaskLogModel taskRoot,
                                   EnvironmentMapBuilder environmentMapBuilder,
                                   File storageSaveFile,
                                   IFileStorage storageModel) {
        String taskId = taskRoot.getId();
        for (FileReleaseTaskLogModel model : values) {
            model.setAfterScript(taskRoot.getAfterScript());
            model.setBeforeScript(taskRoot.getBeforeScript());
            strictSyncFinisher.addWorker(() -> {
                String modelId = model.getId();
                LogRecorder logRecorder = null;
                try {
                    this.updateStatus(taskId, modelId, 1, "开始发布文件");
                    File logFile = logFile(model);
                    logRecorder = LogRecorder.builder().file(logFile).charset(StandardCharsets.UTF_8).build();
                    NodeModel item = nodeService.getByKey(model.getTaskDataId());
                    if (item == null) {
                        logRecorder.systemError("没有找到对应的节点项：{}", model.getTaskDataId());
                        this.updateStatus(taskId, modelId, 3, String.format("没有找到对应的节点项：%s", model.getTaskDataId()));
                        return;
                    }

                    String releasePath = model.getReleasePath();
                    String beforeScript = model.getBeforeScript();
                    if ((beforeScript != null && !beforeScript.isEmpty())) {
                        logRecorder.system("开始执行上传前命令");
                        if ((beforeScript != null && beforeScript.startsWith(ServerConst.REF_SCRIPT))) {
                            String scriptId = (beforeScript != null && beforeScript.startsWith(ServerConst.REF_SCRIPT) ? beforeScript.substring(ServerConst.REF_SCRIPT.length()) : beforeScript);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, "请选择正确的脚本");
                            beforeScript = keyAndGlobal.getContext();
                            logRecorder.system("引入脚本内容：{}[{}]", keyAndGlobal.getName(), scriptId);
                        }
                        this.runNodeScript(beforeScript, item, logRecorder, modelId, environmentMapBuilder, releasePath);
                    }
                    logRecorder.system("{} start file upload", item.getName());
                    // 上传文件
                    JSONObject data = new JSONObject();
                    data.put("path", releasePath);
                    Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
                    String name = storageModel.getName();
                    name = StrUtil.wrapIfMissing(name, "", "." + storageModel.getExtName());
                    LogRecorder finalLogRecorder = logRecorder;
                    ApiResult<String> jsonMessage = NodeForward.requestSharding(item, NodeUrl.Manage_File_Upload_Sharding2, data, storageSaveFile, name,
                        sliceData -> {
                            sliceData.putAll(data);
                            return NodeForward.request(item, NodeUrl.Manage_File_Sharding_Merge2, sliceData);
                        },
                        (total, progressSize) -> {

                            double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                            int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                            if (progressRangeList.add(progressRange)) {
                                String info = "上传文件进度:{}/{} {}";
                                finalLogRecorder.system(info,
                                    FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                                    NumberUtil.formatPercent(((float) progressSize / total), 0));
                            }
                        });
                    if (!jsonMessage.success()) {
                        throw new IllegalStateException("上传文件失败：" + jsonMessage);
                    }
                    logRecorder.system("{} file upload done", item.getName());

                    String afterScript = model.getAfterScript();
                    if ((afterScript != null && !afterScript.isEmpty())) {
                        logRecorder.system("开始执行上传后命令");
                        if ((afterScript != null && afterScript.startsWith(ServerConst.REF_SCRIPT))) {
                            String scriptId = (afterScript != null && afterScript.startsWith(ServerConst.REF_SCRIPT) ? afterScript.substring(ServerConst.REF_SCRIPT.length()) : afterScript);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, "请选择正确的脚本");
                            afterScript = keyAndGlobal.getContext();
                            logRecorder.system("引入脚本内容：{}[{}]", keyAndGlobal.getName(), scriptId);
                        }
                        this.runNodeScript(afterScript, item, logRecorder, modelId, environmentMapBuilder, releasePath);
                    }
                    this.updateStatus(taskId, modelId, 2, "发布成功");
                } catch (Exception e) {
                    log.error("执行发布任务异常", e);
                    updateStatus(taskId, modelId, 3, e.getMessage());
                } finally {
                    IoUtil.close(logRecorder);
                }
            });
        }
    }

    /**
     * 执行节点脚本
     *
     * @param content               脚本内容
     * @param model                 节点
     * @param logRecorder           日志记录器
     * @param id                    任务id
     * @param environmentMapBuilder 环境变量
     * @param path                  执行路径
     * @throws IOException io
     */
    private void runNodeScript(String content, NodeModel model, LogRecorder logRecorder, String id, EnvironmentMapBuilder environmentMapBuilder, String path) throws IOException {
        INodeInfo nodeInfo = NodeForward.parseNodeInfo(model);
        IUrlItem urlItem = NodeForward.parseUrlItem(nodeInfo, model.getWorkspaceId(), NodeUrl.FreeScriptRun, DataContentType.FORM_URLENCODED);
        try (IProxyWebSocket proxySession = TransportServerFactory.get().websocket(nodeInfo, urlItem)) {
            proxySession.onMessage(s -> {
                if (java.util.Objects.equals(s, "VOYAGER1_SYSTEM_TAG:" + id)) {
                    try {
                        proxySession.close();
                    } catch (IOException e) {
                        log.error("关闭会话异常", e);
                        logRecorder.systemError("关闭会话异常：{}", e.getMessage());
                    }
                    return;
                }
                logRecorder.info(s);
            });
            // 等待链接
            proxySession.connectBlocking();
            // 发送操作消息
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tag", id);
            jsonObject.put("path", path);
            jsonObject.put("environment", environmentMapBuilder.toDataJson());
            jsonObject.put("content", content);
            proxySession.send(jsonObject.toString());
            // 阻塞
            while (proxySession.isConnected()) {
                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(500));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 创建 ssh 发布任务
     *
     * @param values                需要发布的任务列表
     * @param strictSyncFinisher    线程同步器
     * @param taskRoot              任务
     * @param environmentMapBuilder 环境变量
     * @param storageSaveFile       文件
     */
    private void crateTaskSshWork(Collection<FileReleaseTaskLogModel> values,
                                  StrictSyncFinisher strictSyncFinisher,
                                  FileReleaseTaskLogModel taskRoot,
                                  EnvironmentMapBuilder environmentMapBuilder,
                                  File storageSaveFile) {
        String taskId = taskRoot.getId();
        for (FileReleaseTaskLogModel model : values) {
            model.setAfterScript(taskRoot.getAfterScript());
            model.setBeforeScript(taskRoot.getBeforeScript());
            strictSyncFinisher.addWorker(() -> {
                String modelId = model.getId();
                Session session = null;
                ChannelSftp channelSftp = null;
                LogRecorder logRecorder = null;
                try {
                    this.updateStatus(taskId, modelId, 1, "开始发布文件");
                    File logFile = logFile(model);
                    logRecorder = LogRecorder.builder().file(logFile).charset(StandardCharsets.UTF_8).build();
                    SshModel item = sshService.getByKey(model.getTaskDataId());
                    if (item == null) {
                        logRecorder.systemError("没有找到对应的ssh项：{}", model.getTaskDataId());
                        this.updateStatus(taskId, modelId, 3, String.format("没有找到对应的ssh项：%s", model.getTaskDataId()));
                        return;
                    }
                    MachineSshModel machineSshModel = sshService.getMachineSshModel(item);
                    Charset charset = machineSshModel.charset();
                    int timeout = machineSshModel.timeout();
                    session = sshService.getSessionByModel(machineSshModel);
                    Map<String, String> environment = environmentMapBuilder.environment();
                    environmentMapBuilder.eachStr(logRecorder::system);
                    String beforeScript = model.getBeforeScript();
                    if ((beforeScript != null && !beforeScript.isEmpty())) {
                        logRecorder.system("开始执行上传前命令");
                        if ((beforeScript != null && beforeScript.startsWith(ServerConst.REF_SCRIPT))) {
                            String scriptId = (beforeScript != null && beforeScript.startsWith(ServerConst.REF_SCRIPT) ? beforeScript.substring(ServerConst.REF_SCRIPT.length()) : beforeScript);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, "请选择正确的脚本");
                            beforeScript = keyAndGlobal.getContext();
                            logRecorder.system("引入脚本内容：{}[{}]", keyAndGlobal.getName(), scriptId);
                        }
                        JschUtils.execCallbackLine(session, charset, timeout, beforeScript, "", environment, logRecorder::info);
                    }
                    logRecorder.system("{} start ftp upload", item.getName());

                    MySftp.ProgressMonitor sftpProgressMonitor = sshService.createProgressMonitor(logRecorder);
                    // 不需要关闭资源，因为共用会话
                    MySftp sftp = new MySftp(session, charset, timeout, sftpProgressMonitor);
                    channelSftp = sftp.getClient();
                    String releasePath = model.getReleasePath();
                    sftp.syncUpload(storageSaveFile, releasePath);
                    logRecorder.system("{} ftp upload done", item.getName());

                    String afterScript = model.getAfterScript();
                    if ((afterScript != null && !afterScript.isEmpty())) {
                        logRecorder.system("开始执行上传后命令");
                        if ((afterScript != null && afterScript.startsWith(ServerConst.REF_SCRIPT))) {
                            String scriptId = (afterScript != null && afterScript.startsWith(ServerConst.REF_SCRIPT) ? afterScript.substring(ServerConst.REF_SCRIPT.length()) : afterScript);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, "请选择正确的脚本");
                            afterScript = keyAndGlobal.getContext();
                            logRecorder.system("引入脚本内容：{}[{}]", keyAndGlobal.getName(), scriptId);
                        }
                        JschUtils.execCallbackLine(session, charset, timeout, afterScript, "", environment, logRecorder::info);
                    }
                    this.updateStatus(taskId, modelId, 2, "发布成功");
                } catch (Exception e) {
                    log.error("执行发布任务异常", e);
                    updateStatus(taskId, modelId, 3, e.getMessage());
                } finally {
                    IoUtil.close(logRecorder);
                    JschUtil.close(channelSftp);
                    JschUtil.close(session);
                }
            });
        }
    }

    /**
     * 更新总任务信息（忽略多线程并发问题，因为最终的更新是单线程）
     *
     * @param taskId    任务ID
     * @param status    状态
     * @param statusMsg 状态描述
     */
    private void updateRootStatus(String taskId, int status, String statusMsg) {
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskId);
        List<FileReleaseTaskLogModel> logModels = this.listByBean(fileReleaseTaskLogModel);
        Map<Integer, List<FileReleaseTaskLogModel>> map = logModels.stream()
            .collect(CollectorUtil.groupingBy(logModel -> (logModel.getStatus() != null ? logModel.getStatus() : 0), Collectors.toList()));
        StringBuilder stringBuilder = new StringBuilder();
        //
        Opt.ofBlankAble(statusMsg).ifPresent(s -> stringBuilder.append(s).append(" "));
        Set<Map.Entry<Integer, List<FileReleaseTaskLogModel>>> entries = map.entrySet();
        for (Map.Entry<Integer, List<FileReleaseTaskLogModel>> entry : entries) {
            Integer key = entry.getKey();
            // 0 等待开始 1 进行中 2 任务结束 3 失败
            switch (key) {
                case 0:
                    stringBuilder.append("等待开始:");
                    break;
                case 1:
                    stringBuilder.append("进行中:");
                    break;
                case 2:
                    stringBuilder.append("任务结束:");
                    break;
                case 3:
                    stringBuilder.append("失败:");
                    break;
                default:
                    stringBuilder.append("未知：");
                    break;
            }
            stringBuilder.append(CollUtil.size(entry.getValue()));
        }
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setStatus(status);
        update.setId(taskId);
        update.setStatusMsg(stringBuilder.toString());
        this.updateById(update);
    }

    /**
     * 更新单给任务状态
     *
     * @param taskId    总任务
     * @param id        子任务id
     * @param status    状态
     * @param statusMsg 状态描述
     */
    private void updateStatus(String taskId, String id, int status, String statusMsg) {
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setId(id);
        fileReleaseTaskLogModel.setStatus(status);
        fileReleaseTaskLogModel.setStatusMsg(statusMsg);
        this.updateById(fileReleaseTaskLogModel);
        // 更新总任务
        updateRootStatus(taskId, 1, "");
    }

    public File logFile(FileReleaseTaskLogModel model) {
        return FileUtil.file(voyager1Application.getDataPath(), "file-release-log",
            model.getTaskId(),
            model.getId() + ".log"
        );
    }

    public File logTaskDir(FileReleaseTaskLogModel model) {
        return FileUtil.file(voyager1Application.getDataPath(), "file-release-log", model.getId());
    }

    @Override
    public int statusRecover() {
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setModifyTimeMillis(System.currentTimeMillis());
        update.setStatus(4);
        update.setStatusMsg("系统取消");
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create()
            .set("status", new java.util.ArrayList<>(java.util.Arrays.asList(0, 1)));
        return this.update(updateEntity, where);
    }
}
