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

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * 忽略键大小写的 Map {@code io.voyager1.util.CaseInsensitiveMap}。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class CaseInsensitiveMap<K, V> extends TreeMap<K, V> {

    private static final long serialVersionUID = 1L;

    private static final Comparator<Object> CASE_INSENSITIVE_ORDER = (o1, o2) -> {
        if (o1 instanceof String && o2 instanceof String) {
            return ((String) o1).compareToIgnoreCase((String) o2);
        }
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        return String.valueOf(o1).compareToIgnoreCase(String.valueOf(o2));
    };

    public CaseInsensitiveMap() {
        super(CASE_INSENSITIVE_ORDER);
    }

    @SuppressWarnings("unused")
    public CaseInsensitiveMap(int initialCapacity) {
        super(CASE_INSENSITIVE_ORDER);
    }

    public CaseInsensitiveMap(Map<? extends K, ? extends V> map) {
        super(CASE_INSENSITIVE_ORDER);
        putAll(map);
    }
}
