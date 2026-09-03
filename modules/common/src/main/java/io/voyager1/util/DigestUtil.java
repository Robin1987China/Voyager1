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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 摘要工具，提供 SHA-1/MD5/SHA-256 等摘要算法。
 */
public class DigestUtil {

    public static String sha1(String data) {
        return digest(data, "SHA-1");
    }

    public static String md5(String data) {
        return digest(data, "MD5");
    }

    public static String md5(java.io.File file) {
        return digestFile(file, "MD5");
    }

    public static String sha1(java.io.File file) {
        return digestFile(file, "SHA-1");
    }

    private static String digestFile(java.io.File file, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }
            }
            return toHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算文件摘要失败: " + file, e);
        }
    }

    public static String sha256(String data) {
        return digest(data, "SHA-256");
    }

    public static Digester sha256() {
        try {
            return new Digester(java.security.MessageDigest.getInstance("SHA-256"));
        } catch (Exception e) {
            throw new RuntimeException("创建 SHA-256 摘要器失败", e);
        }
    }

    public static String sha1(byte[] data) {
        return toHex(digestBytes(data, "SHA-1"));
    }

    public static String md5(byte[] data) {
        return toHex(digestBytes(data, "MD5"));
    }

    public static byte[] sha1Bytes(String data) {
        return digestBytes(data, "SHA-1");
    }

    public static byte[] md5Bytes(String data) {
        return digestBytes(data, "MD5");
    }

    private static String digest(String data, String algorithm) {
        return toHex(digestBytes(data, algorithm));
    }

    private static byte[] digestBytes(String data, String algorithm) {
        return digestBytes(data.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    private static byte[] digestBytes(byte[] data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的摘要算法: " + algorithm, e);
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
