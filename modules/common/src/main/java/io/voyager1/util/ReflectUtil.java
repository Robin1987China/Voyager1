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
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 反射工具 {@code io.voyager1.util.ReflectUtil}。
 */
public class ReflectUtil {

    public static Object getFieldValue(Object obj, Field field) {
        if (obj == null || field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, obj);
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        Field field = ReflectionUtils.findField(obj.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, obj);
    }

    public static void setFieldValue(Object obj, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(obj.getClass(), fieldName);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, obj, value);
        }
    }

    public static void setFieldValue(Object obj, Field field, Object value) {
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, obj, value);
        }
    }

    public static <T> T newInstance(Class<T> clazz) {
        return BeanUtils.instantiateClass(clazz);
    }

    public static <T> T newInstanceIfPossible(Class<T> clazz) {
        try {
            return BeanUtils.instantiateClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        return ReflectionUtils.findField(clazz, fieldName);
    }

    public static Object getStaticFieldValue(Field field) {
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, null);
    }

    public static String getFieldName(Field field) {
        return field == null ? null : field.getName();
    }

    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        Map<String, Field> map = new LinkedHashMap<>();
        for (Field field : getFields(clazz)) {
            map.put(field.getName(), field);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object obj, Method method, Object... args) {
        if (method == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(method);
        return (T) ReflectionUtils.invokeMethod(method, obj, args);
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object obj, String methodName, Object... args) {
        Method method = getMethodByName(obj.getClass(), methodName);
        if (method == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(method);
        return (T) ReflectionUtils.invokeMethod(method, obj, args);
    }

    public static Field[] getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            java.util.Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    public static Field[] getFields(Class<?> clazz, Predicate<Field> filter) {
        return java.util.Arrays.stream(getFields(clazz)).filter(filter).toArray(Field[]::new);
    }

    public static boolean hasField(Class<?> clazz, String fieldName) {
        return ReflectionUtils.findField(clazz, fieldName) != null;
    }

    public static Method getMethodByName(Class<?> clazz, String methodName) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        return ReflectionUtils.findMethod(clazz, methodName, paramTypes);
    }
}
