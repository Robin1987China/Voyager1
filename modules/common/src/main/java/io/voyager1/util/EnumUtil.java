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
 * 枚举工具，"" {@code io.voyager1.util.EnumUtil} 的常用方法。
 */
public class EnumUtil {

    public static <E extends Enum<E>> E fromString(Class<E> clazz, String value) {
        if (value == null) {
            return null;
        }
        for (E e : clazz.getEnumConstants()) {
            if (e.name().equals(value)) {
                return e;
            }
        }
        return null;
    }

    public static <E extends Enum<E>> E fromStringQuietly(Class<E> clazz, String value) {
        return fromString(clazz, value);
    }

    public static <E extends Enum<E>> E likeValueOf(Class<E> clazz, Object value) {
        return value == null ? null : likeValueOf(clazz, value.toString());
    }

    public static <E extends Enum<E>> E likeValueOf(Class<E> clazz, String value) {
        if (value == null) {
            return null;
        }
        for (E e : clazz.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }

    public static <E extends Enum<E>> E fromString(Class<E> clazz, String value, E defaultValue) {
        E result = fromString(clazz, value);
        return result == null ? defaultValue : result;
    }
}
