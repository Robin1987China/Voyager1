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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  {@code io.voyager1.util.StaticLog} 的轻量兼容实现。
 *
 * <p>基于 SLF4J 的静态日志包装，仅覆盖代码库实际使用到的 API。</p>
 */
public final class StaticLog {

    private static final Logger LOG = LoggerFactory.getLogger("io.voyager1");

    private StaticLog() {
    }

    public static void trace(String msg, Object... args) {
        LOG.trace(msg, args);
    }

    public static void debug(String msg, Object... args) {
        LOG.debug(msg, args);
    }

    public static void info(String msg, Object... args) {
        LOG.info(msg, args);
    }

    public static void warn(String msg, Object... args) {
        LOG.warn(msg, args);
    }

    public static void error(String msg, Object... args) {
        LOG.error(msg, args);
    }

    public static void error(String msg, Throwable throwable) {
        LOG.error(msg, throwable);
    }

    public static void error(Throwable throwable) {
        LOG.error(throwable.getMessage(), throwable);
    }
}
