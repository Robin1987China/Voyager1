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

package io.voyager1.common.validator;

/**
 * 验证规则
 *
 * @since  2018/8/21.
 */
public enum ValidatorRule {
    /**
     * 不为空
     */
    NOT_EMPTY,
    /**
     * 空
     */
    EMPTY,
    /**
     * 不为空白
     */
    NOT_BLANK,
    /**
     * 手机号码
     */
    MOBILE,
    /**
     * 邮箱
     */
    EMAIL,
    /**
     * 英文字母 、数字和下划线
     */
    GENERAL,
    /**
     * url
     */
    URL,
    /**
     * 汉字
     */
    CHINESE,
    /**
     * 是否是字母（包括大写和小写字母）
     */
    WORD,
    /**
     * 小数
     *
     * @see ParameterInterceptor#validatorNumber(io.voyager1.common.validator.ValidatorItem, String)
     */
    DECIMAL,
    /**
     * 数字
     *
     * @see ParameterInterceptor#validatorNumber(io.voyager1.common.validator.ValidatorItem, String)
     */
    NUMBERS,
    /**
     * 非零 正整数  最大长度7位
     */
    NON_ZERO_INTEGERS,
    /**
     * 正整数  包括0  最大长度7位
     */
    POSITIVE_INTEGER,
    /**
     * 自定义验证
     */
    CUSTOMIZE
}
