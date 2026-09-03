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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 收集器工具，"" {@code io.voyager1.util.CollectorUtil}。
 */
public class CollectorUtil {

    /**
     * 分组收集。
     *
     * @param classifier 分类函数
     * @param downstream 下游收集器
     * @param <T>        输入类型
     * @param <K>        键类型
     * @param <A>        下游中间累加类型
     * @param <D>        下游结果类型
     * @return 分组收集器
     */
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(
            Function<? super T, ? extends K> classifier,
            Collector<? super T, A, D> downstream) {
        return Collectors.groupingBy(classifier, downstream);
    }
}
