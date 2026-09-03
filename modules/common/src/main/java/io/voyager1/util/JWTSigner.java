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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * JWT 签名器，"" {@code io.voyager1.util.JWTSigner}。
 */
public class JWTSigner {

    private final String algorithmId;
    private final String macAlgorithm;
    private final byte[] key;

    public JWTSigner(String algorithmId, byte[] key) {
        this.algorithmId = algorithmId;
        this.macAlgorithm = macAlgorithm(algorithmId);
        this.key = key;
    }

    /**
     * 对 {@code headerBase64 + "." + payloadBase64} 计算 HMAC 并返回 URL 安全的 Base64（无填充）。
     */
    public String sign(String headerBase64, String payloadBase64) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmac(headerBase64 + "." + payloadBase64));
    }

    public boolean verify(String headerBase64, String payloadBase64, String signBase64) {
        String computed = sign(headerBase64, payloadBase64);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                signBase64.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取算法名，如 {@code HmacSHA256}。
     */
    public String getAlgorithm() {
        return macAlgorithm;
    }

    /**
     * 获取算法 ID，如 {@code HS256}。
     */
    public String getAlgorithmId() {
        return algorithmId;
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(new SecretKeySpec(key, macAlgorithm));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 签名失败: " + macAlgorithm, e);
        }
    }

    private static String macAlgorithm(String algorithmId) {
        switch (algorithmId) {
            case "HS384":
                return "HmacSHA384";
            case "HS512":
                return "HmacSHA512";
            case "HS256":
            default:
                return "HmacSHA256";
        }
    }
}
