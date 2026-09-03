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
import io.voyager1.util.ExceptionUtil;

import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentAuthorize;
import io.voyager1.util.SocketSessionUtil;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件端socket 基类
 *
 * @since 2019/4/24
 */
@Slf4j
public abstract class BaseAgentWebSocketHandle {

    private static final ConcurrentHashMap<String, String> USER = new java.util.concurrent.ConcurrentHashMap<>();
    protected static AgentAuthorize agentAuthorize;

    /**
     * 设置授权对象
     *
     * @param agentAuthorize 授权
     */
    protected static void setAgentAuthorize(AgentAuthorize agentAuthorize) {
        BaseAgentWebSocketHandle.agentAuthorize = agentAuthorize;
    }

    protected void setLanguage(Session session) {
        Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();
        List<String> lang = requestParameterMap.get("lang");
        I18nMessageUtil.setLanguage((lang == null || lang.isEmpty() ? null : lang.get(0)));
    }

    protected void clearLanguage() {
        I18nMessageUtil.clearLanguage();
    }

    protected String getParameters(Session session, String name) {
        Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();
        Map<String, String> parameters = session.getPathParameters();
        if (log.isDebugEnabled()) {
            log.debug("web socket parameters: {} {}", JSONObject.toJSONString(requestParameterMap), parameters);
        }
        List<String> strings = requestParameterMap.get(name);
        String value = String.join(",", strings);
        if ((value == null || value.isEmpty())) {
            value = parameters.get(name);
        }
        return URLUtil.decode(value);
    }

    /**
     * 判断授权信息是否正确
     *
     * @param session session
     * @return true 需要结束回话
     */
    public boolean checkAuthorize(Session session) {
        String authorize = this.getParameters(session, Const.VOYAGER1_AGENT_AUTHORIZE);
        // 新签名令牌优先，旧 sha1 回退（过渡期）
        boolean ok = agentAuthorize.checkSignedToken(authorize) || agentAuthorize.checkAuthorize(authorize);
        if (!ok) {
            log.warn("socket 会话建立失败,授权信息错误");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "授权信息错误"));
            } catch (Exception e) {
                log.error("socket 错误", e);
            }
            return true;
        }
        this.addUser(session, this.getParameters(session, "optUser"));
        return false;
    }

    /**
     * 添加用户监听的
     *
     * @param session session
     * @param name    用户名
     */
    private void addUser(Session session, String name) {
        String optUser = URLUtil.decode(name);
        if (optUser == null) {
            return;
        }
        USER.put(session.getId(), optUser);
    }

    public void onError(Session session, Throwable thr) {
        // java.io.IOException: Broken pipe
        try {
            SocketSessionUtil.send(session, "服务端发生异常" + java.util.Arrays.toString(thr.getStackTrace()));
        } catch (IOException ignored) {
        }
        log.error("{}{}", session.getId(), "socket 异常", thr);
    }

    protected String getOptUserName(Session session) {
        String name = USER.get(session.getId());
        return (name == null || name.isEmpty() ? "-" : name);
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug("会话[{}]关闭原因：{}", session.getId(), closeReason);
        // 清理日志监听
        try {
            AgentFileTailWatcher.offline(session);
        } catch (Exception e) {
            log.error("关闭异常", e);
        }
        // top
        //        TopManager.removeMonitor(session);
        USER.remove(session.getId());
    }
}
