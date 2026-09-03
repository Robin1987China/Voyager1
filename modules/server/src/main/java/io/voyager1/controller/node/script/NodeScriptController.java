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

package io.voyager1.controller.node.script;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
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
import io.voyager1.model.node.NodeScriptCacheModel;
import io.voyager1.model.node.ProjectInfoCacheModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.*;
import io.voyager1.service.node.script.NodeScriptExecuteLogServer;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 脚本管理
 *
 * @since 2019/4/24
 */
@RestController
@RequestMapping(value = "/node/script")
@Feature(cls = ClassFeature.NODE_SCRIPT)
@NodeDataPermission(cls = NodeScriptServer.class)
public class NodeScriptController extends BaseServerController {

    private final NodeScriptServer nodeScriptServer;
    private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public NodeScriptController(NodeScriptServer nodeScriptServer,
                                NodeScriptExecuteLogServer nodeScriptExecuteLogServer,
                                TriggerTokenLogServer triggerTokenLogServer) {
        this.nodeScriptServer = nodeScriptServer;
        this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * load node script list
     * 加载节点脚本列表
     *
     * @return json
     */
    @PostMapping(value = "list_all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<PageResultDto<NodeScriptCacheModel>> listAll(HttpServletRequest request) {
        PageResultDto<NodeScriptCacheModel> modelPageResultDto = nodeScriptServer.listPage(request);
        return ApiResult.success("", modelPageResultDto);
    }


    private void checkProjectPermission(String id, HttpServletRequest request, NodeModel node) {
        if ((id == null || id.isEmpty())) {
            return;
        }
        String workspaceId = nodeScriptServer.getCheckUserWorkspace(request);
        String fullId = ProjectInfoCacheModel.fullId(workspaceId, node.getId(), id);
        boolean exists = nodeScriptServer.exists(fullId);
        if (!exists) {
            // 判断全局脚本
            NodeScriptCacheModel nodeScriptCacheModel = new NodeScriptCacheModel();
            nodeScriptCacheModel.setScriptId(id);
            nodeScriptCacheModel.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
            exists = nodeScriptServer.exists(nodeScriptCacheModel);
            if (exists) {
                return;
            }
        }
        Assert.state(exists, "没有对应的数据或者没有此数据权限");
    }

    @GetMapping(value = "item.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Object> item(HttpServletRequest request, String id) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        return NodeForward.request(node, request, NodeUrl.Script_Item);
    }

    /**
     * 保存脚本
     *
     * @return json
     */
    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> save(String id, String autoExecCron, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        this.checkCron(autoExecCron);
        ApiResult<Object> jsonMessage = NodeForward.request(node, request, NodeUrl.Script_Save, new String[]{}, "nodeId", node.getId());
        if (jsonMessage.success()) {
            nodeScriptServer.syncNode(node);
        }
        return jsonMessage;
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        ApiResult<Object> requestData = NodeForward.request(node, request, NodeUrl.Script_Del);
        if (requestData.success()) {
            nodeScriptServer.syncNode(node);
            // 删除日志
            nodeScriptExecuteLogServer.delCache(id, node.getId(), request);
        }
        return requestData;
    }

    /**
     * 同步脚本模版
     *
     * @return json
     */
    @GetMapping(value = "sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> syncProject(HttpServletRequest request) {
        //
        NodeModel node = getNode();
        int cache = nodeScriptServer.delCache(node.getId(), request);
        String msg = nodeScriptServer.syncExecuteNode(node);
        return ApiResult.success("主动清除" + cache + " " + msg);
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
        nodeScriptServer.delByKey(id, request);
        return ApiResult.success("解绑成功");
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
        NodeScriptCacheModel item = nodeScriptServer.getByKeyAndGlobal(id, request);
        UserModel user = getUser();
        NodeScriptCacheModel updateInfo;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateInfo = new NodeScriptCacheModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), nodeScriptServer.typeName(),
                item.getId(), user.getId()));
            nodeScriptServer.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(NodeScriptCacheModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.NODE_SCRIPT_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.NODE_SCRIPT_TRIGGER_BATCH);
        map.put("batchTriggerUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
