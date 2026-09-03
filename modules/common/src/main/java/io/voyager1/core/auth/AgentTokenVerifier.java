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

package io.voyager1.core.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 校验 Server→Agent 请求令牌。
 * <p>
 * 依次校验：格式与字段、agentId 匹配、未过期、签发时间在时钟偏差容忍内、HMAC 签名（恒定时间比较）、
 * nonce 未重放。任一失败即拒绝。
 */
public final class AgentTokenVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_CLOCK_SKEW_SECONDS = 60;

    private final NonceStore nonceStore;

    public AgentTokenVerifier() {
        this(new NonceStore());
    }

    public AgentTokenVerifier(NonceStore nonceStore) {
        this.nonceStore = nonceStore;
    }

    /**
     * 校验令牌。
     *
     * @param credential  期望的共享凭证
     * @param token       待校验令牌
     * @param nowSeconds  当前时间（epoch 秒）
     * @return {@code true} 表示合法
     */
    public boolean verify(AgentCredential credential, String token, long nowSeconds) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 5) {
            return false;
        }
        String agentId = parts[0];
        String iatStr = parts[1];
        String nonce = parts[2];
        String expStr = parts[3];
        String signature = parts[4];

        if (!credential.agentId().equals(agentId)) {
            return false;
        }
        long issuedAtSeconds;
        long expiresAtSeconds;
        try {
            issuedAtSeconds = Long.parseLong(iatStr);
            expiresAtSeconds = Long.parseLong(expStr);
        } catch (NumberFormatException e) {
            return false;
        }
        if (nowSeconds > expiresAtSeconds) {
            return false; // 已过期
        }
        if (nowSeconds + MAX_CLOCK_SKEW_SECONDS < issuedAtSeconds) {
            return false; // 签发时间晚于当前时间过多（时钟偏差异常）
        }

        String payload = agentId + "." + issuedAtSeconds + "." + nonce + "." + expiresAtSeconds;
        byte[] expected = hmac(credential.secret(), payload);
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            return false; // 签名不匹配（恒定时间比较）
        }
        return nonceStore.mark(nonce); // nonce 重放检测
    }

    private byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }
}
