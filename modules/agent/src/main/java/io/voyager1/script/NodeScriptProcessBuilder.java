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

package io.voyager1.script;

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
import io.voyager1.model.data.NodeScriptModel;
import io.voyager1.service.script.NodeScriptServer;
import io.voyager1.service.system.AgentWorkspaceEnvVarService;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.SocketSessionUtil;

import jakarta.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

/**
 * 脚本执行
 *
 * @since 2019/4/25
 */
@Slf4j
public class NodeScriptProcessBuilder extends BaseRunScript implements Runnable {
    /**
     * 执行中的缓存
     */
    private static final ConcurrentHashMap<String, NodeScriptProcessBuilder> FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    private final ProcessBuilder processBuilder;
    private final Set<Session> sessions = new HashSet<>();
    private final String executeId;
    private final File scriptFile;
    private final EnvironmentMapBuilder environmentMapBuilder;
    private NodeScriptServer nodeScriptServer;

    private NodeScriptProcessBuilder(NodeScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        super(nodeScriptModel.logFile(executeId), StandardCharsets.UTF_8);
        this.executeId = executeId;
        if (nodeScriptServer == null) {
            nodeScriptServer = SpringContextHolder.getBean(NodeScriptServer.class);
        }
        //
        scriptFile = nodeScriptServer.toExecuteFile(nodeScriptModel);
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        processBuilder = new ProcessBuilder();
        List<String> command = CommandParam.toCommandList(args);
        command.add(0, script);
        CommandUtil.paddingPrefix(command);
        log.debug(String.join(" ", command));
        String workspaceId = nodeScriptModel.getWorkspaceId();
        // 添加环境变量
        Map<String, String> environment = processBuilder.environment();
        AgentWorkspaceEnvVarService workspaceService = SpringContextHolder.getBean(AgentWorkspaceEnvVarService.class);
        environmentMapBuilder = workspaceService.getEnv(workspaceId);
        environmentMapBuilder.putStr(paramMap);
        environment.putAll(environmentMapBuilder.environment());
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        processBuilder.directory(scriptFile.getParentFile());
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     * @param paramMap        执行环境变量参数
     */
    public static NodeScriptProcessBuilder create(NodeScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        return FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.computeIfAbsent(executeId, file1 -> {
            NodeScriptProcessBuilder nodeScriptProcessBuilder1 = new NodeScriptProcessBuilder(nodeScriptModel, executeId, args, paramMap);
            I18nThreadUtil.execute(nodeScriptProcessBuilder1);
            return nodeScriptProcessBuilder1;
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
    public static void addWatcher(NodeScriptModel nodeScriptModel, String executeId, String args, Session session) {
        NodeScriptProcessBuilder nodeScriptProcessBuilder = create(nodeScriptModel, executeId, args, null);
        //
        if (nodeScriptProcessBuilder.sessions.add(session)) {
            if (FileUtil.exist(nodeScriptProcessBuilder.logFile)) {
                // 读取之前的信息并发送
                FileUtil.readLines(nodeScriptProcessBuilder.logFile, StandardCharsets.UTF_8, (LineHandler) line -> {
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
    public static void stopWatcher(Session session) {
        Collection<NodeScriptProcessBuilder> nodeScriptProcessBuilders = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.values();
        for (NodeScriptProcessBuilder nodeScriptProcessBuilder : nodeScriptProcessBuilders) {
            Set<Session> sessions = nodeScriptProcessBuilder.sessions;
            sessions.removeIf(session1 -> session1.getId().equals(session.getId()));
        }
    }

    /**
     * 停止脚本命令
     *
     * @param executeId 执行ID
     */
    public static void stopRun(String executeId) {
        NodeScriptProcessBuilder nodeScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.get(executeId);
        if (nodeScriptProcessBuilder != null) {
            nodeScriptProcessBuilder.end("停止运行");
        }
    }

    @Override
    public void run() {
        //初始化ProcessBuilder对象
        try {
            environmentMapBuilder.eachStr(this::info);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) NodeScriptProcessBuilder.this::info);
            int waitFor = process.waitFor();
            this.system("执行结束:{}", waitFor);
            ApiResult<String> jsonMessage = new ApiResult<>(200, "执行完毕:" + waitFor);
            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            jsonObject.put("op", ConsoleCommandOp.stop.name());
            this.end(jsonObject.toString());
        } catch (Exception e) {
            log.error("执行异常", e);
            this.systemError("执行异常", e.getMessage());
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
        Iterator<Session> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, msg);
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
            iterator.remove();
        }
        NodeScriptProcessBuilder nodeScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.remove(this.executeId);
        IoUtil.close(nodeScriptProcessBuilder);
    }

    @Override
    public void close() {
        super.close();
        try {
            FileUtil.del(this.scriptFile);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void msgCallback(String info) {
        //
        Iterator<Session> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, info);
            } catch (IOException e) {
                log.error("发送消息失败", e);
                iterator.remove();
            }
        }
    }
}
