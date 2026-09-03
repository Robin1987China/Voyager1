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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Map 工具 {@code io.voyager1.util.MapUtil} 的常用方法。
 */
public class MapUtil {

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    public static <K, V> HashMap<K, V> newHashMap(int size) {
        return new HashMap<>(size);
    }

    public static <K, V> V get(Map<K, V> map, K key) {
        return map == null ? null : map.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Map<?, ?> map, Object key, Class<T> valueType) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (valueType.isInstance(v)) {
            return (T) v;
        }
        return ConvertUtil.convert(valueType, v);
    }

    @SafeVarargs
    public static <K, V> Map<K, V> removeAny(Map<K, V> map, K... keys) {
        if (map == null) {
            return null;
        }
        for (K key : keys) {
            map.remove(key);
        }
        return map;
    }

    public static <K extends Comparable<? super K>, V> TreeMap<K, V> sort(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        TreeMap<K, V> result = new TreeMap<>();
        map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    public static String join(Map<?, ?> map, CharSequence separator, CharSequence keyValueSeparator) {
        if (map == null) {
            return null;
        }
        return map.entrySet().stream()
            .map(e -> String.valueOf(e.getKey()) + keyValueSeparator + String.valueOf(e.getValue()))
            .collect(Collectors.joining(separator));
    }

    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
