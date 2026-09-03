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

package io.voyager1.encrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * aes
 *
 * @since 2023/3/9
 */
public class AESEncryptor implements Encryptor {

    private final byte[] keyByte;

    private static volatile AESEncryptor singleton;

    private AESEncryptor(String key) {
        //构造器私有化，防止new，导致多个实例
        this.keyByte = key.getBytes(StandardCharsets.UTF_8);
    }

    public static Encryptor getInstance() {
        //向外暴露一个静态的公共方法  getInstance
        //第一层检查
        if (singleton == null) {
            //同步代码块
            synchronized (AESEncryptor.class) {
                //第二层检查
                if (singleton == null) {
                    // 默认 AES 密钥仅用于开发环境，生产部署务必通过环境变量 VOYAGER1_ENCRYPT_AES_KEY 覆盖
                    String aesKey = getProperty("VOYAGER1_ENCRYPT_AES_KEY", "Djnn3runZBzdv9Nv");
                    singleton = new AESEncryptor(aesKey);
                }
            }
        }
        return singleton;
    }

    private static String getProperty(String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        String prop = System.getProperty(key);
        return prop != null && !prop.isEmpty() ? prop : defaultValue;
    }

    @Override
    public String name() {
        return "aes";
    }

    @Override
    public String encrypt(String input) throws Exception {
        if (input == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyByte, "AES"));
        byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return toHex(encrypted);
    }

    @Override
    public String decrypt(String input) throws Exception {
        if (input == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyByte, "AES"));
        byte[] decrypted = cipher.doFinal(fromHex(input));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
