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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Server 与 Agent 之间共享的凭证。
 * <p>
 * 取代旧方案的「账号 + 密码」概念：凭证由节点唯一标识 {@code agentId} 与一段高熵随机
 * {@code secret}（默认 32 字节）组成，secret 只作为 HMAC 密钥使用，从不作为密码在网络上传输。
 */
public final class AgentCredential {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_SECRET_BYTES = 32;
    private static final int MIN_SECRET_BYTES = 16;
    private static final String LEGACY_SECRET_DOMAIN = "voyager1:agent-secret:v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String agentId;
    private final byte[] secret;

    public AgentCredential(String agentId, byte[] secret) {
        this.agentId = Objects.requireNonNull(agentId, "agentId");
        this.secret = Objects.requireNonNull(secret, "secret");
        if (this.agentId.isEmpty()) {
            throw new IllegalArgumentException("agentId 不能为空");
        }
        if (this.secret.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("secret 至少 " + MIN_SECRET_BYTES + " 字节");
        }
    }

    /**
     * 为指定节点生成一份全新凭证（随机 secret）。
     */
    public static AgentCredential generate(String agentId) {
        byte[] secret = new byte[DEFAULT_SECRET_BYTES];
        RANDOM.nextBytes(secret);
        return new AgentCredential(agentId, secret);
    }

    /**
     * 从 Base64URL（无填充）编码的 secret 恢复凭证，用于配置读写。
     */
    public static AgentCredential fromSecretBase64(String agentId, String secretBase64) {
        return new AgentCredential(agentId, Base64.getUrlDecoder().decode(secretBase64));
    }

    /**
     * 迁移桥：从旧方案的 {@code sha1(name@pwd)} 派生新 secret。
     * <p>
     * Server 侧持有旧 {@code authorize}（sha1），Agent 侧持有密码可重算同一 sha1，
     * 两侧经相同 KDF（HMAC-SHA256 + 域分离）得到相同 secret，实现「不重新分发密钥」的向后兼容迁移。
     * 新令牌由此 secret 签名，已解决旧方案的明文/静态/可重放问题。
     *
     * @param agentId         节点唯一标识（旧 agentName）
     * @param legacyAuthorize 旧授权串 {@code sha1(name@pwd)}
     */
    public static AgentCredential fromLegacyAuthorize(String agentId, String legacyAuthorize) {
        if (legacyAuthorize == null || legacyAuthorize.isEmpty()) {
            throw new IllegalArgumentException("legacyAuthorize 不能为空");
        }
        byte[] secret = hmac(legacyAuthorize.getBytes(StandardCharsets.UTF_8), LEGACY_SECRET_DOMAIN);
        return new AgentCredential(agentId, secret);
    }

    public String agentId() {
        return agentId;
    }

    public byte[] secret() {
        return secret;
    }

    public String secretBase64() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }
}
