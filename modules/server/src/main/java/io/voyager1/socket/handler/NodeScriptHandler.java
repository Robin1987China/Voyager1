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

package io.voyager1.socket.handler;

import io.voyager1.common.SpringContextHolder;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.node.NodeScriptCacheModel;
import io.voyager1.model.node.NodeScriptExecuteLogCacheModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.node.script.NodeScriptExecuteLogServer;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.transport.IProxyWebSocket;

import java.io.IOException;
import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @since 2019/4/24
 */
@Feature(cls = ClassFeature.NODE_SCRIPT, method = MethodFeature.EXECUTE)
public class NodeScriptHandler extends BaseProxyHandler {

    public NodeScriptHandler() {
        super(NodeUrl.Script_Run);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        NodeScriptCacheModel scriptModel = (NodeScriptCacheModel) attributes.get("dataItem");
        return new Object[]{"id", attributes.get("scriptId"), "workspaceId", scriptModel.getWorkspaceId()};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, IProxyWebSocket proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        if (consoleCommandOp != ConsoleCommandOp.heart) {
            super.logOpt(this.getClass(), attributes, json);
        }
        if (consoleCommandOp == ConsoleCommandOp.start) {
            // 开始执行
            String executeId = this.createLog(attributes);
            json.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            json.put("executeId", executeId);
        }
        proxySession.send(json.toString());
        return null;
    }

    /**
     * 创建执行日志
     *
     * @param attributes 参数属性
     * @return 执行ID
     */
    private String createLog(Map<String, Object> attributes) {
        NodeModel nodeInfo = (NodeModel) attributes.get("nodeInfo");
        UserModel userModel = (UserModel) attributes.get("userInfo");
        NodeScriptCacheModel dataItem = (NodeScriptCacheModel) attributes.get("dataItem");
        NodeScriptExecuteLogServer logServer = SpringContextHolder.getBean(NodeScriptExecuteLogServer.class);
        NodeScriptServer nodeScriptServer = SpringContextHolder.getBean(NodeScriptServer.class);
        //
        try {
            BaseServerController.resetInfo(userModel);
            //
            NodeScriptCacheModel nodeScriptCacheModel = new NodeScriptCacheModel();
            nodeScriptCacheModel.setId(dataItem.getId());
            nodeScriptCacheModel.setLastRunUser(userModel.getId());
            nodeScriptServer.updateById(nodeScriptCacheModel);
            //
            NodeScriptExecuteLogCacheModel nodeScriptExecuteLogCacheModel = new NodeScriptExecuteLogCacheModel();
            nodeScriptExecuteLogCacheModel.setScriptId(dataItem.getScriptId());
            nodeScriptExecuteLogCacheModel.setNodeId(nodeInfo.getId());
            nodeScriptExecuteLogCacheModel.setScriptName(dataItem.getName());
            nodeScriptExecuteLogCacheModel.setTriggerExecType(0);
            nodeScriptExecuteLogCacheModel.setWorkspaceId(nodeInfo.getWorkspaceId());
            logServer.insert(nodeScriptExecuteLogCacheModel);
            return nodeScriptExecuteLogCacheModel.getId();
        } finally {
            BaseServerController.removeAll();
        }
    }
}
