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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @since 2022/12/26
 */
public interface IProxyWebSocket extends AutoCloseable {

    /**
     * 关闭连接
     *
     * @throws IOException 关闭异常
     */
    void close() throws IOException;

    /**
     * 打开连接,默认停留一秒
     *
     * @return 打开状态
     */
    boolean connect();

    /**
     * 重新打开连接
     *
     * @return 打开状态
     * @throws IOException 关闭异常
     */
    default boolean reconnect() throws IOException {
        this.close();
        return this.connect();
    }

    /**
     * 重新打开连接
     *
     * @return 打开状态
     * @throws IOException 关闭异常
     */
    default boolean reconnectBlocking() throws IOException {
        this.close();
        return this.connectBlocking();
    }

    /**
     * 打开连接，使用节点配置的超时时间
     *
     * @return 打开状态
     */
    boolean connectBlocking();

    /**
     * 打开连接，阻塞指定时间
     *
     * @param seconds 阻塞时间  建议大于 1秒
     * @return 打开状态
     */
    boolean connectBlocking(int seconds);

    /**
     * 发送消息
     *
     * @param msg 消息
     * @throws IOException 发送异常
     */
    void send(String msg) throws IOException;

    /**
     * 发送消息
     *
     * @param bytes 消息
     * @throws IOException 发送异常
     */
    void send(ByteBuffer bytes) throws IOException;

    /**
     * 收到消息
     *
     * @param consumer 回调
     */
    void onMessage(Consumer<String> consumer);

    /**
     * 是否连接上
     *
     * @return true
     */
    boolean isConnected();

    /**
     * 获取关闭状态描述
     *
     * @return 状态描述
     */
    String getCloseStatusMsg();
}
