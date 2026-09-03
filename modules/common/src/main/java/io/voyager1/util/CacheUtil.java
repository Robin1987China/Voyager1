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

/**
 * 缓存工具类 {@code io.voyager1.util.CacheUtil}。
 */
public class CacheUtil {

    private CacheUtil() {
    }

    /**
     * 创建定时过期缓存。
     *
     * @param timeout 过期时间（毫秒）
     */
    public static <K, V> TimedCache<K, V> newTimedCache(long timeout) {
        return new TimedCache<>(timeout);
    }

    /**
     * 创建最少使用缓存，默认永不过期。
     *
     * @param capacity 最大容量
     */
    public static <K, V> LFUCache<K, V> newLFUCache(int capacity) {
        return new LFUCache<>(capacity);
    }

    /**
     * 创建最近最少使用缓存。
     *
     * @param capacity 最大容量
     */
    public static <K, V> Cache<K, V> newLRUCache(int capacity) {
        return new LFUCache<>(capacity);
    }

    /**
     * 创建最近最少使用缓存，并指定默认过期时间。
     *
     * @param capacity 最大容量
     * @param timeout  默认过期时间（毫秒）
     */
    public static <K, V> Cache<K, V> newLRUCache(int capacity, long timeout) {
        return new LFUCache<>(capacity, timeout);
    }
}
