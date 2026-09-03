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

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.transport.IProxyWebSocket;

import java.io.IOException;
import java.util.Map;

/**
 * 插件端系统日志消息控制器
 *
 * @since 2023/12/26
 */
@Feature(cls = ClassFeature.AGENT_LOG, method = MethodFeature.EXECUTE)
@Slf4j
public class AgentLogHandler extends BaseProxyHandler {

    public AgentLogHandler() {
        super(NodeUrl.Socket_SystemLog);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, IProxyWebSocket proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        proxySession.send(json.toString());
        return null;
    }
}
