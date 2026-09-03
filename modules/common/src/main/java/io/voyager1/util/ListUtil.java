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
import java.util.LinkedList;
import java.util.List;

/**
 * 列表工具，"" {@code io.voyager1.util.ListUtil}。
 */
public class ListUtil {

    /**
     * 新建 ArrayList。
     *
     * @param array 元素数组
     * @param <T>   元素类型
     * @return ArrayList
     */
    @SafeVarargs
    public static <T> List<T> of(T... array) {
        return toList(array);
    }

    /**
     * 新建 ArrayList。
     *
     * @param array 元素数组
     * @param <T>   元素类型
     * @return ArrayList
     */
    @SafeVarargs
    public static <T> List<T> toList(T... array) {
        List<T> list = new ArrayList<>(array == null ? 0 : array.length);
        if (array != null) {
            list.addAll(Arrays.asList(array));
        }
        return list;
    }

    /**
     * 新建 List。
     *
     * @param array 元素数组
     * @param <T>   元素类型
     * @return ArrayList
     */
    @SafeVarargs
    public static <T> List<T> list(T... array) {
        return toList(array);
    }

    /**
     * 新建 List。
     *
     * @param isLinked 是否 LinkedList
     * @param array    元素数组
     * @param <T>      元素类型
     * @return List
     */
    @SafeVarargs
    public static <T> List<T> list(boolean isLinked, T... array) {
        List<T> list = isLinked ? new LinkedList<>() : new ArrayList<>(array == null ? 0 : array.length);
        if (array != null) {
            list.addAll(Arrays.asList(array));
        }
        return list;
    }
}
