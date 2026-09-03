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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.Const;
import io.voyager1.common.commander.CommandOpResult;
import io.voyager1.common.commander.ProjectCommander;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.model.RunMode;
import io.voyager1.model.data.DslYmlDto;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.manage.ProjectInfoService;
import io.voyager1.util.FileSearchUtil;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 插件端,控制台socket
 *
 * @since 2019/4/16
 */
@ServerEndpoint(value = "/console")
@Component
@Slf4j
public class AgentWebSocketConsoleHandle extends BaseAgentWebSocketHandle {

    private static ProjectInfoService projectInfoService;
    private static ProjectCommander projectCommander;

    @Autowired
    public void init(ProjectInfoService projectInfoService,
                     AgentConfig agentConfig,
                     ProjectCommander projectCommander) {
        AgentWebSocketConsoleHandle.projectInfoService = projectInfoService;
        AgentWebSocketConsoleHandle.projectCommander = projectCommander;
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            String projectId = super.getParameters(session, "projectId");
            // 判断项目
            if (!Const.SYSTEM_ID.equals(projectId)) {
                NodeProjectInfoModel nodeProjectInfoModel = this.checkProject(projectId, session);
                if (nodeProjectInfoModel == null) {
                    return;
                }
                //
                SocketSessionUtil.send(session, "连接成功：" + nodeProjectInfoModel.getName());
            }
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
     * 静默消息不做过多处理
     *
     * @param consoleCommandOp 操作
     * @param session          回话
     * @return true
     */
    private boolean silentMsg(ConsoleCommandOp consoleCommandOp, Session session) {
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return true;
        }
//        if (consoleCommandOp == ConsoleCommandOp.top) {
//            TopManager.addMonitor(session);
//            return true;
//        }
        return false;
    }

    private NodeProjectInfoModel checkProject(String projectId, Session session) throws IOException {
        NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(projectId);
        if (nodeProjectInfoModel == null) {
            SocketSessionUtil.send(session, "没有对应项目：" + projectId);
            session.close();
            return null;
        }
        return nodeProjectInfoModel;
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
            JSONObject json = JSONObject.parseObject(message);
            String op = json.getString("op");
            ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
            if (silentMsg(consoleCommandOp, session)) {
                return;
            }
            String projectId = json.getString("projectId");
            NodeProjectInfoModel nodeProjectInfoModel = this.checkProject(projectId, session);
            if (nodeProjectInfoModel == null) {
                return;
            }
            // DSL
            RunMode runMode = nodeProjectInfoModel.getRunMode();
            if (runMode == RunMode.Dsl) {
                // 判断是否可以执行 reload 事件
                DslYmlDto dslYmlDto = nodeProjectInfoModel.mustDslConfig();
                boolean b = dslYmlDto.hasRunProcess(ConsoleCommandOp.reload.name());
                json.put("canReload", b);
            }
            runMsg(consoleCommandOp, session, nodeProjectInfoModel, json);
        } finally {
            clearLanguage();
        }
    }

    private void runMsg(ConsoleCommandOp consoleCommandOp, Session session, NodeProjectInfoModel nodeProjectInfoModel, JSONObject reqJson) throws Exception {
        //

        ApiResult<Object> resultData = null;
        CommandOpResult strResult;
        boolean logUser = false;
        try {
            // 执行相应命令
            switch (consoleCommandOp) {
                case start:
                case restart:
                case stop:
                case reload:
                    logUser = true;
                    strResult = projectCommander.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new ApiResult<>(200, "操作成功", strResult);
                    } else {
                        resultData = new ApiResult<>(400, strResult.msgStr());
                    }
                    break;

                case status: {
                    // 获取项目状态
                    strResult = projectCommander.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new ApiResult<>(200, "运行中", strResult);
                    } else {
                        resultData = new ApiResult<>(404, "未运行", strResult);
                    }
                    break;
                }
                case showlog: {
                    // 进入管理页面后需要实时加载日志
                    String search = reqJson.getString("search");
                    if ((search != null && !search.isEmpty())) {
                        resultData = searchLog(session, nodeProjectInfoModel, reqJson);
                    } else {
                        showLog(session, nodeProjectInfoModel, reqJson);
                    }
                    break;
                }
                default:
                    resultData = new ApiResult<>(404, "不支持的方式：" + consoleCommandOp.name());
                    break;
            }
        } catch (Exception e) {
            log.error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, java.util.Arrays.toString(e.getStackTrace()));
            return;
        } finally {
            if (logUser) {
                // 记录操作人
                NodeProjectInfoModel update = new NodeProjectInfoModel();
                String name = getOptUserName(session);
                update.setModifyUser(name);
                projectInfoService.updateById(update, nodeProjectInfoModel.getId());
            }
        }
        // 返回数据
        if (resultData != null) {
            reqJson.putAll(resultData.toJson());
            reqJson.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            SocketSessionUtil.send(session, reqJson.toString());
        }
    }

    /**
     * {
     * "op": "showlog",
     * "projectId": "python",
     * "search": true,
     * "useProjectId": "python",
     * "useNodeId": "localhost",
     * "beforeCount": 0,
     * "afterCount": 10,
     * "head": 0,
     * "tail": 100,
     * "first": "false",
     * "logFile": "/run.log"
     * }
     *
     * @param session              会话
     * @param nodeProjectInfoModel 项目
     * @param reqJson              请求参数
     * @return 返回信息
     */
    private ApiResult<Object> searchLog(Session session, NodeProjectInfoModel nodeProjectInfoModel, JSONObject reqJson) {
        //
        String fileName = reqJson.getString("logFile");
        File libFile = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        File file = FileUtil.file(libFile, fileName);
        if (!FileUtil.isFile(file)) {
            return new ApiResult<>(404, "文件不存在");
        }
        I18nThreadUtil.execute(() -> {
            try {
                boolean first = ConvertUtil.toBool(reqJson.getString("first"), false);
                int head = reqJson.getIntValue("head");
                int tail = reqJson.getIntValue("tail");
                int beforeCount = reqJson.getIntValue("beforeCount");
                int afterCount = reqJson.getIntValue("afterCount");
                String keyword = reqJson.getString("keyword");
                NodeProjectInfoModel originalModel = projectInfoService.resolveModel(nodeProjectInfoModel);
                Charset charset = projectInfoService.resolveLogCharset(nodeProjectInfoModel, originalModel);
                //BaseFileTailWatcher.detectorCharset(file);
                String resultMsg = FileSearchUtil.searchList(file, charset, keyword, beforeCount, afterCount, head, tail, first, objects -> {
                    try {
                        String line = objects.get(1);
                        SocketSessionUtil.send(session, line);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                SocketSessionUtil.send(session, resultMsg);
            } catch (Exception e) {
                log.error("文件搜索失败", e);
                try {
                    SocketSessionUtil.send(session, "执行命令失败,详情如下：");
                } catch (IOException ignored) {
                }
            }
        });
        return null;
    }

    private void showLog(Session session, NodeProjectInfoModel nodeProjectInfoModel, JSONObject reqJson) throws IOException {
        //        日志文件路径
        String fileName = reqJson.getString("fileName");
        File file;
        if ((fileName == null || fileName.isEmpty())) {
            file = projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        } else {
            File libFile = projectInfoService.resolveLibFile(nodeProjectInfoModel);
            file = FileUtil.file(libFile, fileName);
        }
        try {
            NodeProjectInfoModel originalModel = projectInfoService.resolveModel(nodeProjectInfoModel);
            Charset charset = projectInfoService.resolveLogCharset(nodeProjectInfoModel, originalModel);
            boolean watcher = AgentFileTailWatcher.addWatcher(file, charset, session);
            if (!watcher) {
                SocketSessionUtil.send(session, "监听文件失败,可能文件不存在");
            }
        } catch (Exception io) {
            log.error("监听日志变化", io);
            SocketSessionUtil.send(session, io.getMessage());
        }
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        AgentFileTailWatcher.offline(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
