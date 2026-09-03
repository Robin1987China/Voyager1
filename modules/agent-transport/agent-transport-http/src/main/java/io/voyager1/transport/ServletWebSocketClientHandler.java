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

package io.voyager1.transport;

import io.voyager1.util.ThreadUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.SystemPropsUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.transport.i18n.TransportI18nMessageUtil;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @since 2022/12/26
 */
@Slf4j
public class ServletWebSocketClientHandler extends AbstractWebSocketHandler implements IProxyWebSocket {

    private static final StandardWebSocketClient CLIENT = new StandardWebSocketClient();

    private WebSocketSession session;
    private final Integer timeout;
    private final String uriTemplate;
    private Consumer<String> consumerText;
    private WebSocketConnectionManager manager;
    private CloseStatus closeStatus;

    public ServletWebSocketClientHandler(String uriTemplate, Integer timeout) {
        this.uriTemplate = uriTemplate;
        this.timeout = timeout;
    }

    @Override
    public void onMessage(Consumer<String> consumer) {
        this.consumerText = consumer;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Optional.ofNullable(this.consumerText).ifPresent(consumer -> consumer.accept(message.getPayload()));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 发送消息时间限制 60 秒
        long messageSizeLimit = SystemPropsUtil.getLong("VOYAGER1_NODE_WEB_SOCKET_MESSAGE_SIZE_LIMIT", DataSize.ofMegabytes(5).toBytes());
        this.session = new ConcurrentWebSocketSessionDecorator(session, 60 * 1000, (int) messageSizeLimit);
        // 消息大小限制
        this.session.setTextMessageSizeLimit((int) messageSizeLimit);
        this.session.setBinaryMessageSizeLimit((int) messageSizeLimit);
    }

    @Override
    public void close() throws IOException {
        if (this.manager == null) {
            return;
        }
        this.manager.stop();
        this.manager = null;
        this.session = null;
    }

    @Override
    public boolean connect() {
        Assert.isNull(this.manager, "The connection has been established, do not repeat the connection");
        this.manager = new WebSocketConnectionManager(CLIENT, this, this.uriTemplate);
        this.manager.start();
        // 时间不能太短，需要大于 1 秒
        return this.blocking(5);
    }

    @Override
    public boolean connectBlocking() {
        int maxTimeout = Optional.ofNullable(this.timeout).orElse(60);
        return this.connectBlocking(maxTimeout);
    }

    @Override
    public boolean connectBlocking(int seconds) {
        if (this.connect()) {
            return true;
        }
        return this.blocking(seconds);
    }

    private boolean blocking(int seconds) {
        int waitTime = 0;
        do {
            if (this.isConnected()) {
                return true;
            }
            waitTime++;
            try {
                Thread.sleep(TimeUnit.MILLISECONDS.toMillis(500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (waitTime * 2 <= seconds);
        return false;
    }

    @Override
    public void send(String msg) throws IOException {
        Assert.notNull(this.session, "还没有连接上");
        session.sendMessage(new TextMessage(msg));
    }

    @Override
    public void send(ByteBuffer bytes) throws IOException {
        Assert.notNull(this.session, "还没有连接上");
        session.sendMessage(new BinaryMessage(bytes));
    }

    @Override
    public boolean isConnected() {
        if (this.manager == null) {
            return false;
        }
        return this.manager.isConnected();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("websocket出现错误：{}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        this.closeStatus = status;
        log.warn("连接关闭 {} {}", status.getCode(), status.getReason());
    }

    @Override
    public String getCloseStatusMsg() {
        return Optional.ofNullable(this.closeStatus)
            .map(closeStatus -> String.format("%s:%s", closeStatus.getCode(), closeStatus.getReason()))
            .orElse("");
    }
}
