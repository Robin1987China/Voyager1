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

import java.util.Date;

/**
 * JWT 数据校验器，"" {@code io.voyager1.util.JWTValidator}。
 */
public class JWTValidator {

    private final JWT jwt;

    /**
     * 创建 JWT 校验器。
     */
    public static JWTValidator of(String token) {
        return new JWTValidator(JWT.of(token));
    }

    /**
     * 创建 JWT 校验器。
     */
    public static JWTValidator of(JWT jwt) {
        return new JWTValidator(jwt);
    }

    public JWTValidator(JWT jwt) {
        this.jwt = jwt;
    }

    /**
     * 校验算法与签名。
     */
    public JWTValidator validateAlgorithm() {
        return validateAlgorithm(null);
    }

    /**
     * 校验算法与签名。
     *
     * @param signer 用于校验的签名器
     */
    public JWTValidator validateAlgorithm(JWTSigner signer) {
        String algorithmId = jwt.getAlgorithm();
        if (algorithmId == null || algorithmId.isEmpty()) {
            if (signer == null) {
                return this;
            }
            throw new IllegalStateException("No algorithm defined in header!");
        }
        if (signer == null) {
            throw new IllegalArgumentException("No Signer for validate algorithm!");
        }
        if (!algorithmId.equals(signer.getAlgorithmId())) {
            throw new IllegalStateException("Algorithm [" + algorithmId + "] defined in header doesn't match to [" + signer.getAlgorithmId() + "]!");
        }
        if (!jwt.verify(signer)) {
            throw new IllegalStateException("Signature verification failed!");
        }
        return this;
    }

    public JWTValidator validateDate() {
        return validateDate(new Date(), 0);
    }

    public JWTValidator validateDate(Date dateToCheck) {
        return validateDate(dateToCheck, 0);
    }

    /**
     * 校验 nbf/exp/iat 时间字段。
     *
     * @param dateToCheck 被检查的时间，一般为当前时间
     * @param leeway      容忍空间，单位：秒
     */
    public JWTValidator validateDate(Date dateToCheck, long leeway) {
        Date now = dateToCheck == null ? new Date() : dateToCheck;
        long nowMillis = now.getTime();
        long leewayMillis = leeway > 0 ? leeway * 1000 : 0;

        Date notBefore = jwt.getPayloadDate(JWT.NOT_BEFORE);
        if (notBefore != null && notBefore.getTime() > nowMillis + leewayMillis) {
            throw new IllegalStateException("'nbf':[" + notBefore + "] is after now:[" + now + "]");
        }

        Date expiresAt = jwt.getPayloadDate(JWT.EXPIRES_AT);
        if (expiresAt != null && expiresAt.getTime() < nowMillis - leewayMillis) {
            throw new IllegalStateException("'exp':[" + expiresAt + "] is before now:[" + now + "]");
        }

        Date issuedAt = jwt.getPayloadDate(JWT.ISSUED_AT);
        if (issuedAt != null && issuedAt.getTime() > nowMillis + leewayMillis) {
            throw new IllegalStateException("'iat':[" + issuedAt + "] is after now:[" + now + "]");
        }
        return this;
    }
}
