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

import io.voyager1.util.Tuple;
import io.voyager1.core.AppType;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.Constants;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.model.AgentFileModel;
import io.voyager1.model.UploadFileModel;
import io.voyager1.model.WebSocketMessageModel;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 在线升级
 *
 * @since 2021/8/3
 */
@ServerEndpoint(value = "/node_update")
@Component
@Slf4j
public class AgentWebSocketUpdateHandle extends BaseAgentWebSocketHandle {

    private static final Map<String, UploadFileModel> UPLOAD_FILE_INFO = new HashMap<>();

    private static AgentConfig agentConfig;
    private static MultipartProperties multipartProperties;

    @Autowired
    public void init(AgentConfig agentConfig, MultipartProperties multipartProperties) {
        AgentWebSocketUpdateHandle.agentConfig = agentConfig;
        AgentWebSocketUpdateHandle.multipartProperties = multipartProperties;
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            DataSize maxRequestSize = multipartProperties.getMaxRequestSize();
            int max = Optional.ofNullable(maxRequestSize)
                .map(dataSize -> {
                    // 最大 10MB
                    long value = Math.min(dataSize.toBytes(), DataSize.ofMegabytes(10).toBytes());
                    // 最后转换，不然可能出现 0
                    int valueInt = (int) value;
                    return valueInt > 0 ? valueInt : null;
                })
                .orElseGet(() -> (int) DataSize.ofMegabytes(10).toBytes());

            session.setMaxBinaryMessageBufferSize(max);
            //
        } finally {
            clearLanguage();
        }
    }


    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
            WebSocketMessageModel model = WebSocketMessageModel.getInstance(message);
            switch (model.getCommand()) {
                case "getVersion":
                    model.setData(JSONObject.toJSONString(Voyager1Manifest.getInstance()));
                    break;
                case "upload":
                    AgentFileModel agentFileModel = ((JSONObject) model.getParams()).toJavaObject(AgentFileModel.class);
                    UploadFileModel uploadFileModel = new UploadFileModel();
                    uploadFileModel.setId(model.getNodeId());
                    uploadFileModel.setName(agentFileModel.getName());
                    uploadFileModel.setSize(agentFileModel.getSize());
                    uploadFileModel.setVersion(agentFileModel.getVersion());
                    uploadFileModel.setSavePath(agentConfig.getTempPath().getAbsolutePath());
                    uploadFileModel.remove();
                    UPLOAD_FILE_INFO.put(session.getId(), uploadFileModel);
                    break;
                case "restart":
                    model.setData(restart(session));
                    break;
                case "heart":
                    break;
                default:
                    log.warn("忽略的操作：{}", message);
                    break;
            }
            SocketSessionUtil.send(session, model.toString());
            //session.sendMessage(new TextMessage(model.toString()));
        } finally {
            clearLanguage();
        }
    }

    /**
     * @param message byte 消息
     * @param session 会话
     * @throws Exception 异常
     * @see Constants#DEFAULT_BUFFER_SIZE
     */
    @OnMessage(maxMessageSize = 5 * 1024 * 1024)
    public void onMessage(byte[] message, Session session) throws Exception {
        try {
            setLanguage(session);
            UploadFileModel uploadFileModel = UPLOAD_FILE_INFO.get(session.getId());
            uploadFileModel.save(message);
            // 更新进度
            WebSocketMessageModel model = new WebSocketMessageModel("updateNode", uploadFileModel.getId());
            model.setData(uploadFileModel);
            SocketSessionUtil.send(session, model.toString());
            //		session.sendMessage(new TextMessage(model.toString()));
        } finally {
            clearLanguage();
        }
    }

    /**
     * 重启
     *
     * @param session 回话
     * @return 结果
     */
    public String restart(Session session) {
        String result = Const.UPGRADE_MSG.get();
        try {
            UploadFileModel uploadFile = UPLOAD_FILE_INFO.get(session.getId());
            String filePath = uploadFile.getFilePath();
            ApiResult<Tuple> error = Voyager1Manifest.checkVoyager1Jar(filePath, AppType.Agent);
            if (!error.success()) {
                return error.getMsg();
            }
            Voyager1Manifest.releaseJar(filePath, uploadFile.getVersion());
            Voyager1Application.restart();
        } catch (Exception e) {
            result = "重启失败" + e.getMessage();
            log.error("重启失败", e);
        }
        return result;
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        UPLOAD_FILE_INFO.remove(session.getId());
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
