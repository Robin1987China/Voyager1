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

/**
 * @since 2023/3/9
 */
public interface Encryptor {


    /**
     * 加密方法
     *
     * @return 名称
     */
    String name();

    /**
     * 加密
     *
     * @param input 传入的测试
     * @return 加密后的字符串
     * @throws Exception 异常
     */
    String encrypt(String input) throws Exception;

    /**
     * 解密
     *
     * @param input 要解密的密文
     * @return 解密后的明文
     * @throws Exception 异常
     */
    String decrypt(String input) throws Exception;

}
