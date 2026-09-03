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
import java.util.Base64;

/**
 * 签署 Server→Agent 请求令牌。
 * <p>
 * 令牌格式（5 段，以 {@code .} 分隔）：
 * <pre>{@code agentId.iat.nonce.exp.sig}</pre>
 * 其中 {@code sig = base64url( HMAC-SHA256(secret, "agentId.iat.nonce.exp") )}（无填充）。
 * <p>
 * 该令牌取代旧方案 {@code sha1(name@pwd)}：每次请求使用一次性 nonce 与短 TTL，
 * 即使被截获也无法重放。
 */
public final class AgentTokenSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 签署令牌。
     *
     * @param credential      共享凭证
     * @param issuedAtSeconds 签发时间（epoch 秒）
     * @param nonce           一次性随机串
     * @param ttlSeconds      有效期（秒）
     * @return 完整令牌字符串
     */
    public String sign(AgentCredential credential, long issuedAtSeconds, String nonce, long ttlSeconds) {
        if (nonce == null || nonce.isEmpty()) {
            throw new IllegalArgumentException("nonce 不能为空");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds 必须为正数");
        }
        long expiresAtSeconds = issuedAtSeconds + ttlSeconds;
        String payload = credential.agentId() + "." + issuedAtSeconds + "." + nonce + "." + expiresAtSeconds;
        String signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(hmac(credential.secret(), payload));
        return payload + "." + signature;
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
