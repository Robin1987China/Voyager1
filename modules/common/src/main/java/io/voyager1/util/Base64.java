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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Base64 编码解码 {@code io.voyager1.util.Base64}。
 */
public class Base64 {

    /**
     * 编码字节数组。
     *
     * @param data 字节数组
     * @return Base64 字符串
     */
    public static String encode(byte[] data) {
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    /**
     * 解码 Base64 字符串。
     *
     * @param base64 Base64 字符串
     * @return 字节数组
     */
    public static byte[] decode(String base64) {
        return java.util.Base64.getDecoder().decode(base64);
    }

    /**
     * 编码文件内容。
     *
     * @param file 文件
     * @return Base64 字符串
     */
    public static String encode(File file) {
        try {
            return encode(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UtilException(e);
        }
    }

    /**
     * 编码输入流内容。
     *
     * @param inputStream 输入流
     * @return Base64 字符串
     */
    public static String encode(InputStream inputStream) {
        try {
            return encode(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new UtilException(e);
        }
    }
}
