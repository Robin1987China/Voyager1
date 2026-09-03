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

import io.voyager1.util.IoUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.transport.IProxyWebSocket;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @since 2024/4/26
 */
@Feature(cls = ClassFeature.FREE_SCRIPT, method = MethodFeature.EXECUTE)
@Slf4j
public class FreeScriptHandler extends BaseProxyHandler {

    public FreeScriptHandler() {
        super(NodeUrl.FreeScriptRun);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, IProxyWebSocket proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {

        String content = json.getString("content");
        if ((content == null || content.isEmpty())) {
            SocketSessionUtil.send(session, "没有需要执行的内容");
            session.close();
            return null;
        }

        MachineNodeModel machine = (MachineNodeModel) attributes.get("machine");
        String osName = machine.getOsName();
        String template = "";
        boolean appendTemplate = json.getBooleanValue("appendTemplate");
        if (appendTemplate && (osName != null && !osName.isEmpty())) {
            InputStream templateInputStream;
            if (osName.startsWith("Windows")) {
                templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX_WINDOWS);
            } else {
                templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX_UNIX);
            }
            template = IoUtil.readUtf8(templateInputStream);
        }

        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        json.put("tag", uuid);
        json.put("content", template + content);
        String path = json.getString("path");
        json.put("path", (path == null || path.isEmpty() ? "./" : path));
        json.put("environment", new JSONObject());
        attributes.put("uuidTag", uuid);
        proxySession.send(json.toString());
        return null;
    }

    @Override
    protected void onProxyMessage(WebSocketSession session, String msg) {
        if (java.util.Objects.equals(msg, "VOYAGER1_SYSTEM_TAG:" + session.getAttributes().get("uuidTag"))) {
            // 执行结束
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭客户端回话异常", e);
            }
            return;
        }
        super.onProxyMessage(session, msg);
    }
}
