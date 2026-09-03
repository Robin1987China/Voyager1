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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Map;

/**
 * Bean 工具 {@code io.voyager1.util.BeanUtil} 的常用方法。
 */
public class BeanUtil {

    /**
     * 读取属性，支持点分隔的嵌套路径与 Map 键。
     *
     * @param bean       对象（Bean 或 Map）
     * @param expression 属性表达式，如 {@code a.b.c}
     * @param <T>        返回值类型
     * @return 属性值，不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(Object bean, String expression) {
        if (bean == null || expression == null || expression.isEmpty()) {
            return null;
        }
        Object current = bean;
        for (String part : expression.split("\\.")) {
            current = readProperty(current, part);
            if (current == null) {
                return null;
            }
        }
        return (T) current;
    }

    /**
     * 设置属性，支持点分隔的嵌套路径与 Map 键。
     *
     * @param bean       对象（Bean 或 Map）
     * @param expression 属性表达式
     * @param value      值
     */
    public static void setProperty(Object bean, String expression, Object value) {
        if (bean == null || expression == null || expression.isEmpty()) {
            return;
        }
        String[] parts = expression.split("\\.");
        Object current = bean;
        for (int i = 0; i < parts.length - 1; i++) {
            current = readProperty(current, parts[i]);
            if (current == null) {
                return;
            }
        }
        writeProperty(current, parts[parts.length - 1], value);
    }

    /**
     * 将 Map 转换为 Bean。
     *
     * @param map       Map
     * @param beanClass 目标类型
     * @param <T>       目标类型
     * @return Bean
     */
    public static <T> T toBean(Map<?, ?> map, Class<T> beanClass) {
        return toBean(map, beanClass, CopyOptions.create());
    }

    /**
     * 将 Map 转换为 Bean。
     *
     * @param map       Map
     * @param beanClass 目标类型
     * @param options   拷贝选项
     * @param <T>       目标类型
     * @return Bean
     */
    public static <T> T toBean(Map<?, ?> map, Class<T> beanClass, CopyOptions options) {
        if (map == null || beanClass == null) {
            return null;
        }
        CopyOptions opts = options == null ? CopyOptions.create() : options;
        T bean = BeanUtils.instantiateClass(beanClass);
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String name = String.valueOf(entry.getKey());
            if (opts.getIgnoreProperties() != null && opts.getIgnoreProperties().contains(name)) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null && opts.isIgnoreNullValue()) {
                continue;
            }
            if (opts.getFieldNameEditor() != null) {
                name = opts.getFieldNameEditor().apply(name);
            }
            try {
                wrapper.setPropertyValue(name, value);
            } catch (Exception e) {
                if (!opts.isIgnoreError()) {
                    throw new UtilException(e);
                }
            }
        }
        return bean;
    }

    /**
     * 拷贝属性。
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        copyProperties(source, target, CopyOptions.create());
    }

    /**
     * 拷贝属性。
     *
     * @param source  源对象
     * @param target  目标对象
     * @param options 拷贝选项
     */
    public static void copyProperties(Object source, Object target, CopyOptions options) {
        if (source == null || target == null) {
            return;
        }
        CopyOptions opts = options == null ? CopyOptions.create() : options;
        BeanWrapperImpl sourceWrapper = new BeanWrapperImpl(source);
        BeanWrapperImpl targetWrapper = new BeanWrapperImpl(target);
        for (PropertyDescriptor pd : sourceWrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name) || pd.getReadMethod() == null) {
                continue;
            }
            if (opts.getIgnoreProperties() != null && opts.getIgnoreProperties().contains(name)) {
                continue;
            }
            Object value;
            try {
                value = sourceWrapper.getPropertyValue(name);
            } catch (Exception e) {
                if (!opts.isIgnoreError()) {
                    throw new UtilException(e);
                }
                continue;
            }
            if (value == null && opts.isIgnoreNullValue()) {
                continue;
            }
            String targetName = opts.getFieldNameEditor() != null ? opts.getFieldNameEditor().apply(name) : name;
            try {
                targetWrapper.setPropertyValue(targetName, value);
            } catch (Exception e) {
                if (!opts.isIgnoreError()) {
                    throw new UtilException(e);
                }
            }
        }
    }

    private static Object readProperty(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).get(name);
        }
        try {
            return new BeanWrapperImpl(obj).getPropertyValue(name);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeProperty(Object obj, String name, Object value) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Map) {
            ((Map) obj).put(name, value);
            return;
        }
        try {
            new BeanWrapperImpl(obj).setPropertyValue(name, value);
        } catch (Exception ignored) {
            // 忽略不可写属性
        }
    }
}
