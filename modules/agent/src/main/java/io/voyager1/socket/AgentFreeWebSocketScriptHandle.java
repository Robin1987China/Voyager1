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

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.LineHandler;

import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.Constants;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.FileUtils;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自由脚本socket
 *
 * @since 2023/03/28
 */
@ServerEndpoint(value = "/free-script-run")
@Component
@Slf4j
public class AgentFreeWebSocketScriptHandle extends BaseAgentWebSocketHandle {

    private final static Map<String, ScriptProcess> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public void init(AgentConfig agentConfig) {
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            SocketSessionUtil.send(session, "连接成功");
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

    /**
     * @param message 消息
     * @param session 会话
     * @throws Exception 异常
     * @see Constants#DEFAULT_BUFFER_SIZE
     */
    @OnMessage(maxMessageSize = 5 * 1024 * 1024)
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
            if (CACHE.containsKey(session.getId())) {
                SocketSessionUtil.send(session, ApiResult.getString(500, "不要重复打开"));
                return;
            }
            JSONObject json = JSONObject.parseObject(message);
            String type = json.getString("type");
            if (java.util.Objects.equals(type, "close")) {
                // 关闭、停止脚本执行
                IoUtil.close(CACHE.remove(session.getId()));
                session.close();
                return;
            }
            String path = json.getString("path");
            String tag = json.getString("tag");
            JSONObject environment = json.getJSONObject("environment");
            String content = json.getString("content");
            if (((path == null || path.isEmpty()) || (tag == null || tag.isEmpty()) || (content == null || content.isEmpty()))) {
                SocketSessionUtil.send(session, ApiResult.getString(500, "参数存在不正确"));
                return;
            }
            if (environment == null) {
                SocketSessionUtil.send(session, ApiResult.getString(500, "没有环境变量"));
                return;
            }
            Map<String, EnvironmentMapBuilder.Item> map = environment.to(new TypeReference<Map<String, EnvironmentMapBuilder.Item>>() {
            });
            ScriptProcess scriptProcess = new ScriptProcess(content, map, path, tag);
            CACHE.put(session.getId(), scriptProcess);
            scriptProcess.run(line -> {
                try {
                    SocketSessionUtil.send(session, line);
                } catch (IOException e) {
                    log.error("发送消息失败", e);
                }
            });
        } finally {
            clearLanguage();
        }
    }


    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        IoUtil.close(CACHE.remove(session.getId()));
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
        IoUtil.close(CACHE.remove(session.getId()));
    }

    public static class ScriptProcess implements AutoCloseable {
        private final String content;
        private final EnvironmentMapBuilder environment;
        private final String path;
        private final String tag;

        private Process process;
        private InputStream inputStream;
        private File scriptFile;

        public ScriptProcess(String content, Map<String, EnvironmentMapBuilder.Item> environment, String path, String tag) {
            this.content = content;
            this.environment = EnvironmentMapBuilder.builder(environment);
            this.path = path;
            this.tag = tag;
        }

        /**
         * 开始执行脚本
         *
         * @param lineHandler 回调
         * @throws IOException          io 异常
         * @throws InterruptedException 中断
         */
        public void run(LineHandler lineHandler) throws IOException, InterruptedException {
            String dataPath = Voyager1Application.getInstance().getDataPath();
            this.scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, String.format("%s.%s", java.util.UUID.randomUUID().toString().replace("-", ""), CommandUtil.SUFFIX));
            FileUtils.writeScript(this.content, scriptFile, ExtConfigBean.getConsoleLogCharset());
            //
            String script = FileUtil.getAbsolutePath(scriptFile);
            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(0, script);
            CommandUtil.paddingPrefix(command);
            log.debug(String.join(" ", command));
            // 添加环境变量
            this.environment.eachStr(lineHandler::handle);
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(this.environment.environment());
            processBuilder.redirectErrorStream(true);
            processBuilder.command(command);
            //
            if ((path != null && !path.isEmpty())) {
                File directory = FileUtil.file(path).getAbsoluteFile();
                // 需要创建目录
                FileUtil.mkdir(directory);
                processBuilder.directory(directory);
            }
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, lineHandler);
            int waitFor = process.waitFor();
            lineHandler.handle(String.format("执行结束:%s", waitFor));
            // 客户端可以关闭会话啦
            lineHandler.handle("VOYAGER1_SYSTEM_TAG:" + tag);
        }

        @Override
        public void close() throws Exception {
            IoUtil.close(inputStream);
            CommandUtil.kill(process);
            try {
                FileUtil.del(this.scriptFile);
            } catch (Exception ignored) {
            }
        }
    }
}
