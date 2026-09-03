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

package io.voyager1.util;


import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;

/**
 * socket 会话对象
 *
 * @since 2018/9/29
 */
@Slf4j
public class SocketSessionUtil {

 private static final Map<String, WebSocketSession> SOCKET_MAP = new java.util.concurrent.ConcurrentHashMap<>();

 /**
 * 发送文本消息
 *
 * @param session 会话
 * @param msg 消息
 * @return 是否发送成功
 * @throws IOException io
 */
 public static boolean send(WebSocketSession session, String msg) throws IOException {
 return send(session, new TextMessage(msg));
 }

 /**
 * 发送消息
 *
 * @param session 会话
 * @param message 消息
 * @return 是否发送成功
 * @throws IOException io
 */
 public static boolean send(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
 if (!session.isOpen()) {
 // 会话关闭不能发送消息 
 log.warn("会话已经关闭啦，不能发送消息：{}", message.getPayload());
 return false;
 }
 WebSocketSession webSocketSession = SOCKET_MAP.computeIfAbsent(session.getId(), s -> new ConcurrentWebSocketSessionDecorator(session, 60 * 1000, (int) DataSize.ofMegabytes(5).toBytes()));
 webSocketSession.sendMessage(message);
 return true;
 }

 public static void close(WebSocketSession session) {
 SOCKET_MAP.remove(session.getId());
 }
}
