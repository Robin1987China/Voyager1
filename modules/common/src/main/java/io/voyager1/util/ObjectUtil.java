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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 对象工具，"" {@code io.voyager1.util.ObjectUtil} 的常用方法。
 */
public class ObjectUtil {

    public static boolean equal(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static boolean equals(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    public static boolean isEmpty(Object obj) {
        return obj == null;
    }

    public static boolean isNotEmpty(Object obj) {
        return obj != null;
    }

    public static boolean isAllEmpty(Object... objs) {
        if (objs == null) {
            return true;
        }
        for (Object o : objs) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasEmpty(Object... objs) {
        if (objs == null) {
            return true;
        }
        for (Object o : objs) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    public static <T> T defaultIfNull(T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    public static <T> T defaultIfNull(T obj, Supplier<? extends T> supplier) {
        return obj != null ? obj : supplier.get();
    }

    public static String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Cloneable) {
            try {
                return (T) obj.getClass().getMethod("clone").invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException("克隆失败", e);
            }
        }
        return obj;
    }
}
