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

import java.util.Calendar;

/**
 * 日期字段枚举 {@code io.voyager1.util.DateField}。
 */
public enum DateField {

    ERA(Calendar.ERA),
    YEAR(Calendar.YEAR),
    MONTH(Calendar.MONTH),
    WEEK_OF_YEAR(Calendar.WEEK_OF_YEAR),
    WEEK_OF_MONTH(Calendar.WEEK_OF_MONTH),
    DAY_OF_MONTH(Calendar.DAY_OF_MONTH),
    DAY_OF_YEAR(Calendar.DAY_OF_YEAR),
    DAY_OF_WEEK(Calendar.DAY_OF_WEEK),
    DAY_OF_WEEK_IN_MONTH(Calendar.DAY_OF_WEEK_IN_MONTH),
    AM_PM(Calendar.AM_PM),
    HOUR(Calendar.HOUR),
    HOUR_OF_DAY(Calendar.HOUR_OF_DAY),
    MINUTE(Calendar.MINUTE),
    SECOND(Calendar.SECOND),
    MILLISECOND(Calendar.MILLISECOND);

    private final int calendarField;

    DateField(int calendarField) {
        this.calendarField = calendarField;
    }

    public int getValue() {
        return calendarField;
    }
}
