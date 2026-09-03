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
 * JWT 头部字段，"" {@code io.voyager1.util.JWTHeader}。
 */
public enum JWTHeader {

    /** 声明类型 */
    TYPE("typ"),
    /** 签名算法 */
    ALGORITHM("alg"),
    /** 内容类型 */
    CONTENT_TYPE("cty"),
    /** JWK 的 key id */
    KEY_ID("kid");

    private final String value;

    JWTHeader(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
