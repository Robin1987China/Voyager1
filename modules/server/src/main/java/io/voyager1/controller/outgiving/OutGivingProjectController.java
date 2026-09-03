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

package io.voyager1.controller.outgiving;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.BooleanUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.HttpUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.BaseIdModel;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.build.BuildExtraModule;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.func.files.model.FileStorageModel;
import io.voyager1.func.files.model.StaticFileStorageModel;
import io.voyager1.func.files.service.FileStorageService;
import io.voyager1.func.files.service.StaticFileStorageService;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.BaseNodeModel;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.log.OutGivingLog;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.model.outgiving.BaseNodeProject;
import io.voyager1.model.outgiving.OutGivingModel;
import io.voyager1.model.outgiving.OutGivingNodeProject;
import io.voyager1.outgiving.OutGivingRun;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.outgiving.DbOutGivingLogService;
import io.voyager1.service.outgiving.OutGivingServer;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.FileUtils;
import io.voyager1.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 分发文件管理
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
@Slf4j
public class OutGivingProjectController extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final ServerConfig serverConfig;
    private final DbOutGivingLogService dbOutGivingLogService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final FileStorageService fileStorageService;
    private final StaticFileStorageService staticFileStorageService;

    public OutGivingProjectController(OutGivingServer outGivingServer,
                                      OutGivingWhitelistService outGivingWhitelistService,
                                      ServerConfig serverConfig,
                                      DbOutGivingLogService dbOutGivingLogService,
                                      ProjectInfoCacheService projectInfoCacheService,
                                      BuildInfoService buildInfoService,
                                      DbBuildHistoryLogService dbBuildHistoryLogService,
                                      FileStorageService fileStorageService,
                                      StaticFileStorageService staticFileStorageService) {
        this.outGivingServer = outGivingServer;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.serverConfig = serverConfig;
        this.dbOutGivingLogService = dbOutGivingLogService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.fileStorageService = fileStorageService;
        this.staticFileStorageService = staticFileStorageService;
    }

    @RequestMapping(value = "getItemData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误id error") String id,
                                                HttpServletRequest request) {
        String workspaceId = outGivingServer.getCheckUserWorkspace(request);
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Objects.requireNonNull(outGivingServerItem, "没有数据");
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.outGivingNodeProjectList();
        //
        Set<String> nodeIds = outGivingNodeProjectList.stream().map(BaseNodeProject::getNodeId).collect(Collectors.toSet());
        List<NodeModel> nodeModels = nodeService.getByKey(nodeIds);
        Map<String, NodeModel> nodeMap = CollStreamUtil.toMap(nodeModels, BaseIdModel::getId, nodeModel -> nodeModel);
        //
        Set<String> projectIds = outGivingNodeProjectList.stream().map(nodeProject -> BaseNodeModel.fullId(workspaceId, nodeProject.getNodeId(), nodeProject.getProjectId())).collect(Collectors.toSet());
        List<ProjectInfoCacheModel> projectInfoCacheModels = projectInfoCacheService.getByKey(projectIds);
        Map<String, ProjectInfoCacheModel> projectMap = CollStreamUtil.toMap(projectInfoCacheModels, BaseIdModel::getId, data -> data);


        List<JSONObject> collect = outGivingNodeProjectList
            .stream()
            .map(outGivingNodeProject -> {
                NodeModel nodeModel = nodeMap.get(outGivingNodeProject.getNodeId());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sortValue", outGivingNodeProject.getSortValue());
                jsonObject.put("disabled", outGivingNodeProject.getDisabled());
                jsonObject.put("nodeId", outGivingNodeProject.getNodeId());
                jsonObject.put("projectId", outGivingNodeProject.getProjectId());
                jsonObject.put("nodeName", nodeModel.getName());
                String fullId = BaseNodeModel.fullId(workspaceId, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
                jsonObject.put("id", fullId);
                ProjectInfoCacheModel projectInfoCacheModel = projectMap.get(fullId);
                if (projectInfoCacheModel != null) {
                    jsonObject.put("cacheProjectName", projectInfoCacheModel.getName());
                }

                OutGivingLog outGivingLog = dbOutGivingLogService.getByProject(id, outGivingNodeProject);
                if (outGivingLog != null) {
                    jsonObject.put("outGivingStatus", outGivingLog.getStatus());
                    jsonObject.put("outGivingResult", outGivingLog.getResult());
                    jsonObject.put("lastTime", outGivingLog.getCreateTimeMillis());
                    jsonObject.put("fileSize", outGivingLog.getFileSize());
                    jsonObject.put("progressSize", outGivingLog.getProgressSize());
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
        JSONObject data = new JSONObject();
        data.put("data", outGivingServerItem);
        data.put("projectList", collect);
        return ApiResult.success("", data);
    }

    private File checkZip(File path, boolean unzip) {
        if (unzip) {
            boolean zip = false;
            for (String i : StringUtil.PACKAGE_EXT) {
                if (FileUtil.pathEndsWith(path, i)) {
                    zip = true;
                    break;
                }
            }
            Assert.state(zip, "不支持的文件类型:" + path.getName());
        }
        return path;
    }

    /**
     * 节点分发文件
     *
     * @param id 分发id
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public ApiResult<Object> uploadSharding(String id,
                                               MultipartFile file,
                                               String sliceId,
                                               Integer totalSlice,
                                               Integer nowSlice,
                                               String fileSumMd5,
                                               HttpServletRequest request) throws IOException {
        // 状态判断
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        File userTempPath = serverConfig.getUserTempPath();
        // 保存文件
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5);
        return ApiResult.success("上传成功");
    }

    /**
     * 节点分发文件
     *
     * @param id        分发id
     * @param afterOpt  之后的操作
     * @param autoUnzip 是否自动解压
     * @param clearOld  清空发布
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "upload-sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<Object> upload(String id, String afterOpt, String clearOld, String autoUnzip,
                                       String secondaryDirectory, String stripComponents,
                                       String selectProject,
                                       String sliceId,
                                       Integer totalSlice,
                                       String fileSumMd5, HttpServletRequest request) throws IOException {
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        //
        boolean unzip = ConvertUtil.toBool(autoUnzip, false);
        File file = FileUtil.file(Voyager1Application.getInstance().getDataPath(), ServerConst.OUTGIVING_FILE, id);
        FileUtil.mkdir(file);
        //
        File userTempPath = serverConfig.getUserTempPath();
        File successFile = this.shardingTryMerge(userTempPath.getAbsolutePath(), sliceId, totalSlice, fileSumMd5);
        FileUtil.move(successFile, file, true);
        //
        File dest = FileUtil.file(file, successFile.getName());
        dest = this.checkZip(dest, unzip);
        //
        OutGivingModel outGivingModel = new OutGivingModel();
        outGivingModel.setId(id);
        outGivingModel.setClearOld(ConvertUtil.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("upload");
        outGivingModel.setModeData(successFile.getName());
        outGivingServer.updateById(outGivingModel);
        int stripComponentsValue = ConvertUtil.toInt(stripComponents, 0);
        // 开启
        OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
            .id(outGivingModel.getId())
            .file(dest)
            .userModel(getUser())
            .unzip(unzip)
            .mode(outGivingModel.getMode())
            .modeData(outGivingModel.getModeData())
            .stripComponents(stripComponentsValue);
        outGivingRunBuilder.build().startRun(selectProject);
        return ApiResult.success("上传成功,开始分发!");
    }

    private OutGivingModel check(String id, BiConsumer<OutGivingModel.Status, OutGivingModel> consumer, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "上传失败,没有找到对应的分发项目");
        // 检查状态
        Integer statusCode = outGivingModel.getStatus();
        OutGivingModel.Status status = BaseEnum.getEnum(OutGivingModel.Status.class, statusCode, OutGivingModel.Status.NO);
        consumer.accept(status, outGivingModel);
        return outGivingModel;
    }


    /**
     * 远程下载节点分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public ApiResult<String> remoteDownload(String id, String afterOpt, String clearOld, String url, String autoUnzip,
                                               String secondaryDirectory,
                                               String stripComponents,
                                               String selectProject,
                                               HttpServletRequest request) {
        Assert.hasText(url, "填写下载地址");
        Assert.state(StrUtil.length(url) <= 200, "url 长度不能超过 200");
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);

        //outGivingModel = outGivingServer.getItem(id);
        outGivingModel.setClearOld(ConvertUtil.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("download");
        outGivingModel.setModeData(url);
        outGivingServer.updateById(outGivingModel);
        //下载
        File file = FileUtil.file(serverConfig.getUserTempPath(), ServerConst.OUTGIVING_FILE, id);
        FileUtil.mkdir(file);
        File downloadFile = HttpUtil.downloadFileFromUrl(url, file);
        this.startTask(outGivingModel, downloadFile, null, autoUnzip, stripComponents, selectProject, true);
        return ApiResult.success("下载成功,开始分发!");
    }

    /**
     * 通过构建历史分发
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "use-build", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> useBuild(String id, String afterOpt, String clearOld, String buildId, String buildNumberId,
                                         String secondaryDirectory,
                                         String stripComponents,
                                         String selectProject,
                                         HttpServletRequest request) {

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");

        BuildInfoModel infoModel = buildInfoService.getByKey(buildId, request);
        Assert.notNull(infoModel, "没有对应的构建");
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setBuildDataId(infoModel.getId());
        Integer numberId = ConvertUtil.toInt(buildNumberId, 0);
        buildHistoryLog.setBuildNumberId(numberId);
        BuildHistoryLog historyLog = dbBuildHistoryLogService.queryByBean(buildHistoryLog);
        Assert.notNull(historyLog, "没有对应的构建记录");
        BuildExtraModule buildExtraModule = BuildExtraModule.build(historyLog);
        //String resultDirFileStr = buildExtraModule.getResultDirFile();
        EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
        boolean tarGz = environmentMapBuilder.getBool(BuildUtil.USE_TAR_GZ, false);
        int stripComponentsValue = ConvertUtil.toInt(stripComponents, 0);
        //
        outGivingModel.setClearOld(ConvertUtil.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("use-build");
        outGivingModel.setModeData(buildId + ":" + buildNumberId);
        File resultDirFile = buildExtraModule.resultDirFile(numberId);
        outGivingServer.updateById(outGivingModel);
        //
        BuildUtil.loadDirPackage(infoModel.getId(), numberId, resultDirFile, tarGz, (unZip, zipFile) -> {
            OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
                .id(outGivingModel.getId())
                .file(zipFile)
                .userModel(getUser())
                .unzip(unZip)
                // 由构建配置决定是否删除
                .doneDeleteFile(false)
                .mode(outGivingModel.getMode())
                .modeData(outGivingModel.getModeData())
                .stripComponents(stripComponentsValue);
            return outGivingRunBuilder.build().startRun(selectProject);
        });
        return ApiResult.success("开始分发!");
    }

    /**
     * 文件中心分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "use-file-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> useFileStorage(String id, String afterOpt, String clearOld, String fileId, String autoUnzip,
                                               String secondaryDirectory,
                                               String stripComponents,
                                               String selectProject,
                                               HttpServletRequest request) {

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        FileStorageModel storageModel = fileStorageService.getByKey(fileId, request);
        Assert.notNull(storageModel, "对应的文件不存在");
        //
        outGivingModel.setClearOld(ConvertUtil.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("file-storage");
        outGivingModel.setModeData(fileId);
        outGivingServer.updateById(outGivingModel);
        File storageSavePath = serverConfig.fileStorageSavePath();
        File file = FileUtil.file(storageSavePath, storageModel.getPath());
        String fileName = FileUtils.safeFileName(storageModel.getName(), storageModel.getExtName(), "file-storage." + storageModel.getExtName());
        this.startTask(outGivingModel, file, fileName, autoUnzip, stripComponents, selectProject, false);
        return ApiResult.success("开始分发!");
    }

    /**
     * 静态文件分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "use-static-file-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> useStaticFileStorage(String id, String afterOpt, String clearOld, String fileId, String autoUnzip,
                                                     String secondaryDirectory,
                                                     String stripComponents,
                                                     String selectProject,
                                                     HttpServletRequest request) {

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, ConvertUtil.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(fileId);
        String workspaceId = outGivingServer.getCheckUserWorkspace(request);
        staticFileStorageService.checkStaticDir(storageModel, workspaceId);
        //
        outGivingModel.setClearOld(ConvertUtil.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("static-file-storage");
        outGivingModel.setModeData(fileId);
        outGivingServer.updateById(outGivingModel);

        File file = FileUtil.file(storageModel.getAbsolutePath());
        String fileName = FileUtils.safeFileName(storageModel.getName(), storageModel.getExtName(), "file-storage." + storageModel.getExtName());
        this.startTask(outGivingModel, file, fileName, autoUnzip, stripComponents, selectProject, false);
        return ApiResult.success("开始分发!");
    }

    /**
     * 开始发布任务
     *
     * @param outGivingModel  分发对象
     * @param file            文件
     * @param autoUnzip       是否解压
     * @param stripComponents 剔除目录
     * @param selectProject   选择指定项目
     */
    private void startTask(OutGivingModel outGivingModel, File file, String fileName, String autoUnzip,
                           String stripComponents,
                           String selectProject,
                           boolean deleteFile) {
        Assert.state(FileUtil.isFile(file), "当前文件丢失不能执行发布任务");
        //
        boolean unzip = Boolean.parseBoolean(autoUnzip);
        //
        this.checkZip(file, unzip);
        int stripComponentsValue = ConvertUtil.toInt(stripComponents, 0);
        // 开启
        OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
            .id(outGivingModel.getId())
            .file(file)
            .userModel(getUser())
            .fileName(fileName)
            .unzip(unzip)
            .mode(outGivingModel.getMode())
            .modeData(outGivingModel.getModeData())
            // 是否删除
            .doneDeleteFile(deleteFile)
            // 可以不再设置-会查询最新的
            //            .projectSecondaryDirectory(secondaryDirectory)
            .stripComponents(stripComponentsValue);
        outGivingRunBuilder.build().startRun(selectProject);
    }

    @PostMapping(value = "cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> cancel(@ValidatorItem String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status == OutGivingModel.Status.ING, "当前状态不是分发中"), request);
        OutGivingRun.cancel(outGivingModel.getId(), getUser());
        //
        return ApiResult.success("取消成功");
    }

    @PostMapping(value = "config-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> configProject(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, "没有任何信息");
        String id = jsonObject.getString("id");
        List<OutGivingNodeProject> list = jsonObject.getList("data", OutGivingNodeProject.class);
        Assert.notEmpty(list, "没有配置任何项目");
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发项目");
        // 更新信息
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, "分发信息错误,没有任何项目");
        for (OutGivingNodeProject outGivingNodeProject : list) {
            OutGivingNodeProject nodeProject = OutGivingModel.getNodeProject(outGivingNodeProjects, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
            Assert.notNull(nodeProject, "没有找到对应的项目信息");
            nodeProject.setDisabled(outGivingNodeProject.getDisabled());
            nodeProject.setSortValue(outGivingNodeProject.getSortValue());
        }
        // 更新
        OutGivingModel update = new OutGivingModel();
        update.setId(outGivingModel.getId());
        update.outGivingNodeProjectList(outGivingNodeProjects);
        outGivingServer.updateById(update);
        return ApiResult.success("更新成功");
    }

    @GetMapping(value = "remove-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> removeProject(@ValidatorItem String id,
                                              @ValidatorItem String nodeId,
                                              @ValidatorItem String projectId,
                                              HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发项目");
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, "分发信息错误,没有任何项目");
        //
        Assert.state(outGivingNodeProjects.size() > 1, "当前分发只有一个项目啦,删除整个分发即可");
        outGivingNodeProjects = outGivingNodeProjects.stream()
            .filter(nodeProject -> !java.util.Objects.equals(nodeProject.getProjectId(), projectId) || !java.util.Objects.equals(nodeProject.getNodeId(), nodeId))
            .collect(Collectors.toList());
        // 更新
        OutGivingModel update = new OutGivingModel();
        update.setId(outGivingModel.getId());
        update.outGivingNodeProjectList(outGivingNodeProjects);
        outGivingServer.updateById(update);
        return ApiResult.success("删除成功");
    }
}
