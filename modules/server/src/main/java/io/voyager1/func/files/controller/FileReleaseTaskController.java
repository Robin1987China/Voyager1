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

package io.voyager1.func.files.controller;

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.controller.outgiving.OutGivingWhitelistService;
import io.voyager1.func.files.model.FileReleaseTaskLogModel;
import io.voyager1.func.files.model.FileReleaseTaskTemplate;
import io.voyager1.func.files.model.IFileStorage;
import io.voyager1.func.files.service.FileReleaseTaskService;
import io.voyager1.func.files.service.FileReleaseTaskTemplateService;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.ServerWhitelist;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2023/3/18
 */
@RestController
@RequestMapping(value = "/file-storage/release-task")
@Feature(cls = ClassFeature.FILE_STORAGE_RELEASE)
public class FileReleaseTaskController extends BaseServerController {

    private final FileReleaseTaskService fileReleaseTaskService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final ScriptServer scriptServer;
    private final FileReleaseTaskTemplateService fileReleaseTaskTemplateService;

    public FileReleaseTaskController(FileReleaseTaskService fileReleaseTaskService,
                                     OutGivingWhitelistService outGivingWhitelistService,
                                     NodeService nodeService,
                                     ScriptServer scriptServer,
                                     FileReleaseTaskTemplateService fileReleaseTaskTemplateService) {
        this.fileReleaseTaskService = fileReleaseTaskService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.scriptServer = scriptServer;
        this.fileReleaseTaskTemplateService = fileReleaseTaskTemplateService;
        this.nodeService = nodeService;
    }


    @PostMapping(value = "add-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> addTask(@ValidatorItem(msg = "文件 ID 缺失") String fileId,
                                        @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "请选择文件类型") Integer fileType,
                                        @ValidatorItem(msg = "请填写任务名称") String name,
                                        @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "请选择发布方式") int taskType,
                                        @ValidatorItem(msg = "请选择关联数据 ID") String taskDataIds,
                                        @ValidatorItem(msg = "请选择发布目录") String releasePathParent,
                                        @ValidatorItem(msg = "请填写发布的二级目录") String releasePathSecondary,
                                        String beforeScript,
                                        String afterScript,
                                        String save2Template,
                                        HttpServletRequest request) {
        // 判断参数
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, releasePathParent), "请选择正确的项目路径,或者还没有配置授权");
        Assert.hasText(releasePathSecondary, "请填写发布文件的二级目录");

        if ((beforeScript != null && beforeScript.startsWith(ServerConst.REF_SCRIPT))) {
            String scriptId = (beforeScript != null && beforeScript.startsWith(ServerConst.REF_SCRIPT) ? beforeScript.substring(ServerConst.REF_SCRIPT.length()) : beforeScript);
            ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, "请选择正确的发布前脚本");
            Assert.notNull(keyAndGlobal, "请选择正确的发布前脚本");
        }
        if ((afterScript != null && afterScript.startsWith(ServerConst.REF_SCRIPT))) {
            String scriptId = (afterScript != null && afterScript.startsWith(ServerConst.REF_SCRIPT) ? afterScript.substring(ServerConst.REF_SCRIPT.length()) : afterScript);
            ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, "请选择正确的发布后脚本");
            Assert.notNull(keyAndGlobal, "请选择正确的发布后脚本");
        }

        String releasePath = FileUtil.normalize(releasePathParent + "/" + releasePathSecondary);

        IFileStorage storageModel = fileReleaseTaskService.addTask(fileId, fileType, name, taskType, taskDataIds, releasePath, beforeScript, afterScript, null, request);
        // 判断是否需要存储为模板
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("releasePath", releasePath);
        jsonObject.put("save2Template", save2Template);
        jsonObject.put("fileType", fileType);
        jsonObject.put("fileId", fileId);
        jsonObject.put("taskType", taskType);
        jsonObject.put("taskDataIds", taskDataIds);
        jsonObject.put("releasePathParent", releasePathParent);
        jsonObject.put("releasePathSecondary", releasePathSecondary);
        jsonObject.put("beforeScript", beforeScript);
        jsonObject.put("afterScript", afterScript);
        String workspaceId = fileReleaseTaskTemplateService.getCheckUserWorkspace(request);
        fileReleaseTaskTemplateService.add(save2Template, workspaceId, storageModel, fileType, jsonObject);
        return ApiResult.success("创建成功");
    }


    /**
     * 重建-重新发布
     *
     * @param fileId       文件id
     * @param name         任务名
     * @param taskType     任务类型
     * @param taskDataIds  任务关联数据id
     * @param parentTaskId 父级任务id
     * @param beforeScript 发布之前的脚步
     * @param afterScript  发布之后的脚步
     * @param request      请求
     * @return json
     */
    @PostMapping(value = "re-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public ApiResult<String> reTask(@ValidatorItem(msg = "文件 ID 缺失") String fileId,
                                       @ValidatorItem(msg = "请填写任务名称") String name,
                                       @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "请选择发布方式") int taskType,
                                       @ValidatorItem(msg = "请选择关联数据 ID") String taskDataIds,
                                       @ValidatorItem(msg = "父任务id缺失") String parentTaskId,
                                       String beforeScript,
                                       String afterScript,
                                       HttpServletRequest request) {
        FileReleaseTaskLogModel parentTask = fileReleaseTaskService.getByKey(parentTaskId, request);
        Assert.notNull(parentTask, "父任务不存在");
        Integer fileType = parentTask.getFileType();
        fileType = (fileType != null ? fileType : 1);
        fileReleaseTaskService.addTask(fileId, fileType, name, taskType, taskDataIds, parentTask.getReleasePath(), beforeScript, afterScript, null, request);
        return ApiResult.success("创建成功");
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<FileReleaseTaskLogModel>> list(HttpServletRequest request) {
        //
        PageResultDto<FileReleaseTaskLogModel> listPage = fileReleaseTaskService.listPage(request);
        return ApiResult.success("", listPage);
    }


    /**
     * 发布模块列表
     *
     * @return json
     */
    @PostMapping(value = "list-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<FileReleaseTaskTemplate>> listTemplate(HttpServletRequest request) {
        //
        PageResultDto<FileReleaseTaskTemplate> listPage = fileReleaseTaskTemplateService.listPage(request);
        return ApiResult.success("", listPage);
    }

    /**
     * 获取模板
     *
     * @param id 模板id
     * @return json
     */
    @GetMapping(value = "get-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<FileReleaseTaskTemplate> getTemplate(@ValidatorItem(msg = "id不能为空") String id,
                                                             String alias,
                                                             Integer fileType,
                                                             HttpServletRequest request) {
        String workspaceId = fileReleaseTaskTemplateService.getCheckUserWorkspace(request);
        String templateTag = (alias != null && !alias.isEmpty()) ? ("alias:" + alias) : ("id:" + id);
        FileReleaseTaskTemplate template = fileReleaseTaskTemplateService.getTemplate(workspaceId, fileType, templateTag);
        if (template == null && (alias != null && !alias.isEmpty())) {
            template = fileReleaseTaskTemplateService.getTemplate(workspaceId, fileType, "id:" + id);
        }
        return ApiResult.success("", template);
    }

    /**
     * 删除模板
     *
     * @param id 模板id
     * @return json
     */
    @GetMapping(value = "delete-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<JSONObject> deleteTemplate(@ValidatorItem(msg = "id不能为空") String id, HttpServletRequest request) {
        //
        fileReleaseTaskTemplateService.delByKey(id, request);
        return ApiResult.success("删除成功");
    }

    /**
     * 取消任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "cancel-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> hasFile(@ValidatorItem(msg = "任务id缺失") String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, "不存在对应的任务");
        fileReleaseTaskService.cancelTask(taskLogModel.getId());
        return ApiResult.success("取消成功");
    }

    /**
     * 查询任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "details", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> details(@ValidatorItem(msg = "任务id缺失") String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, "不存在对应的任务");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("taskData", taskLogModel);
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskLogModel.getId());
        List<FileReleaseTaskLogModel> logModels = fileReleaseTaskService.listByBean(fileReleaseTaskLogModel);
        jsonObject.put("taskList", logModels);
        return ApiResult.success("取消成功", jsonObject);
    }

    /**
     * 删除任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<JSONObject> delete(@ValidatorItem(msg = "任务id缺失") String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, "不存在对应的任务");

        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskLogModel.getId());
        List<FileReleaseTaskLogModel> logModels = fileReleaseTaskService.listByBean(fileReleaseTaskLogModel);
        if (logModels != null) {
            List<String> ids = logModels.stream()
                .map(logModel -> {
                    File file = fileReleaseTaskService.logFile(logModel);
                    FileUtil.del(file);
                    return logModel.getId();
                })
                .collect(Collectors.toList());
            fileReleaseTaskService.delByKey(ids, null);
        }
        File taskDir = fileReleaseTaskService.logTaskDir(taskLogModel);
        FileUtil.del(taskDir);
        //
        fileReleaseTaskService.delByKey(taskLogModel.getId());
        return ApiResult.success("删除成功");
    }

    /**
     * 获取日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     */
    @GetMapping(value = "log-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                        @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "行号错误") int line,
                                        HttpServletRequest request) {
        FileReleaseTaskLogModel item = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(item, "没有对应数据");
        File file = fileReleaseTaskService.logFile(item);
        if (!FileUtil.isFile(file)) {
            return new ApiResult<>(405, "还没有日志信息或者日志文件错误");
        }

        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = item.getStatus();
        data.put("run", status != null && (status == 0 || status == 1));
        data.put("status", item.getStatus());
        return ApiResult.success("", data);
    }

}
