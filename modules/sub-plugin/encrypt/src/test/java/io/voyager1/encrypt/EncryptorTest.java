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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

/**
 * 加密插件 round-trip 契约测试
 *
 * @since 2026/8/3
 */
public class EncryptorTest {

    @Test
    public void testBase64RoundTrip() throws Exception {
        Encryptor encryptor = BASE64Encryptor.getInstance();
        Assertions.assertEquals("base64", encryptor.name());

        String plain = "hello voyager1 123!";
        String encrypted = encryptor.encrypt(plain);
        Assertions.assertNotEquals(plain, encrypted);
        Assertions.assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    public void testBase64Null() throws Exception {
        Encryptor encryptor = BASE64Encryptor.getInstance();
        Assertions.assertNull(encryptor.encrypt(null));
        Assertions.assertNull(encryptor.decrypt(null));
    }

    @Test
    public void testAesRoundTrip() throws Exception {
        Encryptor encryptor = AESEncryptor.getInstance();
        Assertions.assertEquals("aes", encryptor.name());

        String plain = "hello voyager1 aes 456!";
        String encrypted = encryptor.encrypt(plain);
        Assertions.assertNotEquals(plain, encrypted);
        Assertions.assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    public void testFactory() throws NoSuchAlgorithmException {
        Assertions.assertEquals("no", EncryptFactory.createEncryptor(0).name());
        Assertions.assertEquals("base64", EncryptFactory.createEncryptor(1).name());
        Assertions.assertEquals("aes", EncryptFactory.createEncryptor(2).name());
        Assertions.assertThrows(NoSuchAlgorithmException.class, () -> EncryptFactory.createEncryptor(99));
    }

    @Test
    public void testNotEncryptor() throws Exception {
        Encryptor encryptor = NotEncryptor.getInstance();
        String value = "plain";
        Assertions.assertEquals(value, encryptor.encrypt(value));
        Assertions.assertEquals(value, encryptor.decrypt(value));
    }
}
