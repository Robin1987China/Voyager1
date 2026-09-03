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

import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.script.ScriptExecuteLogModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.script.ScriptExecuteLogServer;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.socket.ServerScriptProcessBuilder;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端脚本日志
 *
 * @since 2022/1/19
 */
@Feature(cls = ClassFeature.SCRIPT, method = MethodFeature.EXECUTE)
public class ServerScriptHandler extends BaseProxyHandler {

    private ScriptExecuteLogServer logServer;
    private ScriptServer nodeScriptServer;

    @Override
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        super.init(session, attributes);
        //
        this.logServer = SpringContextHolder.getBean(ScriptExecuteLogServer.class);
        this.nodeScriptServer = SpringContextHolder.getBean(ScriptServer.class);
        ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
        this.sendMsg(session, "连接成功：" + scriptModel.getName());
    }

    public ServerScriptHandler() {
        super(null);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[0];
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return null;
        }
        super.logOpt(this.getClass(), attributes, json);
        switch (consoleCommandOp) {
            case start: {

                String args = json.getString("args");
                String executeId = this.createLog(attributes, scriptModel);
                json.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
                json.put("executeId", executeId);
                ServerScriptProcessBuilder.addWatcher(scriptModel, executeId, args, session);
                ApiResult<String> jsonMessage = new ApiResult<>(200, "开始执行");
                JSONObject jsonObject = jsonMessage.toJson();
                jsonObject.putAll(json);
                this.sendMsg(session, jsonObject.toString());
                break;
            }
            case stop: {
                String executeId = json.getString("executeId");
                if ((executeId == null || executeId.isEmpty())) {
                    SocketSessionUtil.send(session, "没有执行ID");
                    session.close();
                    return null;
                }
                ServerScriptProcessBuilder.stopRun(executeId);
                break;
            }
            default:
                return null;
        }
        return null;
    }

    /**
     * 创建执行日志
     *
     * @param attributes 参数属性
     * @return 执行ID
     */
    private String createLog(Map<String, Object> attributes, ScriptModel scriptModel) {
        UserModel userModel = (UserModel) attributes.get("userInfo");

        //
        try {
            BaseServerController.resetInfo(userModel);
            //
            ScriptModel scriptCacheModel = new ScriptModel();
            scriptCacheModel.setId(scriptModel.getId());
            scriptCacheModel.setLastRunUser(userModel.getId());
            nodeScriptServer.updateById(scriptCacheModel);
            //
            ScriptExecuteLogModel scriptExecuteLogCacheModel = logServer.create(scriptModel, 0);
            return scriptExecuteLogCacheModel.getId();
        } finally {
            BaseServerController.removeAll();
        }
    }


    @Override
    public void destroy(WebSocketSession session) {
        //
        super.destroy(session);
        ServerScriptProcessBuilder.stopWatcher(session);
    }
}
