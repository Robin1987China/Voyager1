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

package io.voyager1.plugin;

import java.lang.annotation.*;

/**
 * 插件配置 相关属性注解
 *
 * @since 2021/12/24
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginConfig {

    /**
     * 是否为原生对象，原生对象将使用 默认构造方法创建单利对象
     *
     * @return 默认 原生对象
     */
    boolean nativeObject() default true;

    /**
     * 插件名、该字段优先级高于 plugin
     *
     * @return 插件名
     */
    String name();
}
