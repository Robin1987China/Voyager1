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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON Web Token (JWT)，"" {@code io.voyager1.util.JWT}。
 *
 * <p>结构为 {@code header.payload.signature}，其中 header/payload 为 URL 安全 Base64（无填充）
 * 编码的 JSON，签名为对 {@code header + "." + payload} 的 HMAC 结果。
 */
public class JWT {

    /** jwt 签发者 */
    public static final String ISSUER = "iss";
    /** jwt 所面向的用户 */
    public static final String SUBJECT = "sub";
    /** 接收 jwt 的一方 */
    public static final String AUDIENCE = "aud";
    /** jwt 的过期时间 */
    public static final String EXPIRES_AT = "exp";
    /** 生效时间 */
    public static final String NOT_BEFORE = "nbf";
    /** jwt 的签发时间 */
    public static final String ISSUED_AT = "iat";
    /** jwt 的唯一身份标识 */
    public static final String JWT_ID = "jti";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Object> header;
    private final Map<String, Object> payload;

    private String headerSegment;
    private String payloadSegment;
    private String signatureSegment;

    /**
     * 创建空的 JWT 对象。
     */
    public static JWT create() {
        return new JWT();
    }

    /**
     * 创建并解析 JWT 对象（仅解析，不校验签名）。
     */
    public static JWT of(String token) {
        return new JWT(token);
    }

    public JWT() {
        this.header = new LinkedHashMap<>();
        this.payload = new LinkedHashMap<>();
    }

    public JWT(String token) {
        this();
        parse(token);
    }

    /**
     * 解析 JWT 内容。
     */
    public JWT parse(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token String must be not blank!");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("The token was expected 3 parts, but got " + parts.length + ".");
        }
        this.headerSegment = parts[0];
        this.payloadSegment = parts[1];
        this.signatureSegment = parts[2];
        this.header.clear();
        this.header.putAll(decodeJson(parts[0]));
        this.payload.clear();
        this.payload.putAll(decodeJson(parts[1]));
        return this;
    }

    public JWT setHeader(String name, Object value) {
        header.put(name, value);
        return this;
    }

    public JWT setHeader(JWTHeader header, Object value) {
        this.header.put(header.getValue(), value);
        return this;
    }

    public Object getHeader(String name) {
        return header.get(name);
    }

    /**
     * 获取算法 ID（alg 头信息）。
     */
    public String getAlgorithm() {
        Object alg = header.get(JWTHeader.ALGORITHM.getValue());
        return alg == null ? null : String.valueOf(alg);
    }

    public JWT setPayload(String name, Object value) {
        if (value == null) {
            payload.remove(name);
        } else {
            payload.put(name, value);
        }
        return this;
    }

    public Object getPayload(String name) {
        return payload.get(name);
    }

    /**
     * 获取所有载荷信息。
     */
    public Map<String, Object> getPayloads() {
        return new LinkedHashMap<>(payload);
    }

    public JWT setIssuer(String issuer) {
        return setPayload(ISSUER, issuer);
    }

    public JWT setSubject(String subject) {
        return setPayload(SUBJECT, subject);
    }

    public JWT setAudience(String... audience) {
        return setPayload(AUDIENCE, audience);
    }

    public JWT setExpiresAt(Date expiresAt) {
        return setPayload(EXPIRES_AT, expiresAt);
    }

    public JWT setNotBefore(Date notBefore) {
        return setPayload(NOT_BEFORE, notBefore);
    }

    public JWT setIssuedAt(Date issuedAt) {
        return setPayload(ISSUED_AT, issuedAt);
    }

    public JWT setJWTId(String jwtId) {
        return setPayload(JWT_ID, jwtId);
    }

    /**
     * 读取时间类载荷（iat/exp/nbf），其值为秒级时间戳。
     */
    public Date getPayloadDate(String name) {
        Object value = payload.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue() * 1000);
        }
        try {
            return new Date(Long.parseLong(value.toString()) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 签名生成 JWT 字符串。
     */
    public String sign(JWTSigner signer) {
        if (signer == null) {
            throw new IllegalArgumentException("No Signer provided!");
        }
        if (isBlank(header.get(JWTHeader.TYPE.getValue()))) {
            header.put(JWTHeader.TYPE.getValue(), "JWT");
        }
        if (isBlank(header.get(JWTHeader.ALGORITHM.getValue()))) {
            header.put(JWTHeader.ALGORITHM.getValue(), signer.getAlgorithmId());
        }

        String headerBase64 = encode(toJson(header));
        String payloadBase64 = encode(toJson(payload));
        String sign = signer.sign(headerBase64, payloadBase64);
        return headerBase64 + "." + payloadBase64 + "." + sign;
    }

    /**
     * 校验 JWT 签名是否有效。
     */
    public boolean verify(JWTSigner signer) {
        if (signer == null) {
            throw new IllegalArgumentException("No Signer provided!");
        }
        if (headerSegment == null) {
            throw new IllegalStateException("No token to verify!");
        }
        return signer.verify(headerSegment, payloadSegment, signatureSegment);
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String toJson(Map<String, Object> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Date) {
                copy.put(entry.getKey(), ((Date) value).getTime() / 1000);
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        try {
            return MAPPER.writeValueAsString(copy);
        } catch (Exception e) {
            throw new IllegalStateException("JWT JSON 序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decodeJson(String base64) {
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWT Base64 解码失败", e);
        }
        try {
            return MAPPER.readValue(bytes, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT JSON 解析失败", e);
        }
    }
}
