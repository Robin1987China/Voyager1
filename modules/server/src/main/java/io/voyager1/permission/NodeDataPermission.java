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
 * @since 2021/12/23
 */

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeDataPermission {

    /**
     * 参数名
     *
     * @return 默认ID
     */
    String parameterName() default "id";

    /**
     * 数据 class
     *
     * @return cls
     */
    Class<? extends io.voyager1.core.jpa.DataService<?>> cls();
}
