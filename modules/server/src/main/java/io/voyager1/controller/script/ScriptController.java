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

package io.voyager1.controller.script;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.script.CommandParam;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.service.script.ScriptExecuteLogServer;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 服务端脚本
 *
 * @since 2022/1/19
 */
@RestController
@RequestMapping(value = "/script")
@Feature(cls = ClassFeature.SCRIPT)
public class ScriptController extends BaseServerController {

    private final ScriptServer scriptServer;
    private final NodeScriptServer nodeScriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final TriggerTokenLogServer triggerTokenLogServer;
    private final WorkspaceService workspaceService;

    public ScriptController(ScriptServer scriptServer,
                            NodeScriptServer nodeScriptServer,
                            ScriptExecuteLogServer scriptExecuteLogServer,
                            TriggerTokenLogServer triggerTokenLogServer,
                            WorkspaceService workspaceService) {
        this.scriptServer = scriptServer;
        this.nodeScriptServer = nodeScriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.workspaceService = workspaceService;
    }

    /**
     * get script list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<ScriptModel>> scriptList(HttpServletRequest request) {
        PageResultDto<ScriptModel> pageResultDto = scriptServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * get script list
     *
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<ScriptModel>> scriptListAll(HttpServletRequest request) {
        List<ScriptModel> pageResultDto = scriptServer.listByWorkspace(request);
        return ApiResult.success("", pageResultDto);
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(String id,
                                     @ValidatorItem String context,
                                     @ValidatorItem String name,
                                     String autoExecCron,
                                     String defArgs,
                                     String description,
                                     String nodeIds,
                                     HttpServletRequest request) {
        ScriptModel scriptModel = new ScriptModel();
        scriptModel.setId(id);
        scriptModel.setContext(context);
        scriptModel.setName(name);
        scriptModel.setNodeIds(nodeIds);
        scriptModel.setDescription(description);
        scriptModel.setDefArgs(CommandParam.checkStr(defArgs));
        scriptModel.setWorkspaceId(scriptServer.covertGlobalWorkspace(request));

        Assert.hasText(scriptModel.getContext(), "内容为空");
        //
        scriptModel.setAutoExecCron(this.checkCron(autoExecCron));
        //
        String oldNodeIds = null;
        if ((id == null || id.isEmpty())) {
            scriptServer.insert(scriptModel);
        } else {
            ScriptModel byKey = scriptServer.getByKeyAndGlobal(id, request);
            Assert.notNull(byKey, "没有对应的数据");
            oldNodeIds = byKey.getNodeIds();
            scriptServer.updateById(scriptModel, request);
        }
        this.syncNodeScript(scriptModel, oldNodeIds, request);
        return ApiResult.success("修改成功");
    }

    private void syncDelNodeScript(ScriptModel scriptModel, Collection<String> delNode) {
        for (String nodeId : delNode) {
            NodeModel byKey = nodeService.getByKey(nodeId);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            ApiResult<String> request = NodeForward.request(byKey, NodeUrl.Script_Del, jsonObject);
            Assert.state(request.getCode() == 200, String.format("处理 %s 节点删除脚本失败%s", byKey.getName(), request.getMsg()));
            nodeScriptServer.syncNode(byKey);
        }
    }

    private void syncNodeScript(ScriptModel scriptModel, String oldNode, HttpServletRequest request) {
        List<String> oldNodeIds = io.voyager1.util.ConvertUtil.splitTrim(oldNode, ",");
        // StrUtil.splitTrim(null) 返回 null，nodeIds 为空时需得到空集合
        List<String> newNodeIds = StrUtil.splitTrim(scriptModel.getNodeIds(), ",");
        if (newNodeIds == null) {
            newNodeIds = new java.util.ArrayList<>();
        }
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        // 删除
        this.syncDelNodeScript(scriptModel, delNode);
        // 更新
        for (String newNodeId : newNodeIds) {
            NodeModel byKey = nodeService.getByKey(newNodeId);
            Assert.notNull(byKey, "没有找到对应的节点");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            jsonObject.put("type", "sync");
            jsonObject.put("context", scriptModel.getContext());
            jsonObject.put("autoExecCron", scriptModel.getAutoExecCron());
            jsonObject.put("defArgs", scriptModel.getDefArgs());
            jsonObject.put("description", scriptModel.getDescription());
            jsonObject.put("name", scriptModel.getName());
            jsonObject.put("workspaceId", byKey.getWorkspaceId());
            jsonObject.put("global", scriptModel.global());
            jsonObject.put("nodeId", byKey.getId());
            ApiResult<String> jsonMessage = NodeForward.request(byKey, NodeUrl.Script_Save, jsonObject);
            Assert.state(jsonMessage.success(), String.format("处理 %s 节点同步脚本失败 %s", byKey.getName(), jsonMessage.getMsg()));
            nodeScriptServer.syncNode(byKey);
        }
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id, HttpServletRequest request) {
        ScriptModel server = scriptServer.getByKeyAndGlobal(id, request);
        if (server != null) {
            File file = server.scriptPath();
            // 脚本文件存在则尝试清理；文件不存在或清理失败不阻止删除记录，避免记录僵死无法删除
            if (file != null) {
                FileUtil.del(file);
            }
            // 删除节点中的脚本
            String nodeIds = server.getNodeIds();
            List<String> delNode = io.voyager1.util.ConvertUtil.splitTrim(nodeIds, ",");
            this.syncDelNodeScript(server, delNode);
            scriptServer.delByKey(id, request);
            //
            scriptExecuteLogServer.delByWorkspace(request, entity -> entity.set("scriptId", id));
        }
        return ApiResult.success("删除成功");
    }

    @GetMapping(value = "get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> get(String id, HttpServletRequest request) {
        String workspaceId = scriptServer.getCheckUserWorkspace(request);
        ScriptModel server = scriptServer.getByKeyAndGlobal(id, request);
        Assert.notNull(server, "没有对应的脚本");
        String nodeIds = server.getNodeIds();
        List<String> newNodeIds = io.voyager1.util.ConvertUtil.splitTrim(nodeIds, ",");
        List<JSONObject> nodeList = newNodeIds.stream()
            .map(s -> {
                JSONObject jsonObject = new JSONObject();
                NodeModel nodeModel = nodeService.getByKey(s);
                if (nodeModel == null) {
                    jsonObject.put("nodeName", "未知(数据丢失)");
                } else {
                    jsonObject.put("nodeName", nodeModel.getName());
                    jsonObject.put("nodeId", nodeModel.getId());
                    jsonObject.put("workspaceId", nodeModel.getWorkspaceId());
                    WorkspaceModel workspaceModel = workspaceService.getByKey(nodeModel.getWorkspaceId());
                    jsonObject.put("workspaceName", Optional.ofNullable(workspaceModel).map(WorkspaceModel::getName).orElse("未知(数据丢失)"));
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
        // 判断是否可以编辑节点
        boolean prohibitSync = nodeList.stream()
            .anyMatch(jsonObject -> {
                String workspaceId11 = (String) jsonObject.get("workspaceId");
                return !java.util.Objects.equals(workspaceId11, workspaceId);
            });
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", server);
        jsonObject.put("nodeList", nodeList);
        jsonObject.put("prohibitSync", prohibitSync);
        return ApiResult.success("", jsonObject);
    }

    /**
     * 释放脚本关联的节点
     *
     * @param id 脚本ID
     * @return json
     */
    @RequestMapping(value = "unbind.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public ApiResult<String> unbind(@ValidatorItem String id, HttpServletRequest request) {
        ScriptModel update = new ScriptModel();
        update.setId(id);
        update.setNodeIds("");
        scriptServer.updateById(update, request);
        return ApiResult.success("解绑成功");
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids           节点ID
     * @param toWorkspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public ApiResult<String> syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String toWorkspaceId, HttpServletRequest request) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        scriptServer.checkUserWorkspace(toWorkspaceId);
        scriptServer.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return ApiResult.success("操作成功");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        ScriptModel item = scriptServer.getByKey(id, request);
        UserModel user = getUser();
        ScriptModel updateInfo;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateInfo = new ScriptModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), scriptServer.typeName(),
                item.getId(), user.getId()));
            scriptServer.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(ScriptModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SERVER_SCRIPT_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.SERVER_SCRIPT_TRIGGER_BATCH);
        map.put("batchTriggerUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
