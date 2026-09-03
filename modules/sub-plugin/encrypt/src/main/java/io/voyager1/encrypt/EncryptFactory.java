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

import java.security.NoSuchAlgorithmException;

/**
 * @since 2023/3/9
 */
public class EncryptFactory {

    public static Encryptor createEncryptor(Integer type) throws NoSuchAlgorithmException {
        switch (type) {
            case 0:
                return NotEncryptor.getInstance();
            case 1:
                return BASE64Encryptor.getInstance();
            case 2:
                return AESEncryptor.getInstance();
            default:
                throw new NoSuchAlgorithmException("Unsupported encrypt type");
        }
    }
}
