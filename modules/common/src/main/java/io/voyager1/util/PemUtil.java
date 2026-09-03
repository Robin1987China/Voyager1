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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  {@code io.voyager1.util.PemUtil} 的兼容实现。
 *
 * <p>纯 JDK 实现，不依赖 BouncyCastle。支持以下私钥 PEM 格式：</p>
 * <ul>
 *     <li>PKCS#8 {@code BEGIN PRIVATE KEY}（RSA / EC / DSA / Ed25519）</li>
 *     <li>PKCS#1 {@code BEGIN RSA PRIVATE KEY}</li>
 *     <li>SEC1 {@code BEGIN EC PRIVATE KEY}</li>
 * </ul>
 */
public final class PemUtil {

    private static final Pattern PEM_BLOCK_PATTERN = Pattern.compile("-----BEGIN ([^-]+)-----([\\s\\S]*?)-----END \\1-----");

    private static final String[] PRIVATE_KEY_ALGORITHMS = {
        "RSA", "EC", "DSA", "Ed25519", "Ed448", "DH"
    };

    private PemUtil() {
    }

    // ------------------------------------------------------------------ 公开 API

    public static PrivateKey readPemPrivateKey(InputStream inputStream) {
        try {
            String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return readPemPrivateKey(pem);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取 PEM 私钥失败", e);
        }
    }

    public static PrivateKey readPemPrivateKey(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return readPemPrivateKey(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取 PEM 私钥失败", e);
        }
    }

    public static PrivateKey readPemPrivateKey(byte[] content) {
        return readPemPrivateKey(new String(content, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ 内部

    private static PrivateKey readPemPrivateKey(String pem) {
        Matcher matcher = PEM_BLOCK_PATTERN.matcher(pem);
        while (matcher.find()) {
            String type = matcher.group(1).trim();
            if (!type.endsWith("PRIVATE KEY")) {
                continue;
            }
            String body = matcher.group(2).replaceAll("\\s+", "");
            byte[] der = Base64.getMimeDecoder().decode(body);
            try {
                switch (type) {
                    case "PRIVATE KEY":
                        return generatePrivateKeyFromPkcs8(der);
                    case "RSA PRIVATE KEY":
                        return parsePkcs1RsaPrivateKey(der);
                    case "EC PRIVATE KEY":
                        return parseSec1EcPrivateKey(der);
                    case "ENCRYPTED PRIVATE KEY":
                        throw new IllegalArgumentException("不支持加密的 PEM 私钥");
                    default:
                        throw new IllegalArgumentException("不支持的 PEM 私钥类型：" + type);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("解析 PEM 私钥失败：" + type, e);
            }
        }
        throw new IllegalArgumentException("无法解析 PEM 私钥内容");
    }

    private static PrivateKey generatePrivateKeyFromPkcs8(byte[] der) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        Exception last = null;
        for (String algorithm : PRIVATE_KEY_ALGORITHMS) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (Exception e) {
                last = e;
            }
        }
        throw new IllegalArgumentException("无法解析 PKCS#8 私钥", last);
    }

    /**
     * 解析 PKCS#1 RSA 私钥，提取 modulus 与 privateExponent 构造私钥。
     */
    private static PrivateKey parsePkcs1RsaPrivateKey(byte[] der) throws Exception {
        int[] pos = new int[]{0};
        int tag = readTag(der, pos);
        int length = readLength(der, pos);
        if (tag != 0x30) {
            throw new IllegalArgumentException("非法的 PKCS#1 RSA 私钥");
        }
        int end = pos[0] + length;
        List<BigInteger> integers = new ArrayList<>();
        while (pos[0] < end) {
            int t = readTag(der, pos);
            int l = readLength(der, pos);
            byte[] content = Arrays.copyOfRange(der, pos[0], pos[0] + l);
            pos[0] += l;
            if (t == 0x02) {
                integers.add(new BigInteger(content));
            }
        }
        if (integers.size() < 4) {
            throw new IllegalArgumentException("非法的 PKCS#1 RSA 私钥结构");
        }
        BigInteger modulus = integers.get(1);
        BigInteger privateExponent = integers.get(3);
        return KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, privateExponent));
    }

    /**
     * 将 SEC1 EC 私钥包装为 PKCS#8 后解析。
     */
    private static PrivateKey parseSec1EcPrivateKey(byte[] der) throws Exception {
        int[] pos = new int[]{0};
        int tag = readTag(der, pos);
        int length = readLength(der, pos);
        if (tag != 0x30) {
            throw new IllegalArgumentException("非法的 SEC1 EC 私钥");
        }
        int end = pos[0] + length;
        byte[] curveOidContent = null;
        while (pos[0] < end) {
            int t = readTag(der, pos);
            int l = readLength(der, pos);
            byte[] content = Arrays.copyOfRange(der, pos[0], pos[0] + l);
            pos[0] += l;
            if (t == 0xA0) {
                // [0] EXPLICIT ECParameters（命名曲线为单个 OID）
                int[] p2 = new int[]{0};
                int t2 = readTag(content, p2);
                int l2 = readLength(content, p2);
                if (t2 != 0x06) {
                    throw new IllegalArgumentException("SEC1 EC 私钥缺少命名曲线");
                }
                curveOidContent = Arrays.copyOfRange(content, p2[0], p2[0] + l2);
            }
        }
        if (curveOidContent == null) {
            throw new IllegalArgumentException("SEC1 EC 私钥缺少命名曲线参数");
        }
        String curveOid = decodeOid(curveOidContent);
        byte[] algorithmIdentifier = tlv(0x30, concat(encodeOid("1.2.840.10045.2.1"), encodeOid(curveOid)));
        byte[] version = encodeInteger(BigInteger.ZERO);
        byte[] pkcs8 = tlv(0x30, concat(version, algorithmIdentifier, tlv(0x04, der)));
        return generatePrivateKeyFromPkcs8(pkcs8);
    }

    // ------------------------------------------------------------------ DER 读取

    private static int readTag(byte[] der, int[] pos) {
        return der[pos[0]++] & 0xFF;
    }

    private static int readLength(byte[] der, int[] pos) {
        int first = der[pos[0]++] & 0xFF;
        if ((first & 0x80) == 0) {
            return first;
        }
        int numBytes = first & 0x7F;
        int length = 0;
        for (int i = 0; i < numBytes; i++) {
            length = (length << 8) | (der[pos[0]++] & 0xFF);
        }
        return length;
    }

    // ------------------------------------------------------------------ DER 写入

    private static byte[] tlv(int tag, byte[] content) {
        return concat(new byte[]{(byte) tag}, encodeLength(content.length), content);
    }

    private static byte[] encodeLength(int length) {
        if (length < 0x80) {
            return new byte[]{(byte) length};
        }
        int count = 0;
        int n = length;
        while (n > 0) {
            n >>>= 8;
            count++;
        }
        byte[] result = new byte[count + 1];
        result[0] = (byte) (0x80 | count);
        n = length;
        for (int i = count; i >= 1; i--) {
            result[i] = (byte) (n & 0xFF);
            n >>>= 8;
        }
        return result;
    }

    private static byte[] encodeInteger(BigInteger value) {
        return tlv(0x02, value.toByteArray());
    }

    private static byte[] encodeOid(String oid) {
        String[] parts = oid.split("\\.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long first = Long.parseLong(parts[0]) * 40 + Long.parseLong(parts[1]);
        writeBase128(out, first);
        for (int i = 2; i < parts.length; i++) {
            writeBase128(out, Long.parseLong(parts[i]));
        }
        return tlv(0x06, out.toByteArray());
    }

    private static void writeBase128(ByteArrayOutputStream out, long value) {
        byte[] buffer = new byte[10];
        int pos = 10;
        buffer[--pos] = (byte) (value & 0x7F);
        value >>>= 7;
        while (value > 0) {
            buffer[--pos] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(buffer, pos, 10 - pos);
    }

    private static String decodeOid(byte[] content) {
        StringBuilder sb = new StringBuilder();
        int first = content[0] & 0xFF;
        if (first < 40) {
            sb.append('0').append('.').append(first);
        } else if (first < 80) {
            sb.append('1').append('.').append(first - 40);
        } else {
            sb.append('2').append('.').append(first - 80);
        }
        long value = 0;
        for (int i = 1; i < content.length; i++) {
            int b = content[i] & 0xFF;
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                sb.append('.').append(value);
                value = 0;
            }
        }
        return sb.toString();
    }

    private static byte[] concat(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            out.write(array, 0, array.length);
        }
        return out.toByteArray();
    }
}
