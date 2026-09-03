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
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 *  {@code io.voyager1.util.ECIES} 的兼容实现。
 *
 * <p>纯 JDK 实现：临时 ECDH + SHA-256 KDF + AES/GCM。仅保证本实现的加密与解密
 * 可互相往返（项目内仅用于校验公私钥匹配），不与外部 ECIES 实现互通。</p>
 */
public class ECIES {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public ECIES(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public byte[] encrypt(byte[] data, KeyType keyType) {
        try {
            if (!(publicKey instanceof ECPublicKey)) {
                throw new IllegalStateException("ECIES 仅支持 EC 公钥");
            }
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            AlgorithmParameterSpec params = ecPublicKey.getParams();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(params);
            KeyPair ephemeral = generator.generateKeyPair();

            byte[] sharedSecret = deriveSharedSecret(ephemeral.getPrivate(), publicKey);
            SecretKeySpec aesKey = deriveAesKey(sharedSecret);

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(data);

            byte[] ephemeralPublicKey = ephemeral.getPublic().getEncoded();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(intToBytes(ephemeralPublicKey.length));
            out.write(ephemeralPublicKey);
            out.write(iv);
            out.write(ciphertext);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("ECIES 加密失败", e);
        }
    }

    public byte[] decrypt(byte[] data, KeyType keyType) {
        try {
            int offset = 0;
            int ephemeralKeyLength = bytesToInt(data, offset);
            offset += 4;
            byte[] ephemeralPublicKeyBytes = Arrays.copyOfRange(data, offset, offset + ephemeralKeyLength);
            offset += ephemeralKeyLength;
            byte[] iv = Arrays.copyOfRange(data, offset, offset + IV_LENGTH);
            offset += IV_LENGTH;
            byte[] ciphertext = Arrays.copyOfRange(data, offset, data.length);

            PublicKey ephemeralPublicKey = KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(ephemeralPublicKeyBytes));

            byte[] sharedSecret = deriveSharedSecret(privateKey, ephemeralPublicKey);
            SecretKeySpec aesKey = deriveAesKey(sharedSecret);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("ECIES 解密失败", e);
        }
    }

    /**
     * 解密 Base64 字符串，返回明文字节数组（与  语义一致）。
     */
    public byte[] decrypt(String data, KeyType keyType) {
        return decrypt(Base64.getDecoder().decode(data), keyType);
    }

    public String encryptBase64(String data, KeyType keyType) {
        byte[] encrypted = encrypt(data.getBytes(StandardCharsets.UTF_8), keyType);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptStr(String data, KeyType keyType) {
        byte[] decrypted = decrypt(data, keyType);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static byte[] deriveSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    private static SecretKeySpec deriveAesKey(byte[] sharedSecret) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(sharedSecret), "AES");
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }

    private static int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
            | ((bytes[offset + 1] & 0xFF) << 16)
            | ((bytes[offset + 2] & 0xFF) << 8)
            | (bytes[offset + 3] & 0xFF);
    }
}
