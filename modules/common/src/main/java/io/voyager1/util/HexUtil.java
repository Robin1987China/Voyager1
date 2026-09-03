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
 * Hex 工具，"" {@code .core.util.HexUtil}。
 */
public class HexUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String encodeHexStr(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX[(b >> 4) & 0x0f]).append(HEX[b & 0x0f]);
        }
        return sb.toString();
    }

    public static String encodeHexStr(byte[] data, boolean toLowerCase) {
        String s = encodeHexStr(data);
        return toLowerCase ? s.toLowerCase() : s.toUpperCase();
    }

    public static byte[] decodeHex(String hex) {
        if (hex == null) {
            return null;
        }
        String s = hex.trim();
        if (s.length() % 2 != 0) {
            s = "0" + s;
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ((Character.digit(s.charAt(i * 2), 16) << 4)
                + Character.digit(s.charAt(i * 2 + 1), 16));
        }
        return out;
    }
}
