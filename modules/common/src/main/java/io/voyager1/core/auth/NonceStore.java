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

package io.voyager1.core.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次性 nonce 记录（LRU），用于防重放。
 * <p>
 * 每个已接受的 nonce 会被记住；重复出现的 nonce 直接判为非法。容量满后按访问时间淘汰最久未用项，
 * 由于令牌本身带 TTL（默认短时），LRU 容量足以覆盖令牌有效窗口。
 */
public final class NonceStore {

    private final int capacity;
    private final Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > capacity;
        }
    };

    public NonceStore() {
        this(1024);
    }

    public NonceStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正数");
        }
        this.capacity = capacity;
    }

    /**
     * 标记并消费一个 nonce。
     *
     * @return {@code true} 表示首次出现（已消费）；{@code false} 表示重复（重放）。
     */
    public synchronized boolean mark(String nonce) {
        if (nonce == null) {
            return false;
        }
        if (seen.containsKey(nonce)) {
            return false;
        }
        seen.put(nonce, Boolean.TRUE);
        return true;
    }
}
