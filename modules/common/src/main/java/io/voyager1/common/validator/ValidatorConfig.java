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

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 字段验证配置
 *
 * @since 2018/8/21.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidatorConfig {
    /**
     * 需要验证的规则
     *
     * @return ValidatorItem
     */
    ValidatorItem[] value() default
        {
            @ValidatorItem(value = ValidatorRule.NOT_EMPTY)
        };

    /**
     * 自动参数值
     *
     * @return url 参数
     */
    String name() default "";

    /**
     * 默认值
     *
     * @return 默认
     */
    String defaultVal() default ValueConstants.DEFAULT_NONE;

    /**
     * 自定义验证 Controller 中方法名
     * <p>
     * public boolean customizeValidator(MethodParameter methodParameter, String value)
     *
     * @return 默认 customizeValidator
     */
    String customizeMethod() default "customizeValidator";

    /**
     * 判断参数为空 是字符串空
     * 如果为false
     *
     * @return 默认true
     */
    boolean strEmpty() default true;

    /**
     * 错误条件
     * <p>
     * or  一项正确返回正确，所有错误抛出错误
     * <p>
     * and 一项错误 抛出错误并结束整个判断
     *
     * @return 默认or
     */
    ErrorCondition errorCondition() default ErrorCondition.AND;
}
