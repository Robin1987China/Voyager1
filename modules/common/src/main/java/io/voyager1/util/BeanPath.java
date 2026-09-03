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
 * Bean 属性路径 {@code io.voyager1.util.BeanPath}。
 */
public class BeanPath {

    private final String expression;

    private BeanPath(String expression) {
        this.expression = expression;
    }

    /**
     * 创建 BeanPath。
     *
     * @param expression 属性表达式，如 {@code a.b.c}
     * @return BeanPath
     */
    public static BeanPath create(String expression) {
        return new BeanPath(expression);
    }

    /**
     * 读取属性值。
     *
     * @param bean 对象
     * @return 属性值
     */
    public Object get(Object bean) {
        return BeanUtil.getProperty(bean, expression);
    }

    /**
     * 设置属性值。
     *
     * @param bean  对象
     * @param value 值
     */
    public void set(Object bean, Object value) {
        BeanUtil.setProperty(bean, expression, value);
    }
}
