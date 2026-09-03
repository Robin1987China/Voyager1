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


import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.NodeScriptExecuteLogCacheModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.node.script.NodeScriptExecuteLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2021/12/24
 */
@RestController
@RequestMapping(value = "/node/script_log")
@Feature(cls = ClassFeature.NODE_SCRIPT_LOG)
public class NodeScriptLogController extends BaseServerController {

    private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;

    public NodeScriptLogController(NodeScriptExecuteLogServer nodeScriptExecuteLogServer) {
        this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
    }

    /**
     * get script log list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<PageResultDto<NodeScriptExecuteLogCacheModel>> scriptList(HttpServletRequest request) {
        PageResultDto<NodeScriptExecuteLogCacheModel> pageResultDto = nodeScriptExecuteLogServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * 查日志
     *
     * @return json
     */
    @RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Object> log(HttpServletRequest request) {
        NodeModel node = getNode();
        return NodeForward.request(node, request, NodeUrl.SCRIPT_LOG);
    }

    /**
     * 删除日志
     *
     * @param id        模版ID
     * @param executeId 日志ID
     * @return json
     */
    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem String id, String executeId, HttpServletRequest request) {
        NodeModel node = getNode();
        NodeScriptExecuteLogCacheModel executeLogModel = nodeScriptExecuteLogServer.getByKey(executeId, request);
        Assert.notNull(executeLogModel, "没有对应的执行日志");
        Assert.state(java.util.Objects.equals(id, executeLogModel.getScriptId()), "数据关联的id 不一致");
//        NodeScriptExecuteLogCacheModel nodeScriptExecuteLogCacheModel = new NodeScriptExecuteLogCacheModel();
//        nodeScriptExecuteLogCacheModel.setId(executeId);
//        nodeScriptExecuteLogCacheModel.setScriptId(id);
//        nodeScriptExecuteLogCacheModel.setNodeId(node.getId());
//        NodeScriptExecuteLogCacheModel executeLogModel = nodeScriptExecuteLogServer.queryByBean(nodeScriptExecuteLogCacheModel);

        ApiResult<Object> jsonMessage = NodeForward.request(node, request, NodeUrl.SCRIPT_DEL_LOG);
        if (jsonMessage.success()) {
            nodeScriptExecuteLogServer.delByKey(executeId);
        }
        return jsonMessage;
    }
}
