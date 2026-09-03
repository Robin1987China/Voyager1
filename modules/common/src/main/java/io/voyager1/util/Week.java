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

import java.time.DayOfWeek;

/**
 * 星期枚举， {@code io.voyager1.util.Week}。
 */
public enum Week {

    MONDAY("一", 1),
    TUESDAY("二", 2),
    WEDNESDAY("三", 3),
    THURSDAY("四", 4),
    FRIDAY("五", 5),
    SATURDAY("六", 6),
    SUNDAY("日", 7);

    private final String chinese;
    private final int iso8601Value;

    Week(String chinese, int iso8601Value) {
        this.chinese = chinese;
        this.iso8601Value = iso8601Value;
    }

    public int getIso8601Value() {
        return iso8601Value;
    }

    public String toChinese() {
        return toChinese("星期");
    }

    public String toChinese(String name) {
        return name + chinese;
    }

    public static Week of(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
        switch (dayOfWeek) {
            case MONDAY:
                return MONDAY;
            case TUESDAY:
                return TUESDAY;
            case WEDNESDAY:
                return WEDNESDAY;
            case THURSDAY:
                return THURSDAY;
            case FRIDAY:
                return FRIDAY;
            case SATURDAY:
                return SATURDAY;
            case SUNDAY:
            default:
                return SUNDAY;
        }
    }

    /**
     * 依据 ISO-8601 星期值（周一=1 … 周日=7）获取枚举。
     */
    public static Week of(int iso8601Value) {
        for (Week week : values()) {
            if (week.iso8601Value == iso8601Value) {
                return week;
            }
        }
        return null;
    }
}
