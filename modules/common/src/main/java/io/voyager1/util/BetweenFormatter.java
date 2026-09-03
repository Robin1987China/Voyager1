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
 * 时间间隔格式化 {@code io.voyager1.util.BetweenFormatter}。
 */
public class BetweenFormatter {

    public enum Level {
        MILLISECOND, SECOND, MINUTE, HOUR, DAY
    }

    private long betweenMs;
    private Level level;
    private int levelMaxCount;
    private String separator = "";
    private java.util.function.Function<Level, String> levelFormatter;

    public BetweenFormatter(long betweenMs, Level level) {
        this.betweenMs = betweenMs;
        this.level = level;
        this.levelMaxCount = 0;
    }

    public BetweenFormatter(long betweenMs, Level level, int levelMaxCount) {
        this.betweenMs = betweenMs;
        this.level = level;
        this.levelMaxCount = levelMaxCount;
    }

    public BetweenFormatter setSeparator(String separator) {
        this.separator = separator == null ? "" : separator;
        return this;
    }

    public BetweenFormatter setLevelFormatter(java.util.function.Function<Level, String> levelFormatter) {
        this.levelFormatter = levelFormatter;
        return this;
    }

    public String format() {
        switch (level) {
            case MILLISECOND:
                return betweenMs + "ms";
            case SECOND:
                return betweenMs / 1000 + "s";
            case MINUTE:
                return betweenMs / 60000 + "min";
            case HOUR:
                return betweenMs / 3600000 + "h";
            default:
                return betweenMs / 86400000 + "d";
        }
    }

    @Override
    public String toString() {
        return format();
    }
}
