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

package io.voyager1.socket.handler;

import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.socket.BaseHandler;
import io.voyager1.util.SocketSessionUtil;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @since 2022/2/10
 */
@Slf4j
public abstract class BaseTerminalHandler extends BaseHandler {

    protected void sendBinary(WebSocketSession session, String msg) {
        if (msg == null) {
            return;
        }
        BinaryMessage byteBuffer = new BinaryMessage(msg.getBytes());
        try {
            SocketSessionUtil.send(session, byteBuffer);
        } catch (IOException e) {
            log.error("发送消息失败:" + msg, e);
        }
    }
}
