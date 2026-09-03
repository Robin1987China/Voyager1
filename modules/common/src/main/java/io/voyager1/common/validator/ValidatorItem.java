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

import java.lang.annotation.*;

/**
 * 验证规则
 *
 * @since 2018/8/21
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidatorItem {
    /**
     * 规则
     *
     * @return ValidatorRule
     */
    ValidatorRule value() default ValidatorRule.NOT_EMPTY;

    /**
     * 还原html 转义字符
     * 一般用户字符串长度验证统一性
     *
     * @return 默认不还原
     */
    boolean unescape() default false;

    /**
     * 响应码
     *
     * @return 默认400
     */
    int code() default 400;

    /**
     * 错误信息
     *
     * @return msg
     */
    String msg() default "";

    /**
     * 数字类型的范围
     * 配置错误将不判断
     * <p>
     * 范围写反也将不判断
     * <p>
     * 逻辑判断符 是 &gt; 或者 &lt;
     * <p>
     * 当 ValidatorRule 为 CUSTOMIZE 时此参数无效
     * <p>
     * 1 则为长度必须为1
     * <p>
     * 1.2:2  double类型的范围，值为1.2~2
     *
     * <p>
     * 1.2:2.5[1] double类型的范围，值为1.2~2.5 且小数点只能有一位
     *
     * @return 具体的规则
     * @see ValidatorRule#DECIMAL
     * @see ValidatorRule#NUMBERS
     * @see ValidatorRule#POSITIVE_INTEGER
     * @see ValidatorRule#NON_ZERO_INTEGERS
     * @see ValidatorRule#NOT_EMPTY
     * @see ValidatorRule#NOT_BLANK
     * @see ValidatorRule#GENERAL
     */
    String range() default "";
}
