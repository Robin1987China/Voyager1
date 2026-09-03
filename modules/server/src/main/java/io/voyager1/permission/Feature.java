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

package io.voyager1.permission;

import java.lang.annotation.*;

/**
 * 功能
 *
 * @since 2019/8/13
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Feature {

    /**
     * 类
     *
     * @return ClassFeature
     */
    ClassFeature cls() default ClassFeature.NULL;

    /**
     * 方法
     *
     * @return MethodFeature
     */
    MethodFeature method() default MethodFeature.NULL;

    /**
     * 是否记录响应 日志
     *
     * @return 默认 记录
     */
    boolean logResponse() default true;

    /**
     * 是否记录操作日志
     *
     * @return false 不记录操作日志
     */
    boolean log() default true;
}
