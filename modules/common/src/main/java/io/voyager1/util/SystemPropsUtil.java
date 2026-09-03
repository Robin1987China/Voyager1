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
 * 系统属性工具，"" {@code io.voyager1.util.SystemPropsUtil}。
 */
public class SystemPropsUtil {

    /**
     * 获取系统属性。
     *
     * @param key 键
     * @return 值，不存在返回 null
     */
    public static String get(String key) {
        return System.getProperty(key);
    }

    /**
     * 获取系统属性。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，不存在返回默认值
     */
    public static String get(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    /**
     * 获取 int 类型系统属性。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，不存在或解析失败返回默认值
     */
    public static int getInt(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取 long 类型系统属性。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，不存在或解析失败返回默认值
     */
    public static long getLong(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 设置系统属性。
     *
     * @param key   键
     * @param value 值
     */
    public static void set(String key, String value) {
        System.setProperty(key, value);
    }
}
