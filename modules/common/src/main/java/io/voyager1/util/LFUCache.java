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

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 最少使用缓存，"" {@code io.voyager1.util.LFUCache}。
 */
public class LFUCache<K, V> extends AbstractCache<K, V> {

    private final int capacity;
    private final long timeout;

    /**
     * 构造，默认永不过期。
     *
     * @param capacity 最大容量
     */
    public LFUCache(int capacity) {
        this(capacity, 0);
    }

    /**
     * 构造。
     *
     * @param capacity 最大容量
     * @param timeout  默认过期时间（毫秒），0 或负数表示永不过期
     */
    public LFUCache(int capacity, long timeout) {
        this.capacity = Math.max(1, capacity);
        this.timeout = timeout;
        this.raw = finish(Caffeine.newBuilder().maximumSize(this.capacity));
    }

    @Override
    protected long defaultTimeout() {
        return timeout;
    }
}
