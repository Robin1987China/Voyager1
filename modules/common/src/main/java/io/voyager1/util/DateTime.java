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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期时间，"" {@code io.voyager1.util.DateTime}。
 */
public class DateTime extends Date {

    public DateTime() {
        super();
    }

    public DateTime(long timeMillis) {
        super(timeMillis);
    }

    public DateTime(Date date) {
        super(date.getTime());
    }

    public static DateTime now() {
        return new DateTime();
    }

    public static DateTime of(long timeMillis) {
        return new DateTime(timeMillis);
    }

    public static DateTime of(Date date) {
        return new DateTime(date);
    }

    public long getTime() {
        return super.getTime();
    }

    public String toString(String pattern) {
        return DateUtil.format(this, pattern);
    }

    public String toDateStr() {
        return DateUtil.format(this, "yyyy-MM-dd");
    }

    public String toStringDefaultTimeZone() {
        return DateUtil.format(this, "yyyy-MM-dd HH:mm:ss");
    }

    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault());
    }

    public DateTime setTimeZone(java.util.TimeZone timeZone) {
        return this;
    }

    public DateTime offset(DateField dateField, int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this);
        calendar.add(dateField.getValue(), offset);
        return new DateTime(calendar.getTime());
    }

    public DateTime offsetNew(DateField dateField, int offset) {
        return offset(dateField, offset);
    }

    public Week dayOfWeekEnum() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this);
        int iso = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1;
        return Week.of(iso);
    }
}
