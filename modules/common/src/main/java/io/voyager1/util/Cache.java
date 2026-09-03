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

import java.util.Iterator;
import java.util.function.Supplier;

/**
 * 缓存接口 {@code io.voyager1.util.Cache}。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface Cache<K, V> extends Iterable<CacheObj<K, V>> {

    /**
     * 获取缓存值，不存在或已过期返回 {@code null}。
     */
    V get(K key);

    /**
     * 获取缓存值，不存在或已过期时使用 {@link Supplier} 生成并放入缓存。
     */
    default V get(K key, Supplier<V> supplier) {
        V value = get(key);
        if (value == null && supplier != null) {
            value = supplier.get();
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    /**
     * 放入缓存，使用缓存默认过期时间。
     */
    void put(K key, V object);

    /**
     * 放入缓存并指定过期时间（毫秒）。
     */
    void put(K key, V object, long timeout);

    /**
     * 移除缓存项。
     */
    void remove(K key);

    /**
     * 是否包含且未过期。
     */
    boolean containsKey(K key);

    /**
     * 缓存项数量。
     */
    int size();

    /**
     * 清空缓存。
     */
    void clear();

    /**
     * 获取缓存项迭代器。
     */
    Iterator<CacheObj<K, V>> cacheObjIterator();

    /**
     * 设置过期移除监听器。
     */
    void setListener(CacheListener<K, V> listener);

    /**
     * 安排定时清理任务。
     */
    void schedulePrune(long delay);
}
