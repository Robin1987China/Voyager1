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

import java.nio.charset.StandardCharsets;

/**
 * base64
 *
 * @since 2023/3/9
 */
public class BASE64Encryptor implements Encryptor {

    private static volatile BASE64Encryptor singleton;

    private BASE64Encryptor() {
        //构造器私有化，防止new，导致多个实例
    }

    public static Encryptor getInstance() {
        //向外暴露一个静态的公共方法  getInstance
        //第一层检查
        if (singleton == null) {
            //同步代码块
            synchronized (BASE64Encryptor.class) {
                //第二层检查
                if (singleton == null) {
                    singleton = new BASE64Encryptor();
                }
            }
        }
        return singleton;
    }

    @Override
    public String name() {
        return "base64";
    }

    @Override
    public String encrypt(String input) {
        if (input == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String input) {
        if (input == null) {
            return null;
        }
        return new String(java.util.Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
    }
}
