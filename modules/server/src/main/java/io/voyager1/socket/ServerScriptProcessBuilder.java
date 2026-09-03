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

import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.CommandExecLogModel;
import io.voyager1.model.script.ScriptModel;
import io.voyager1.script.BaseRunScript;
import io.voyager1.script.CommandParam;
import io.voyager1.service.script.ScriptExecuteLogServer;
import io.voyager1.service.system.WorkspaceEnvVarService;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

/**
 * 脚本执行
 *
 * @since 2022/1/19
 */
@Slf4j
public class ServerScriptProcessBuilder extends BaseRunScript implements Runnable {
    /**
     * 执行中的缓存
     */
    private static final ConcurrentHashMap<String, ServerScriptProcessBuilder> FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    private final ProcessBuilder processBuilder;
    private final Set<WebSocketSession> sessions = new HashSet<>();
    private final String executeId;
    private final File scriptFile;

    private final EnvironmentMapBuilder environmentMapBuilder;
    private ScriptExecuteLogServer scriptExecuteLogServer;

    private ServerScriptProcessBuilder(ScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        super(nodeScriptModel.logFile(executeId), StandardCharsets.UTF_8);
        //
        if (scriptExecuteLogServer == null) {
            scriptExecuteLogServer = SpringContextHolder.getBean(ScriptExecuteLogServer.class);
        }
        this.executeId = executeId;
        //
        WorkspaceEnvVarService workspaceEnvVarService = SpringContextHolder.getBean(WorkspaceEnvVarService.class);
        environmentMapBuilder = workspaceEnvVarService.getEnv(nodeScriptModel.getWorkspaceId());
        environmentMapBuilder.putStr(paramMap);
        scriptFile = scriptExecuteLogServer.toExecLogFile(nodeScriptModel);
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        processBuilder = new ProcessBuilder();
        List<String> command = CommandParam.toCommandList(args);
        command.add(0, script);
        CommandUtil.paddingPrefix(command);
        log.debug(String.join(" ", command));
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        Map<String, String> environment = processBuilder.environment();
        environment.putAll(environmentMapBuilder.environment());
        processBuilder.directory(scriptFile.getParentFile());
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     */
    public static ServerScriptProcessBuilder create(ScriptModel nodeScriptModel, String executeId, String args) {
        return create(nodeScriptModel, executeId, args, null);
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     * @param paramMap        环境变量参数
     */
    public static ServerScriptProcessBuilder create(ScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        return FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.computeIfAbsent(executeId, file1 -> {
            ServerScriptProcessBuilder serverScriptProcessBuilder1 = new ServerScriptProcessBuilder(nodeScriptModel, executeId, args, paramMap);
            I18nThreadUtil.execute(serverScriptProcessBuilder1);
            return serverScriptProcessBuilder1;
        });
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     * @param session         会话
     */
    public static void addWatcher(ScriptModel nodeScriptModel, String executeId, String args, WebSocketSession session) {
        ServerScriptProcessBuilder serverScriptProcessBuilder = create(nodeScriptModel, executeId, args);
        //
        if (serverScriptProcessBuilder.sessions.add(session)) {
            if (FileUtil.exist(serverScriptProcessBuilder.logFile)) {
                // 读取之前的信息并发送
                FileUtil.readLines(serverScriptProcessBuilder.logFile, StandardCharsets.UTF_8, (LineHandler) line -> {
                    try {
                        SocketSessionUtil.send(session, line);
                    } catch (IOException e) {
                        log.error("发送消息失败", e);
                    }
                });
            }
        }
    }

    /**
     * 判断是否还在执行中
     *
     * @param executeId 执行id
     * @return true 还在执行
     */
    public static boolean isRun(String executeId) {
        return FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.containsKey(executeId);
    }

    /**
     * 关闭会话
     *
     * @param session 会话
     */
    public static void stopWatcher(WebSocketSession session) {
        Collection<ServerScriptProcessBuilder> serverScriptProcessBuilders = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.values();
        for (ServerScriptProcessBuilder serverScriptProcessBuilder : serverScriptProcessBuilders) {
            Set<WebSocketSession> sessions = serverScriptProcessBuilder.sessions;
            sessions.removeIf(session1 -> session1.getId().equals(session.getId()));
        }
    }

    /**
     * 停止脚本命令
     *
     * @param executeId 执行ID
     */
    public static void stopRun(String executeId) {
        ServerScriptProcessBuilder serverScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.get(executeId);
        if (serverScriptProcessBuilder != null) {
            serverScriptProcessBuilder.end("停止运行");
        }
    }

    @Override
    public void run() {
        //初始化ProcessBuilder对象
        try {
            scriptExecuteLogServer.updateStatus(executeId, CommandExecLogModel.Status.ING);
            this.environmentMapBuilder.eachStr(this::info);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) ServerScriptProcessBuilder.this::info);
            int waitFor = process.waitFor();
            this.system("执行结束:{}", waitFor);
            scriptExecuteLogServer.updateStatus(executeId, CommandExecLogModel.Status.DONE, waitFor);
            //
            ApiResult<String> jsonMessage = new ApiResult<>(200, "执行完毕:" + waitFor);
            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            jsonObject.put("op", ConsoleCommandOp.stop.name());
            this.end(jsonObject.toString());
        } catch (Exception e) {
            log.error("执行异常", e);
            scriptExecuteLogServer.updateStatus(executeId, CommandExecLogModel.Status.ERROR);
            this.system("执行异常", e.getMessage());
            this.end("执行异常：" + e.getMessage());
        } finally {
            this.close();
        }
    }

    /**
     * 结束执行
     *
     * @param msg 响应的消息
     */
    @Override
    protected void end(String msg) {
        Iterator<WebSocketSession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            WebSocketSession session = iterator.next();
            try {
                SocketSessionUtil.send(session, msg);
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
            iterator.remove();
        }
        ServerScriptProcessBuilder serverScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.remove(this.executeId);
        IoUtil.close(serverScriptProcessBuilder);
    }

    @Override
    protected void msgCallback(String info) {
        //
        Iterator<WebSocketSession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            WebSocketSession session = iterator.next();
            try {
                SocketSessionUtil.send(session, info);
            } catch (IOException e) {
                log.error("发送消息失败", e);
                iterator.remove();
            }
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            FileUtil.del(this.scriptFile);
        } catch (Exception ignored) {
        }
    }
}
