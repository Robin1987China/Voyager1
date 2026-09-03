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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 数组工具，"" {@code io.voyager1.util.ArrayUtil} 的常用方法。
 */
public class ArrayUtil {

    public static final int INDEX_NOT_FOUND = -1;
    public static final int INDEX = INDEX_NOT_FOUND;

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    public static int length(Object array) {
        return array == null ? 0 : Array.getLength(array);
    }

    public static boolean contains(Object[] array, Object value) {
        return array != null && Arrays.asList(array).contains(value);
    }

    public static boolean containsIgnoreCase(String[] array, String value) {
        if (array == null) {
            return false;
        }
        for (String s : array) {
            if (s != null && s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static int indexOf(Object[] array, Object value) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        for (int i = 0; i < array.length; i++) {
            if (java.util.Objects.equals(array[i], value)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static Integer get(int[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Object[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return (T) array[index];
    }

    public static String join(Object[] array, CharSequence separator) {
        if (array == null) {
            return null;
        }
        return Arrays.stream(array).map(String::valueOf).collect(Collectors.joining(separator));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] wrap(Object obj) {
        if (obj == null) {
            return (T[]) new Object[0];
        }
        if (obj.getClass().isArray()) {
            return (T[]) obj;
        }
        Object[] arr = (Object[]) Array.newInstance(obj.getClass(), 1);
        arr[0] = obj;
        return (T[]) arr;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] append(T[] array, T... newElements) {
        if (array == null) {
            return newElements;
        }
        T[] result = Arrays.copyOf(array, array.length + newElements.length);
        System.arraycopy(newElements, 0, result, array.length, newElements.length);
        return result;
    }

    public static <T> T firstMatch(java.util.function.Predicate<T> predicate, T... array) {
        if (array == null) {
            return null;
        }
        for (T t : array) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }
}
