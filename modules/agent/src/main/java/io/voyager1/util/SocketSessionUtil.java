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

import io.voyager1.util.ThreadUtil;
import io.voyager1.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import jakarta.websocket.Session;
import java.io.IOException;

/**
 * socket 会话对象
 *
 * @since 2018/9/29
 */
@Slf4j
public class SocketSessionUtil {
    /**
     * 锁
     */
    private static final KeyLock<String> LOCK = new KeyLock<>();
    /**
     * 错误尝试次数
     */
    private static final int ERROR_TRY_COUNT = 10;

    /**
     * 发送消息
     *
     * @param session 会话对象
     * @param msg     消息
     * @throws IOException 异常
     */
    public static void send(final Session session, String msg) throws IOException {
        if ((msg == null || msg.isEmpty())) {
            return;
        }
        Assert.state(session.isOpen(), "session close ");
        try {
            LOCK.lock(session.getId());
            IOException exception = null;
            int tryCount = 0;
            do {
                tryCount++;
                if (exception != null) {
                    // 上一次有异常、休眠 500
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                try {
                    session.getBasicRemote().sendText(msg);
                    exception = null;
                    break;
                } catch (IOException e) {
                    log.error("{}{}", "发送消息失败:", tryCount, e);
                    exception = e;
                }
            } while (tryCount <= ERROR_TRY_COUNT);
            if (exception != null) {
                throw exception;
            }
        } finally {
            LOCK.unlock(session.getId());
        }
    }
}
