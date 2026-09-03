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

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

/**
 * 函数比较器，"" {@code io.voyager1.util.FuncComparator}。
 *
 * @param <T> 比较对象类型
 */
public class FuncComparator<T> implements Comparator<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Function<T, ? extends Comparable<?>> keyExtractor;
    private final boolean nullGreater;

    /**
     * 构造。
     *
     * @param nullGreater   是否 null 值更大
     * @param keyExtractor  比较键提取器
     */
    public FuncComparator(boolean nullGreater, Function<T, ? extends Comparable<?>> keyExtractor) {
        this.nullGreater = nullGreater;
        this.keyExtractor = keyExtractor;
    }

    @Override
    public int compare(T o1, T o2) {
        if (this.nullGreater) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
        } else {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
        }
        Comparable<?> v1 = keyExtractor.apply(o1);
        Comparable<?> v2 = keyExtractor.apply(o2);
        if (v1 == v2) {
            return 0;
        }
        if (v1 == null) {
            return nullGreater ? 1 : -1;
        }
        if (v2 == null) {
            return nullGreater ? -1 : 1;
        }
        return CompareUtil.compare(v1, v2);
    }
}
