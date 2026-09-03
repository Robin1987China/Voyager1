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

package io.voyager1.socket;

import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.model.data.NodeScriptModel;
import io.voyager1.script.NodeScriptProcessBuilder;
import io.voyager1.service.script.NodeScriptServer;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * 脚本模板socket
 *
 * @since 2019/4/24
 */
@ServerEndpoint(value = "/script_run")
@Component
@Slf4j
public class AgentWebSocketScriptHandle extends BaseAgentWebSocketHandle {

    private static NodeScriptServer nodeScriptServer;

    @Autowired
    public void init(NodeScriptServer nodeScriptServer, AgentConfig agentConfig) {
        AgentWebSocketScriptHandle.nodeScriptServer = nodeScriptServer;
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            String id = this.getParameters(session, "id");
            String workspaceId = this.getParameters(session, "workspaceId");
            if (((id == null || id.isEmpty()) || (workspaceId == null || workspaceId.isEmpty()))) {
                SocketSessionUtil.send(session, "脚本模板或者工作空间未知");
                return;
            }

            NodeScriptModel nodeScriptModel = nodeScriptServer.getItem(id);
            if (nodeScriptModel == null) {
                SocketSessionUtil.send(session, "没有找到对应的脚本模板");
                return;
            }
            SocketSessionUtil.send(session, "连接成功：" + nodeScriptModel.getName());
        } catch (Exception e) {
            log.error("socket 错误", e);
            try {
                SocketSessionUtil.send(session, ApiResult.getString(500, "系统错误!"));
                session.close();
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        } finally {
            clearLanguage();
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
            JSONObject json = JSONObject.parseObject(message);
            String scriptId = json.getString("scriptId");
            NodeScriptModel nodeScriptModel = nodeScriptServer.getItem(scriptId);
            if (nodeScriptModel == null) {
                SocketSessionUtil.send(session, "没有对应脚本模板:" + scriptId);
                session.close();
                return;
            }
            String op = json.getString("op");
            ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
            switch (consoleCommandOp) {
                case start: {
                    String args = json.getString("args");
                    String executeId = json.getString("executeId");
                    if ((executeId == null || executeId.isEmpty())) {
                        SocketSessionUtil.send(session, "没有执行ID");
                        session.close();
                        return;
                    }
                    NodeScriptProcessBuilder.addWatcher(nodeScriptModel, executeId, args, session);
                    break;
                }
                case stop: {
                    String executeId = json.getString("executeId");
                    if ((executeId == null || executeId.isEmpty())) {
                        SocketSessionUtil.send(session, "没有执行ID");
                        session.close();
                        return;
                    }
                    NodeScriptProcessBuilder.stopRun(executeId);
                    break;
                }
                case heart:
                default:
                    return;
            }
            // 记录操作人
            nodeScriptModel = nodeScriptServer.getItem(scriptId);
            String name = getOptUserName(session);
            nodeScriptModel.setLastRunUser(name);
            nodeScriptServer.updateItem(nodeScriptModel);
            json.put("code", 200);
            String value = "执行成功";
            json.put("msg", value);
            log.debug(json.toString());
            SocketSessionUtil.send(session, json.toString());
        } finally {
            clearLanguage();
        }
    }


    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        NodeScriptProcessBuilder.stopWatcher(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
