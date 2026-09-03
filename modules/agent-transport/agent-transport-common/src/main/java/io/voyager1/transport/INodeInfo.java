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

import java.net.Proxy;

/**
 * 节点通讯的 接口
 *
 * @since 2022/12/23
 */
public interface INodeInfo {

    /**
     * 节点名称
     *
     * @return 名称
     */
    String name();

    /**
     * 通讯登录账号（鉴权身份标识）。
     * <p>
     * 用于新令牌签名的 {@code agentId} 字段；默认退回 {@link #name()}，
     * 实现类可用登录账号覆盖（如 {@code voyager1Username}）。
     *
     * @return 登录账号
     */
    default String loginName() {
        return name();
    }

    /**
     * 节点 url
     * <p>
     * HOST:PORT
     *
     * @return 节点 url
     */
    String url();

    /**
     * 协议
     *
     * @return http
     */
    String scheme();

    /**
     * 节点 授权信息
     * sha1(user@pwd)
     *
     * @return 用户
     */
    String authorize();

    /**
     * 节点通讯代理
     *
     * @return proxy
     */
    Proxy proxy();

    /**
     * 超时时间
     *
     * @return 超时时间 单位秒
     */
    Integer timeout();

    /**
     * 传输加密方式
     *
     * @return 传输加密方式 0 不加密 1 BASE64 2 AES
     */
    Integer transportEncryption();
}
