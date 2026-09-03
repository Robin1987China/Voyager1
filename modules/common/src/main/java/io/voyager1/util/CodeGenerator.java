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

/**
 * 验证码生成器接口，"" {@code io.voyager1.util.CodeGenerator}。
 */
public interface CodeGenerator {

    /**
     * 生成验证码。
     *
     * @return 验证码字符串
     */
    String generate();

    /**
     * 校验用户输入是否与生成的验证码匹配。
     *
     * @param code          生成的验证码
     * @param userInputCode 用户输入的验证码
     * @return 是否匹配
     */
    boolean verify(String code, String userInputCode);
}
