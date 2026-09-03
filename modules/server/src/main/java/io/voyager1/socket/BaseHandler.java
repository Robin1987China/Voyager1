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

import io.voyager1.util.BeanPath;
import io.voyager1.util.ThreadUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.BaseNodeModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.system.init.OperateLogController;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @since 2019/8/9
 */
@Slf4j
public abstract class BaseHandler extends TextWebSocketHandler {

    protected void setLanguage(WebSocketSession session) {
        if (session == null) {
            return;
        }
        Map<String, Object> attributes = session.getAttributes();
        String lang = (String) attributes.get("lang");
        I18nMessageUtil.setLanguage(lang);
    }

    protected void clearLanguage() {
        I18nMessageUtil.clearLanguage();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        setLanguage(session);
        try {
            Map<String, Object> attributes = session.getAttributes();
            //
            this.showHelloMsg(attributes, session);
            //
            String permissionMsg = (String) attributes.get("permissionMsg");
            if ((permissionMsg != null && !permissionMsg.isEmpty())) {
                this.sendMsg(session, permissionMsg);
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                this.destroy(session);
                return;
            }
            this.afterConnectionEstablishedImpl(session);
        } finally {
            clearLanguage();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            setLanguage(session);
            super.handleMessage(session, message);
        } finally {
            clearLanguage();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            setLanguage(session);
            super.handleTextMessage(session, message);
        } finally {
            clearLanguage();
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            setLanguage(session);
            super.handleBinaryMessage(session, message);
        } finally {
            clearLanguage();
        }
    }

    protected void showHelloMsg(Map<String, Object> attributes, WebSocketSession session) {
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        if (userInfo != null) {
            String payload = String.format("欢迎加入:%s 会话id:%s ", userInfo.getName(), session.getId() + "\n");
            this.sendMsg(session, payload);
        }
    }

    /**
     * 建立会话后
     *
     * @param session 会话
     * @throws Exception 异常
     */
    protected void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        // 连接成功后记录
        this.logOpt(this.getClass(), attributes, attributes);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("{}{}", session.getId(), "socket 异常", exception);
        destroy(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        destroy(session);
        log.debug("会话[{}]关闭原因：{}", session.getId(), status);
    }

    /**
     * 关闭连接
     *
     * @param session session
     */
    public abstract void destroy(WebSocketSession session);

    protected void sendMsg(WebSocketSession session, String msg) {
        try {
            SocketSessionUtil.send(session, msg);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 操作 websocket 日志
     *
     * @param cls        class
     * @param attributes 属性
     * @param reqData    请求数据
     */
    protected void logOpt(Class<?> cls, Map<String, Object> attributes, Object reqData) {
        String ip = (String) attributes.get("ip");
        NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
        // 记录操作日志
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        String workspaceId = (String) attributes.get("workspaceId");
        OperateLogController.CacheInfo cacheInfo = new OperateLogController.CacheInfo();
        cacheInfo.setIp(ip);
        Feature feature = cls.getAnnotation(Feature.class);
        MethodFeature method = feature.method();
//		Assert.state(feature != null && feature, "权限功能没有配置正确");
        cacheInfo.setClassFeature(feature.cls());
        cacheInfo.setWorkspaceId(workspaceId);
        cacheInfo.setMethodFeature(method);

        cacheInfo.setNodeModel(nodeModel);
        //
        Object dataItem = attributes.get("dataItem");
        Optional.ofNullable(dataItem).map(o -> {
            if (o instanceof BaseNodeModel) {
                BaseNodeModel baseNodeModel = (BaseNodeModel) o;
                return baseNodeModel.dataId();
            }
            Object id = BeanPath.create("id").get(o);
            return (id == null ? null : id.toString());

        }).ifPresent(cacheInfo::setDataId);
        String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
        cacheInfo.setUserAgent(userAgent);
        cacheInfo.setReqData(JSONObject.toJSONString(reqData));

        //cacheInfo.setMethodFeature(execute);
        Object proxySession = attributes.get("proxySession");
        try {
            attributes.remove("proxySession");
            attributes.put("use_type", "WebSocket");
            attributes.put("class_type", cls.getName());
            OperateLogController operateLogController = SpringContextHolder.getBean(OperateLogController.class);
            operateLogController.log(userInfo, JSONObject.toJSONString(attributes), cacheInfo);
        } catch (Exception e) {
            log.error("记录操作日志异常", e);
        } finally {
            if (proxySession != null) {
                attributes.put("proxySession", proxySession);
            }
        }
    }

}
