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

import io.voyager1.util.FileUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.socket.ServiceFileTailWatcher;
import io.voyager1.system.LogbackConfig;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @since 2019/4/24
 */
@Feature(cls = ClassFeature.SYSTEM_LOG, method = MethodFeature.EXECUTE)
@Slf4j
public class SystemLogHandler extends BaseProxyHandler {

    public SystemLogHandler() {
        super(null);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {

        String fileName = json.getString("fileName");
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            // 服务端心跳
            return null;
        }

        super.logOpt(this.getClass(), attributes, json);

        //
        if (consoleCommandOp == ConsoleCommandOp.showlog) {

            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(LogbackConfig.getPath(), fileName);
            //
            File nowFile = (File) attributes.get("nowFile");
            if (nowFile != null && !nowFile.equals(file)) {
                // 离线上一个日志
                ServiceFileTailWatcher.offlineFile(file, session);
            }
            try {
                ServiceFileTailWatcher.addWatcher(file, session);
                attributes.put("nowFile", file);
            } catch (Exception io) {
                log.error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        }
        return null;
    }

    @Override
    public void destroy(WebSocketSession session) {
        super.destroy(session);
        ServiceFileTailWatcher.offline(session);
    }
}
