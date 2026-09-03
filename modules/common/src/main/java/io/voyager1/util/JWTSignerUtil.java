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

/**
 * JWT 签名器工具类，"" {@code io.voyager1.util.JWTSignerUtil}。
 */
public class JWTSignerUtil {

    private JWTSignerUtil() {
    }

    /**
     * HS256(HmacSHA256) 签名器。
     *
     * @param key 密钥
     */
    public static JWTSigner hs256(byte[] key) {
        return new JWTSigner("HS256", key);
    }

    /**
     * HS384(HmacSHA384) 签名器。
     *
     * @param key 密钥
     */
    public static JWTSigner hs384(byte[] key) {
        return new JWTSigner("HS384", key);
    }

    /**
     * HS512(HmacSHA512) 签名器。
     *
     * @param key 密钥
     */
    public static JWTSigner hs512(byte[] key) {
        return new JWTSigner("HS512", key);
    }
}
