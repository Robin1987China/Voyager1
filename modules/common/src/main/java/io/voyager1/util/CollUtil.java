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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 集合工具 {@code io.voyager1.util.CollUtil} 的常用方法。
 */
public class CollUtil {

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return coll != null && !coll.isEmpty();
    }

    public static int size(Collection<?> coll) {
        return coll == null ? 0 : coll.size();
    }

    public static <T> T get(Collection<T> coll, int index) {
        if (coll == null || index < 0 || index >= coll.size()) {
            return null;
        }
        return new ArrayList<>(coll).get(index);
    }

    public static <T> T getFirst(Collection<T> coll) {
        return coll == null || coll.isEmpty() ? null : coll.iterator().next();
    }

    public static <T> T getLast(Collection<T> coll) {
        if (coll == null || coll.isEmpty()) {
            return null;
        }
        List<T> list = new ArrayList<>(coll);
        return list.get(list.size() - 1);
    }

    public static boolean contains(Collection<?> coll, Object value) {
        return coll != null && coll.contains(value);
    }

    public static boolean containsAny(Collection<?> coll, Object... values) {
        if (coll == null) {
            return false;
        }
        for (Object v : values) {
            if (coll.contains(v)) {
                return true;
            }
        }
        return false;
    }

    public static String join(Collection<?> coll, CharSequence separator) {
        if (coll == null) {
            return null;
        }
        return coll.stream().map(String::valueOf).collect(Collectors.joining(separator));
    }

    public static <T> List<T> newArrayList() {
        return new ArrayList<>();
    }

    @SafeVarargs
    public static <T> List<T> newArrayList(T... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    @SafeVarargs
    public static <T> Set<T> newHashSet(T... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    public static <T> List<T> subtract(Collection<T> coll1, Collection<T> coll2) {
        if (coll1 == null) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(coll1);
        if (coll2 != null) {
            result.removeAll(coll2);
        }
        return result;
    }

    public static <T extends Comparable<? super T>> List<T> sort(Collection<T> coll) {
        List<T> list = coll == null ? new ArrayList<>() : new ArrayList<>(coll);
        list.sort(null);
        return list;
    }

    public static <T> List<T> sort(Collection<T> coll, java.util.Comparator<? super T> comparator) {
        List<T> list = coll == null ? new ArrayList<>() : new ArrayList<>(coll);
        list.sort(comparator);
        return list;
    }

    public static <T> List<T> filter(Collection<T> coll, java.util.function.Predicate<T> predicate) {
        if (coll == null) {
            return new ArrayList<>();
        }
        return coll.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T> boolean addAll(Collection<T> coll, Collection<? extends T> other) {
        return coll != null && other != null && coll.addAll(other);
    }
}
