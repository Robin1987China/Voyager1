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

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 *  {@code io.voyager1.util.RSA} 的兼容实现。
 *
 * <p>基于 JDK 自带的 RSA 实现，仅覆盖代码库实际使用到的 API。</p>
 */
public class RSA {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public RSA(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public byte[] encrypt(byte[] data, KeyType keyType) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keyType == KeyType.PublicKey ? publicKey : privateKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("RSA 加密失败", e);
        }
    }

    public byte[] decrypt(byte[] data, KeyType keyType) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyType == KeyType.PrivateKey ? privateKey : publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("RSA 解密失败", e);
        }
    }

    public String encryptBase64(String data, KeyType keyType) {
        byte[] encrypted = encrypt(data.getBytes(StandardCharsets.UTF_8), keyType);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptStr(String data, KeyType keyType) {
        byte[] decrypted = decrypt(Base64.getDecoder().decode(data), keyType);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
