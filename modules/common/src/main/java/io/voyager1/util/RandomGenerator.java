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

import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机字符验证码生成器{@code io.voyager1.util.RandomGenerator}。
 */
public class RandomGenerator implements CodeGenerator {

    /** 基础字符集合 */
    private final String baseStr;
    /** 验证码长度 */
    private final int length;

    /**
     * 构造，使用字母+数字作为基础字符集合。
     *
     * @param count 生成验证码长度
     */
    public RandomGenerator(int count) {
        this("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", count);
    }

    /**
     * 构造。
     *
     * @param baseStr 基础字符集合
     * @param length  生成验证码长度
     */
    public RandomGenerator(String baseStr, int length) {
        this.baseStr = baseStr;
        this.length = length;
    }

    @Override
    public String generate() {
        if (baseStr == null || baseStr.isEmpty() || length <= 0) {
            return "";
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(baseStr.charAt(random.nextInt(baseStr.length())));
        }
        return sb.toString();
    }

    @Override
    public boolean verify(String code, String userInputCode) {
        if (userInputCode == null || userInputCode.trim().isEmpty()) {
            return false;
        }
        return code != null && code.equalsIgnoreCase(userInputCode);
    }

    public int getLength() {
        return length;
    }
}
