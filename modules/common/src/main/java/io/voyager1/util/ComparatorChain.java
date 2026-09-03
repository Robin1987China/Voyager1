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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 比较器链 {@code io.voyager1.util.ComparatorChain}。
 *
 * @param <E> 比较对象类型
 */
public class ComparatorChain<E> implements Comparator<E>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Comparator<E>> comparators;

    public ComparatorChain() {
        this(new ArrayList<>());
    }

    @SafeVarargs
    public ComparatorChain(Comparator<E>... comparators) {
        this(Arrays.asList(comparators));
    }

    public ComparatorChain(List<Comparator<E>> comparators) {
        this.comparators = comparators == null ? new ArrayList<>() : comparators;
    }

    /**
     * 创建比较器链。
     *
     * @param comparators 比较器数组
     * @param <E>         比较对象类型
     * @return 比较器链
     */
    @SafeVarargs
    public static <E> ComparatorChain<E> of(Comparator<E>... comparators) {
        return new ComparatorChain<>(comparators);
    }

    /**
     * 追加比较器。
     *
     * @param comparator 比较器
     * @return this
     */
    public ComparatorChain<E> addComparator(Comparator<E> comparator) {
        this.comparators.add(comparator);
        return this;
    }

    @Override
    public int compare(E o1, E o2) {
        for (Comparator<E> comparator : this.comparators) {
            int result = comparator.compare(o1, o2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
