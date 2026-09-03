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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 类型转换工具 {@code io.voyager1.util.Convert} 的常用方法。
 */
public class ConvertUtil {

    public static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int toInt(Object value, int defaultValue) {
        Integer result = toInt(value);
        return result == null ? defaultValue : result;
    }

    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long toLong(Object value, long defaultValue) {
        Long result = toLong(value);
        return result == null ? defaultValue : result;
    }

    public static Boolean toBool(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s)) {
            return Boolean.TRUE;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "n".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public static boolean toBool(Object value, boolean defaultValue) {
        Boolean result = toBool(value);
        return result == null ? defaultValue : result;
    }

    public static String toStr(Object value) {
        return value == null ? null : value.toString();
    }

    public static String toStr(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    public static Float toFloat(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static double toDouble(Object value, double defaultValue) {
        Double result = toDouble(value);
        return result == null ? defaultValue : result;
    }

    public static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 通用类型转换
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Class<T> clazz, Object value) {
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        if (clazz == String.class) {
            return (T) toStr(value);
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) toInt(value);
        }
        if (clazz == Long.class || clazz == long.class) {
            return (T) toLong(value);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) toBool(value);
        }
        if (clazz == Double.class || clazz == double.class) {
            return (T) toDouble(value);
        }
        if (value instanceof String) {
            return com.alibaba.fastjson2.JSON.parseObject((String) value, clazz);
        }
        return com.alibaba.fastjson2.JSON.parseObject(toStr(value), clazz);
    }

    /**
     * 按分隔符拆分并去除空项与前后空白
     */
    public static List<String> splitTrim(String str, String separator) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(str.split(Pattern.quote(separator)))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
