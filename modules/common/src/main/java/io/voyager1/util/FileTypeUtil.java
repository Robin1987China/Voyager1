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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 文件类型判断工具，"" {@code io.voyager1.util.FileTypeUtil}。
 * <p>根据文件头部魔数猜测文件类型，返回不带点的小写扩展名，无法识别时按扩展名兜底。</p>
 */
public class FileTypeUtil {

    private FileTypeUtil() {
    }

    /**
     * 根据文件头部信息获得文件类型（扩展名，不带点）。
     *
     * @param file 文件
     * @return 类型，无法识别为 {@code null}
     */
    public static String getType(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        String type = null;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            type = getType(in);
        } catch (IOException ignore) {
            // 忽略读取异常，走扩展名兜底
        }
        if (type != null) {
            return type;
        }
        return extName(file.getName());
    }

    /**
     * 根据文件头部信息获得文件类型（扩展名，不带点）。
     * <p>注意：此方法会读取头部若干字节，导致流后续读取缺失，如需复用流请先支持 reset。</p>
     *
     * @param in 输入流
     * @return 类型，无法识别为 {@code null}
     */
    public static String getType(InputStream in) {
        return getType(in, false);
    }

    /**
     * 根据文件头部信息获得文件类型（扩展名，不带点）。
     *
     * @param in      输入流
     * @param isExact 是否精确匹配（使用更多字节）
     * @return 类型，无法识别为 {@code null}
     */
    public static String getType(InputStream in, boolean isExact) {
        if (in == null) {
            return null;
        }
        try {
            byte[] head = in.readNBytes(isExact ? 64 : 16);
            if (head.length == 0) {
                return null;
            }
            return matchMagic(head);
        } catch (IOException e) {
            return null;
        }
    }

    private static String matchMagic(byte[] b) {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (startsWith(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png";
        }
        // JPG: FF D8 FF
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        // GIF: "GIF8"
        if (startsWith(b, 'G', 'I', 'F', '8')) {
            return "gif";
        }
        // ICO: 00 00 01 00
        if (startsWith(b, 0x00, 0x00, 0x01, 0x00)) {
            return "ico";
        }
        // BMP: "BM"
        if (startsWith(b, 'B', 'M')) {
            return "bmp";
        }
        // PDF: "%PDF"
        if (startsWith(b, '%', 'P', 'D', 'F')) {
            return "pdf";
        }
        // ZIP 系列: "PK" 03 04 / 05 06 / 07 08
        if (b.length >= 4 && b[0] == 'P' && b[1] == 'K'
                && ((b[2] == 0x03 && b[3] == 0x04) || (b[2] == 0x05 && b[3] == 0x06) || (b[2] == 0x07 && b[3] == 0x08))) {
            return "zip";
        }
        // TIFF: "II" 2A 00 或 "MM" 00 2A
        if (startsWith(b, 'I', 'I', 0x2A, 0x00) || startsWith(b, 'M', 'M', 0x00, 0x2A)) {
            return "tiff";
        }
        // WEBP: "RIFF" .... "WEBP"
        if (b.length >= 12 && startsWith(b, 'R', 'I', 'F', 'F')
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] b, int... expected) {
        if (b.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((b[i] & 0xFF) != (expected[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    private static String extName(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
