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

import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.FileUtil;

import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.configuration.SystemConfig;
import io.voyager1.system.LogbackConfig;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 系统日志
 *
 * @since 2019/4/16
 */
@ServerEndpoint(value = "/system_log")
@Component
@Slf4j
public class AgentWebSocketSystemLogHandle extends BaseAgentWebSocketHandle {

    private static SystemConfig systemConfig;

    private static final Map<String, File> CACHE_FILE = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public void init(AgentConfig agentConfig) {
        AgentWebSocketSystemLogHandle.systemConfig = agentConfig.getSystem();
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            SocketSessionUtil.send(session, "连接成功：插件端日志");
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
            String op = json.getString("op");
            ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
            if (consoleCommandOp == ConsoleCommandOp.heart) {
                return;
            }
            runMsg(session, json);
        } finally {
            clearLanguage();
        }
    }

    private void runMsg(Session session, JSONObject reqJson) throws Exception {
        try {
            String fileName = reqJson.getString("fileName");
            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(LogbackConfig.getPath(), fileName);
            File file1 = CACHE_FILE.get(session.getId());
            if (file1 != null && !file1.equals(file)) {
                // 离线上一个日志
                AgentFileTailWatcher.offlineFile(file, session);
            }
            try {
                AgentFileTailWatcher.addWatcher(file, systemConfig.getLogCharset(), session);
                CACHE_FILE.put(session.getId(), file);
            } catch (Exception io) {
                log.error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        } catch (Exception e) {
            log.error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, java.util.Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
