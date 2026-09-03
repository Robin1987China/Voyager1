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

package io.voyager1.common.commander;


import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @since 2023/4/6
 */
public class CacheObject<T> {

    private final T value;

    private final Long enterTime;

    public CacheObject(T value) {
        this.value = value;
        this.enterTime = System.currentTimeMillis();
    }

    private boolean isExpired() {
        return (System.currentTimeMillis() - this.enterTime > TimeUnit.MINUTES.toMillis(10));
    }

    /**
     * 添加到缓存对象中
     *
     * @param map   map
     * @param key   缓存的 key
     * @param value 缓存的 value
     * @param <K>   缓存的 key
     * @param <V>   缓存的 value
     */
    public static <K, V> void put(Map<K, CacheObject<V>> map, K key, V value) {
        map.put(key, new CacheObject<>(value));
        int size = map.size();
        if (size > 100) {
            // 清空过期的数据
            Iterator<Map.Entry<K, CacheObject<V>>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, CacheObject<V>> next = iterator.next();
                CacheObject<V> nextValue = next.getValue();
                if (nextValue.isExpired()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 获取缓存中的值
     *
     * @param map 缓存 map
     * @param key 缓存的 key
     * @param <K> 缓存的 key
     * @return value
     */
    public static <K, V> V get(Map<K, CacheObject<V>> map, K key) {
        CacheObject<V> cacheObject = map.get(key);
        if (cacheObject == null || cacheObject.isExpired()) {
            map.remove(key);
            return null;
        }
        return cacheObject.value;
    }
}
