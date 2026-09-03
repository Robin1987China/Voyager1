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

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * URL 编解码工具，"" {@code io.voyager1.util.URLEncodeUtil}。
 */
public class URLEncodeUtil {

    /**
     * query 编码安全字符：unreserved 字符 + {@code !*'()}。
     */
    private static final BitSet QUERY_SAFE = new BitSet(256);

    static {
        for (char c : "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~".toCharArray()) {
            QUERY_SAFE.set(c);
        }
        for (char c : "!*'()".toCharArray()) {
            QUERY_SAFE.set(c);
        }
    }

    /**
     * 表单编码（空格编码为 {@code +}）。
     *
     * @param source 原文
     * @return 编码结果
     */
    public static String encode(String source) {
        if (source == null) {
            return null;
        }
        return URLEncoder.encode(source, StandardCharsets.UTF_8);
    }

    /**
     * 解码。
     *
     * @param source 编码内容
     * @return 解码结果
     */
    public static String decode(String source) {
        if (source == null) {
            return null;
        }
        return URLDecoder.decode(source, StandardCharsets.UTF_8);
    }

    /**
     * query 组件编码（空格编码为 {@code %20}）。
     *
     * @param source 原文
     * @return 编码结果
     */
    public static String encodeQuery(String source) {
        return encode(source, QUERY_SAFE);
    }

    private static String encode(String source, BitSet safe) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            int c = b & 0xff;
            if (safe.get(c)) {
                out.write(c);
            } else {
                out.write('%');
                char hi = Character.toUpperCase(Character.forDigit((c >> 4) & 0xf, 16));
                char lo = Character.toUpperCase(Character.forDigit(c & 0xf, 16));
                out.write(hi);
                out.write(lo);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
