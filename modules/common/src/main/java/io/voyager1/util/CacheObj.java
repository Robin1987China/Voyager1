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
 * 缓存对象包装 {@code io.voyager1.util.CacheObj}。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class CacheObj<K, V> {

    private final K key;
    private final V value;
    /** 过期时间（绝对毫秒时间戳），-1 表示永不过期 */
    private final long expireAt;
    /** 存活时长（毫秒），-1 表示永不过期 */
    private final long ttlMillis;

    public CacheObj(K key, V value, long ttlMillis) {
        this.key = key;
        this.value = value;
        this.ttlMillis = ttlMillis;
        this.expireAt = ttlMillis <= 0 ? -1L : System.currentTimeMillis() + ttlMillis;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public boolean isExpired() {
        return expireAt >= 0 && System.currentTimeMillis() > expireAt;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }
}
