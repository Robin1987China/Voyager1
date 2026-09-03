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

package io.voyager1.controller.node.manage;
import io.voyager1.util.CsvUtil;
import io.voyager1.util.CsvWriteConfig;
import io.voyager1.util.CsvRow;
import io.voyager1.util.CsvData;

import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.CharsetDetector;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.CsvReadConfig;
import io.voyager1.util.CsvReader;
import io.voyager1.util.CsvWriter;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.BaseNodeModel;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.RunMode;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.MonitorModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.RepositoryModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.permission.*;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.dblog.RepositoryService;
import io.voyager1.service.monitor.MonitorService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.outgiving.LogReadServer;
import io.voyager1.service.outgiving.OutGivingServer;
import io.voyager1.service.system.WhitelistDirectoryService;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目管理
 *
 * @since 2018/9/29
 */
@RestController
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
@Slf4j
public class ProjectManageControl extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final LogReadServer logReadServer;
    private final MonitorService monitorService;
    private final BuildInfoService buildService;
    private final RepositoryService repositoryService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final ServerConfig serverConfig;
    private final WhitelistDirectoryService whitelistDirectoryService;

    public ProjectManageControl(OutGivingServer outGivingServer,
                                LogReadServer logReadServer,
                                MonitorService monitorService,
                                BuildInfoService buildService,
                                RepositoryService repositoryService,
                                ProjectInfoCacheService projectInfoCacheService,
                                DbBuildHistoryLogService dbBuildHistoryLogService,
                                ServerConfig serverConfig,
                                WhitelistDirectoryService whitelistDirectoryService) {
        this.outGivingServer = outGivingServer;
        this.logReadServer = logReadServer;
        this.monitorService = monitorService;
        this.buildService = buildService;
        this.repositoryService = repositoryService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.serverConfig = serverConfig;
        this.whitelistDirectoryService = whitelistDirectoryService;
    }


    private void checkProjectPermission(String id, HttpServletRequest request, NodeModel node) {
        if ((id == null || id.isEmpty())) {
            return;
        }
        String workspaceId = projectInfoCacheService.getCheckUserWorkspace(request);
        String fullId = ProjectInfoCacheModel.fullId(workspaceId, node.getId(), id);
        boolean exists = projectInfoCacheService.exists(fullId);
        if (!exists) {
            // 判断如果项目 id 不存在则表示新增
            ProjectInfoCacheModel projectInfoCacheModel = new ProjectInfoCacheModel();
            projectInfoCacheModel.setProjectId(id);
            projectInfoCacheModel.setNodeId(node.getId());
            boolean exists1 = projectInfoCacheService.exists(projectInfoCacheModel);
            if (!exists1) {
                // 新增数据
                return;
            }
        }
        Assert.state(exists, "没有对应的数据或者没有此数据权限");
    }

    @RequestMapping(value = "getProjectData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> getProjectData(@ValidatorItem String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        JSONObject projectInfo = projectInfoCacheService.getItem(node, id);
        return ApiResult.success("", projectInfo);
    }

    /**
     * get project access list
     * 获取项目的授权
     *
     * @return json
     */
    @RequestMapping(value = "project-access-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<String>> projectAccessList() {
        List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
        return ApiResult.success("", jsonArray);
    }

    /**
     * 保存项目
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveProject(String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        //
        ApiResult<String> jsonMessage = NodeForward.request(node, request, NodeUrl.Manage_SaveProject, "outGivingProject");
        if (jsonMessage.success()) {
            projectInfoCacheService.syncNode(node, id);
        }
        return jsonMessage;
    }


    /**
     * 释放分发
     *
     * @return json
     */
    @RequestMapping(value = "release-outgiving", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> releaseOutgiving(String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        ApiResult<String> jsonMessage = NodeForward.request(getNode(), request, NodeUrl.Manage_ReleaseOutGiving);
        if (jsonMessage.success()) {
            projectInfoCacheService.syncNode(node, id);
        }
        return jsonMessage;
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> getProjectPort(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_GetProjectPort);
    }


    /**
     * 查询所有项目
     *
     * @return json
     */
    @PostMapping(value = "get_project_info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<ProjectInfoCacheModel>> getProjectInfo(HttpServletRequest request) {
        PageResultDto<ProjectInfoCacheModel> modelPageResultDto = projectInfoCacheService.listPage(request);
        return ApiResult.success("", modelPageResultDto);
    }

    /**
     * 删除项目
     *
     * @param id id
     * @return json
     */
    @PostMapping(value = "deleteProject", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> deleteProject(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id,

                                              HttpServletRequest request) {
        NodeModel nodeModel = getNode();
        this.checkProjectPermission(id, request, nodeModel);
        // 检查节点分发
        outGivingServer.checkNodeProject(nodeModel.getId(), id, request, "当前项目存在节点分发，不能直接删除");
        // 检查日志阅读
        logReadServer.checkNodeProject(nodeModel.getId(), id, request, "当前项目存在日志阅读，不能直接删除");
        // 项目监控
        List<MonitorModel> monitorModels = monitorService.listByWorkspace(request);
        if (monitorModels != null) {
            boolean match = monitorModels.stream().anyMatch(monitorModel -> monitorModel.checkNodeProject(nodeModel.getId(), id));
            Assert.state(!match, "当前项目存在监控项，不能直接删除");
        }
        // 构建
        boolean releaseMethod = buildService.checkReleaseMethod(nodeModel.getId() + ":" + id, request, BuildReleaseMethod.Project);
        Assert.state(!releaseMethod, "当前项目存在构建项，不能直接删除");

        ApiResult<String> jsonMessage = NodeForward.request(nodeModel, request, NodeUrl.Manage_DeleteProject);
        if (jsonMessage.success()) {
            //
            projectInfoCacheService.syncExecuteNode(nodeModel);
        }
        return jsonMessage;
    }

    /**
     * 查看项目关联在线构建的数据
     *
     * @param projectData   项目数据
     * @param request       请求
     * @param toWorkspaceId 工作空间ID
     * @return list
     */
    private List<Tuple> checkBuild(ProjectInfoCacheModel projectData, String toWorkspaceId, HttpServletRequest request) {
        // 构建
        String dataId = projectData.getNodeId() + ":" + projectData.getProjectId();
        List<BuildInfoModel> buildInfoModels = buildService.listReleaseMethod(dataId, request, BuildReleaseMethod.Project);
        if (buildInfoModels != null) {
            return buildInfoModels.stream()
                .map(buildInfoModel -> {
                    // 判断共享仓库
                    RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId());
                    Assert.notNull(repositoryModel, "仓库不存在");
                    if (java.util.Objects.equals(repositoryModel.getWorkspaceId(), toWorkspaceId)) {
                        // 迁移前后是同一个工作空间
                        return null;
                    }
                    // 非全局仓库判断仓库关联的构建
                    if (!repositoryModel.global()) {
                        BuildInfoModel buildInfoModel1 = new BuildInfoModel();
                        buildInfoModel1.setRepositoryId(buildInfoModel.getRepositoryId());
                        buildInfoModel1.setWorkspaceId(projectData.getWorkspaceId());
                        List<BuildInfoModel> infoModels = buildService.listByBean(buildInfoModel1);
                        if ((infoModels == null ? 0 : infoModels.size()) > 1) {
                            // 判断如果使用通过一个仓库
                            long count = infoModels.stream()
                                .filter(buildInfoModel2 -> {
                                    // 发布方式和数据id 不一样
                                    return !java.util.Objects.equals(buildInfoModel2.getReleaseMethodDataId(), dataId);
                                })
                                .count();
                            Assert.state(count <= 0, String.format("当前【项目】关联的【在线构建】关联的【仓库(%s)】被其他 %s 个不同发布方式的【在线构建】绑定暂不支持迁移", repositoryModel.getName(), count));
                        }
                    }
                    return new Tuple(buildInfoModel, repositoryModel);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * 迁移项目关联在线构建的数据
     *
     * @param list          构建数据
     * @param toWorkspaceId 迁移到哪个工作空间
     * @return list
     */
    private String migrateBuild(List<Tuple> list, String toWorkspaceId, String toNodeId, ProjectInfoCacheModel projectData) {
        return list.stream()
            .map(tuple -> {
                BuildInfoModel infoModel = tuple.get(0);
                RepositoryModel repository = tuple.get(1);
                if (!repository.global()) {
                    // 非全局仓库才 修改仓库所属工作空间
                    String repositoryId = infoModel.getRepositoryId();
                    RepositoryModel repositoryModel = new RepositoryModel();
                    repositoryModel.setId(repositoryId);
                    repositoryModel.setWorkspaceId(toWorkspaceId);
                    repositoryService.updateById(repositoryModel);
                }
                //
                BuildInfoModel buildInfoModel = new BuildInfoModel();
                buildInfoModel.setId(infoModel.getId());
                buildInfoModel.setWorkspaceId(toWorkspaceId);
                // 修改发布的关联数据
                buildInfoModel.setReleaseMethodDataId(toNodeId + ":" + projectData.getProjectId());
                buildService.updateById(buildInfoModel);
                // 修改构建记录
                dbBuildHistoryLogService.update(
                    Entity.create().set("workspaceId", toWorkspaceId),
                    Entity.create().set("buildDataId", infoModel.getId())
                );
                if (!repository.global()) {
                    return String.format("自动迁移关联的构建：%s 和 仓库：%s", infoModel.getName(), repository.getName());
                }
                return String.format("自动迁移关联的构建：%s", infoModel.getName());
            }).
            collect(Collectors.joining(" | "));
    }

    /**
     * 迁移工作空间
     *
     * @param id id
     * @return json
     */
    @PostMapping(value = "migrate-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission
    public ApiResult<String> migrateWorkspace(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id,
                                                 @ValidatorItem(value = ValidatorRule.NOT_BLANK) String toWorkspaceId,
                                                 @ValidatorItem(value = ValidatorRule.NOT_BLANK) String toNodeId,
                                                 HttpServletRequest request) {
        ProjectInfoCacheModel projectData = projectInfoCacheService.getByKey(id, request);
        Assert.notNull(projectData, "项目不存在");

        Assert.state(!java.util.Objects.equals(toWorkspaceId, projectData.getWorkspaceId()) || !java.util.Objects.equals(projectData.getNodeId(), toNodeId), "目标工作空间与当前工作空间一致并且目标节点与当前节点一致");
        projectInfoCacheService.checkUserWorkspace(toWorkspaceId);
        //
        NodeModel nowNode = nodeService.getByKey(projectData.getNodeId());
        Assert.notNull(nowNode, "当前对应的节点不存在");
        NodeModel toNodeModel = nodeService.getByKey(toNodeId);
        Assert.notNull(toNodeModel, "对应的节点不存在");
        Assert.state(java.util.Objects.equals(toWorkspaceId, toNodeModel.getWorkspaceId()), "要迁移到的目标工作空间和节点不一致");
        // 检查节点分发
        outGivingServer.checkNodeProject(projectData.getNodeId(), projectData.getProjectId(), request, "当前项目存在节点分发，不能直接迁移");
        // 检查日志阅读
        logReadServer.checkNodeProject(projectData.getNodeId(), projectData.getProjectId(), request, "当前项目存在日志阅读，不能直接迁移");
        // 项目监控
        List<MonitorModel> monitorModels = monitorService.listByWorkspace(request);
        if (monitorModels != null) {
            boolean match = monitorModels.stream().anyMatch(monitorModel -> monitorModel.checkNodeProject(projectData.getNodeId(), id));
            Assert.state(!match, "当前项目存在监控项，不能直接迁移");
        }
        // 检查构建
        List<Tuple> buildInfoModels = this.checkBuild(projectData, toWorkspaceId, request);
        ApiResult<String> result;
        if (java.util.Objects.equals(nowNode.getMachineId(), toNodeModel.getMachineId())) {
            // 相同机器
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("newWorkspaceId", toWorkspaceId);
            jsonObject.put("newNodeId", toNodeId);
            jsonObject.put("id", projectData.getProjectId());
            ApiResult<String> jsonMessage = NodeForward.request(nowNode, NodeUrl.Manage_ChangeWorkspaceId, jsonObject);
            if (!jsonMessage.success()) {
                return new ApiResult<>(406, nowNode.getName() + "节点迁移项目失败" + jsonMessage.getMsg());
            }
            result = jsonMessage;
        } else {
            JSONObject item = projectInfoCacheService.getItem(nowNode, projectData.getProjectId());
            Assert.notNull(item, "项目数据丢失");
            item = projectInfoCacheService.convertToRequestData(item);
            item.put("nodeId", toNodeId);
            item.put("workspaceId", toWorkspaceId);
            item.put("previewData", true);
            // 发起预检查数据
            ApiResult<String> jsonMessage = NodeForward.request(toNodeModel, NodeUrl.Manage_SaveProject, item);
            if (!jsonMessage.success()) {
                return new ApiResult<>(406, toNodeModel.getName() + "节点与检查项目失败" + jsonMessage.getMsg());
            }
            item.remove("previewData");
            jsonMessage = NodeForward.request(toNodeModel, NodeUrl.Manage_SaveProject, item);
            if (!jsonMessage.success()) {
                return new ApiResult<>(406, toNodeModel.getName() + "节点同步项目失败" + jsonMessage.getMsg());
            }
            // 删除之前节点项目
            JSONObject delData = new JSONObject();
            delData.put("id", projectData.getProjectId());
            // 非强制
            delData.put("thorough", "");
            ApiResult<String> delJsonMeg = NodeForward.request(nowNode, NodeUrl.Manage_DeleteProject, delData);
            if (!delJsonMeg.success()) {
                return new ApiResult<>(406, nowNode.getName() + "节点删除项目失败" + delJsonMeg.getMsg());
            }
            result = jsonMessage;
        }
        // 迁移构建
        String buildMsg = this.migrateBuild(buildInfoModels, toWorkspaceId, toNodeId, projectData);
        // 刷新缓存
        projectInfoCacheService.syncExecuteNode(nowNode);
        projectInfoCacheService.syncExecuteNode(toNodeModel);
        return new ApiResult<>(200, String.format("项目迁移成功：%s | %s", result.getMsg(), buildMsg));
    }

    /**
     * 操作项目
     * <p>
     * nodeId,id
     *
     * @return json
     */
    @RequestMapping(value = "operate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<Object> operate(HttpServletRequest request) {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, request, NodeUrl.Manage_Operate);
    }


    /**
     * 下载导入模板
     */
    @GetMapping(value = "import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "项目导入模板.csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("id", "name", "groupName", "whitelistDirectory", "path", "logPath", "runMode",
            "mainClass",
            "jvm", "args",
            "javaExtDirsCp",
            "dslContent",
            "webHooks",
            "autoStart");
        writer.flush();
    }

    /**
     * 导出数据
     */
    @GetMapping(value = "export-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void exportData(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String workspace = projectInfoCacheService.getCheckUserWorkspace(request);
        String prefix = "导出的项目数据 ";
        String fileName = prefix + DateTime.now().toString("yyyy-MM-dd") + ".csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriteConfig csvWriteConfig = CsvWriteConfig.defaultConfig();
        csvWriteConfig.setAlwaysDelimitText(true);
        CsvWriter writer = CsvUtil.getWriter(response.getWriter(), csvWriteConfig);
        int pageInt = 0;
        writer.writeLine("id", "name", "groupName", "whitelistDirectory", "path", "logPath", "runMode",
            "mainClass",
            "jvm", "args", "javaExtDirsCp",
            "dslContent",
            "webHooks",
            "autoStart", "outGivingProject");
        while (true) {
            Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<ProjectInfoCacheModel> listPage = projectInfoCacheService.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<ProjectInfoCacheModel, List<Object>>) projectInfoCacheModel -> new java.util.ArrayList<>(java.util.Arrays.asList(projectInfoCacheModel.getProjectId())))
                .map(objects -> objects.stream().map(StrUtil::toStringOrNull).toArray(String[]::new))
                .forEach(writer::writeLine);
            if (java.util.Objects.equals(listPage.getPage(), listPage.getTotalPage())) {
                // 最后一页
                break;
            }
        }
        writer.flush();
    }


    private String encodeCsv(String data) {
        return data.replace("\n", "\"\n\"");
    }

    private String decodeCsv(String data) {
        return data.replace("\"\n\"", "\n");
    }

    /**
     * 导入数据
     *
     * @return json
     */
    @PostMapping(value = "import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<String> importData(MultipartFile file, HttpServletRequest request) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String workspaceId = projectInfoCacheService.getCheckUserWorkspace(request);
        NodeModel node = getNode();
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean csv = StrUtil.endWithIgnoreCase(extName, "csv");
        Assert.state(csv, "不允许的文件格式");
        assert originalFilename != null;
        File csvFile = FileUtil.file(serverConfig.getUserTempPath(), originalFilename);
        int updateCount = 0, ignoreCount = 0;
        Charset fileCharset;
        try {
            file.transferTo(csvFile);
            //
            fileCharset = CharsetDetector.detect(csvFile);
            Reader bomReader = FileUtil.getReader(csvFile, fileCharset);
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
            CsvData csvData;
            try {
                csvData = reader.read();
            } catch (Exception e) {
                log.error("解析项目 csv 异常", e);
                return new ApiResult<>(405, "解析文件异常," + e.getMessage());
            } finally {
                IoUtil.close(reader);
            }
            List<CsvRow> rows = csvData.getRows();
            Assert.notEmpty(rows, "没有任何数据");

            for (int i = 0; i < rows.size(); i++) {
                CsvRow csvRow = rows.get(i);
                JSONObject jsonObject = this.loadProjectData(csvRow, workspaceId, node);
                if (jsonObject == null) {
                    ignoreCount++;
                    continue;
                }
                try {
                    //
                    ApiResult<String> jsonMessage = NodeForward.request(node, NodeUrl.Manage_SaveProject, jsonObject);
                    if (jsonMessage.success()) {
                        updateCount++;
                        continue;
                    }
                    throw new IllegalArgumentException(String.format("导入第 %s 条数据保存失败:%s", i + 2, jsonMessage.getMsg()));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw Lombok.sneakyThrow(e);
                } catch (Exception e) {
                    log.error("导入保存项目异常", e);
                    throw new IllegalArgumentException(String.format("导入第 %s 条数据异常:%s", i + 2, e.getMessage()));
                }
            }
            projectInfoCacheService.syncExecuteNode(node);
        } finally {
            FileUtil.del(csvFile);
        }
        String fileCharsetStr = Optional.ofNullable(fileCharset).map(Charset::name).orElse("");
        return ApiResult.success("导入成功(编码格式：{}),更新 {} 条数据,因为节点分发/项目副本忽略 {} 条数据", fileCharsetStr, updateCount, ignoreCount);
    }

    private JSONObject loadProjectData(CsvRow csvRow, String workspaceId, NodeModel node) {
        String id = csvRow.getByName("id");
        String fullId = BaseNodeModel.fullId(workspaceId, node.getId(), id);

        ProjectInfoCacheModel projectInfoCacheModel1 = projectInfoCacheService.getByKey(fullId);
        if (projectInfoCacheModel1 != null) {
            // 节点分发项目不能在这里导入
            Boolean outGivingProject = projectInfoCacheModel1.getOutGivingProject();
            if (outGivingProject != null && outGivingProject) {
                return null;
            }
            if ((projectInfoCacheModel1.getJavaCopyItemList() != null && !projectInfoCacheModel1.getJavaCopyItemList().isEmpty())) {
                return null;
            }
        }
//        "id", "name", "groupName", "whitelistDirectory", "path", "logPath", "runMode",
//                "mainClass",
//                "jvm", "args", "javaExtDirsCp",
//                "dslContent",
//                "webHooks",
//                "autoStart", "outGivingProject"
        JSONObject data = new JSONObject();
        data.put("id", id);
        data.put("name", csvRow.getByName("name"));
        data.put("group", csvRow.getByName("groupName"));
        String runModeStr = csvRow.getByName("runMode");
        // 运行模式
        RunMode runMode1 = EnumUtil.fromString(RunMode.class, runModeStr, RunMode.ClassPath);
        data.put("runMode", runMode1.name());
        if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
            data.put("mainClass", csvRow.getByName("mainClass"));
        }
        if (runMode1 == RunMode.JavaExtDirsCp) {
            data.put("javaExtDirsCp", decodeCsv(csvRow.getByName("javaExtDirsCp")));
        }
        if (runMode1 == RunMode.Dsl) {
            data.put("dslContent", decodeCsv(csvRow.getByName("dslContent")));
        }
        data.put("whitelistDirectory", csvRow.getByName("whitelistDirectory"));
        data.put("logPath", csvRow.getByName("logPath"));
        data.put("lib", csvRow.getByName("path"));
        data.put("autoStart", csvRow.getByName("autoStart"));
        data.put("token", csvRow.getByName("webHooks"));
        data.put("jvm", decodeCsv(csvRow.getByName("jvm")));
        data.put("args", decodeCsv(csvRow.getByName("args")));
        return data;
    }
}
