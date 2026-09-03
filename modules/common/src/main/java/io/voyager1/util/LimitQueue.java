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

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 定长队列
 *
 * @since 2019/3/16
 */
public class LimitQueue<E> extends ConcurrentLinkedDeque<E> {
    private final int limit;

    public LimitQueue(int limit) {
        this.limit = Math.max(limit, 0);
    }

    @Override
    public boolean offerFirst(E s) {
        if (full()) {
            // 删除最后一个
            pollLast();
        }
        return super.offerFirst(s);
    }

    @Override
    public boolean offerLast(E s) {
        if (full()) {
            // 删除第一个
            pollFirst();
        }
        return super.offerLast(s);
    }

    public boolean full() {
        return size() > limit;
    }
}
