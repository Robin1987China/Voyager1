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

package io.voyager1.plugins;

import java.nio.charset.Charset;

/**
 * @since 2023/4/6
 */
public interface ISshInfo {

    int timeout();

    String host();

    ConnectType connectType();

    Charset charset();

    int port();

    String user();

    String password();

    /**
     * 私钥
     *
     * @return 私钥
     */
    String privateKey();

    /**
     * id
     *
     * @return 数据id
     */
    String id();

    enum ConnectType {
        /**
         * 账号密码
         */
        PASS,
        /**
         * 密钥
         */
        PUBKEY
    }
}
