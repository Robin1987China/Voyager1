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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 工具，"" {@code io.voyager1.util.IdUtil} 的常用方法。
 */
public class IdUtil {

    public static String fastUUID() {
        return UUID.randomUUID().toString();
    }

    public static String fastSimpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String simpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static Snowflake getSnowflake() {
        return new Snowflake();
    }

    public static Snowflake getSnowflake(long workerId, long datacenterId) {
        return new Snowflake();
    }

    /**
     * 简化版雪花 ID 生成器
     */
    public static class Snowflake {
        private static final AtomicLong COUNTER = new AtomicLong(0);

        public long nextId() {
            long timestamp = System.currentTimeMillis();
            long seq = COUNTER.getAndIncrement() & 0xFFF;
            return (timestamp << 12) | seq;
        }

        public String nextIdStr() {
            return String.valueOf(nextId());
        }
    }
}
