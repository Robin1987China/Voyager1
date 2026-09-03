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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 字符集探测 {@code io.voyager1.util.CharsetDetector}。
 */
public class CharsetDetector {

    /**
     * 探测文件字符集。支持 UTF-8 / UTF-16 / UTF-32 BOM，无 BOM 时默认 UTF-8。
     *
     * @param file 文件
     * @return 字符集
     */
    public static Charset detect(File file) {
        if (file == null || !file.isFile()) {
            return Charset.defaultCharset();
        }
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return detect(in);
        } catch (IOException e) {
            return Charset.defaultCharset();
        }
    }

    /**
     * 探测输入流字符集。
     *
     * @param in 输入流
     * @return 字符集
     */
    public static Charset detect(InputStream in) throws IOException {
        byte[] head = new byte[4];
        int len = 0;
        int read;
        while (len < head.length && (read = in.read(head, len, head.length - len)) != -1) {
            len += read;
        }
        return detect(head, len);
    }

    private static Charset detect(byte[] bytes, int len) {
        if (len >= 3 && (bytes[0] & 0xff) == 0xEF && (bytes[1] & 0xff) == 0xBB && (bytes[2] & 0xff) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (len >= 4 && bytes[0] == 0 && bytes[1] == 0 && (bytes[2] & 0xff) == 0xFE && (bytes[3] & 0xff) == 0xFF) {
            return Charset.forName("UTF-32BE");
        }
        if (len >= 4 && (bytes[0] & 0xff) == 0xFF && (bytes[1] & 0xff) == 0xFE && bytes[2] == 0 && bytes[3] == 0) {
            return Charset.forName("UTF-32LE");
        }
        if (len >= 2 && (bytes[0] & 0xff) == 0xFE && (bytes[1] & 0xff) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (len >= 2 && (bytes[0] & 0xff) == 0xFF && (bytes[1] & 0xff) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        return StandardCharsets.UTF_8;
    }
}
