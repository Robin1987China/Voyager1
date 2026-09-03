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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 令牌签名/校验纯函数测试。
 */
public class AgentTokenSignerVerifierTest {

    private final AgentTokenSigner signer = new AgentTokenSigner();
    private final AgentTokenVerifier verifier = new AgentTokenVerifier();
    private final AgentCredential credential = AgentCredential.generate("agent-001");

    @Test
    public void testSignAndVerifyRoundTrip() {
        String token = signer.sign(credential, 1_700_000_000L, "nonce-1", 300);
        assertNotNull(token);
        assertTrue(token.startsWith("agent-001."));
        assertTrue(verifier.verify(credential, token, 1_700_000_000L));
    }

    @Test
    public void testTamperedPayloadRejected() {
        String token = signer.sign(credential, 1_700_000_000L, "nonce-1", 300);
        // 篡改 iat 字段（第 2 段）
        String[] parts = token.split("\\.");
        parts[1] = String.valueOf(1_700_000_100L);
        String tampered = String.join(".", parts);
        assertFalse(verifier.verify(credential, tampered, 1_700_000_000L));
    }

    @Test
    public void testExpiredTokenRejected() {
        String token = signer.sign(credential, 1_700_000_000L, "nonce-1", 300);
        // 当前时间已超过 exp
        assertFalse(verifier.verify(credential, token, 1_700_000_301L));
    }

    @Test
    public void testWrongAgentIdRejected() {
        AgentCredential other = AgentCredential.generate("agent-other");
        String token = signer.sign(credential, 1_700_000_000L, "nonce-1", 300);
        assertFalse(verifier.verify(other, token, 1_700_000_000L));
    }

    @Test
    public void testWrongSecretRejected() {
        AgentCredential other = AgentCredential.generate("agent-001");
        String token = signer.sign(credential, 1_700_000_000L, "nonce-1", 300);
        assertFalse(verifier.verify(other, token, 1_700_000_000L));
    }

    @Test
    public void testReplayedNonceRejected() {
        String token = signer.sign(credential, 1_700_000_000L, "nonce-replay", 300);
        assertTrue(verifier.verify(credential, token, 1_700_000_000L));
        // 同一 nonce 重放（即使换新 token）应被拒绝
        String token2 = signer.sign(credential, 1_700_000_000L, "nonce-replay", 300);
        assertFalse(verifier.verify(credential, token2, 1_700_000_000L));
    }

    @Test
    public void testMalformedTokenRejected() {
        assertFalse(verifier.verify(credential, "too.few.parts", 1_700_000_000L));
        assertFalse(verifier.verify(credential, "", 1_700_000_000L));
        assertFalse(verifier.verify(credential, null, 1_700_000_000L));
        assertFalse(verifier.verify(credential, "a.b.c.d.e.f", 1_700_000_000L));
    }

    @Test
    public void testClockSkewRejected() {
        String token = signer.sign(credential, 1_700_000_000L, "nonce-skew", 300);
        // 当前时间远早于签发时间（超过 60s 容忍）
        assertFalse(verifier.verify(credential, token, 1_699_999_000L));
    }

    @Test
    public void testLegacyDerivationProducesSameCredentialOnBothSides() {
        // 模拟 Server 侧与 Agent 侧各自从旧 sha1(name@pwd) 派生同一凭证
        String legacyAuthorize = "d5b3db3f31fffdec4d122d327afbb3b01b25ffaf";
        AgentCredential serverSide = AgentCredential.fromLegacyAuthorize("agent-001", legacyAuthorize);
        AgentCredential agentSide = AgentCredential.fromLegacyAuthorize("agent-001", legacyAuthorize);
        String token = signer.sign(serverSide, 1_700_000_000L, "nonce-legacy", 300);
        assertTrue(verifier.verify(agentSide, token, 1_700_000_000L));
        // 不同 legacy 派生出的 secret 不能互验
        AgentCredential other = AgentCredential.fromLegacyAuthorize("agent-001", "other-sha1");
        assertFalse(verifier.verify(other, token, 1_700_000_000L));
    }

    @Test
    public void testCredentialSecretRoundTrip() {
        String encoded = credential.secretBase64();
        AgentCredential restored = AgentCredential.fromSecretBase64(credential.agentId(), encoded);
        String token = signer.sign(credential, 1_700_000_000L, "nonce-2", 300);
        assertTrue(verifier.verify(restored, token, 1_700_000_000L));
    }
}
