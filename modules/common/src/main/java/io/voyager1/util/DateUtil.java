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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日期工具 {@code io.voyager1.util.DateUtil}。
 */
public class DateUtil {

    public static DateTime parse(CharSequence dateStr, String pattern) {
        Date d = parseDate(dateStr, pattern);
        return d == null ? null : new DateTime(d);
    }

    private static Date parseDate(CharSequence dateStr, String pattern) {
        if (dateStr == null || dateStr.length() == 0) {
            return null;
        }
        try {
            return new SimpleDateFormat(pattern).parse(dateStr.toString());
        } catch (Exception e) {
            throw new RuntimeException("日期解析失败: " + dateStr, e);
        }
    }

    public static DateTime parse(CharSequence dateStr) {
        return parse(dateStr, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date parseDate(CharSequence dateStr) {
        return parse(dateStr, "yyyy-MM-dd HH:mm:ss");
    }

    public static DateTime now() {
        return new DateTime();
    }

    public static DateTime date() {
        return new DateTime();
    }

    public static DateTime date(Date date) {
        return date == null ? null : new DateTime(date);
    }

    public static DateTime date(long timeMillis) {
        return new DateTime(timeMillis);
    }

    public static DateTime beginOfDay(Date date) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate().atStartOfDay();
        return new DateTime(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }

    public static DateTime endOfDay(Date date) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate().atTime(23, 59, 59);
        return new DateTime(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }

    public static DateTime offsetDay(Date date, int offset) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).plusDays(offset);
        return new DateTime(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }

    public static DateTime parseTimeToday(String timeStr) {
        LocalDateTime ldt = LocalDateTime.now().toLocalDate().atTime(java.time.LocalTime.parse(timeStr));
        return new DateTime(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }

    public static long currentSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static boolean isIn(Date date, Date beginDate, Date endDate) {
        return date != null && beginDate != null && endDate != null
            && !date.before(beginDate) && !date.after(endDate);
    }

    public static DateTime parseUTC(CharSequence dateStr) {
        String s = dateStr.toString();
        java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s);
        return new DateTime(Date.from(odt.toInstant()));
    }

    public static String format(Date date, String pattern) {
        return date == null ? null : new SimpleDateFormat(pattern).format(date);
    }

    public static long between(Date beginDate, Date endDate, DateUnit unit) {
        if (beginDate == null || endDate == null) {
            return 0;
        }
        return (endDate.getTime() - beginDate.getTime()) / unit.getMillis();
    }

    public static long betweenDay(Date beginDate, Date endDate, boolean isReset) {
        long diff = endDate.getTime() - beginDate.getTime();
        return diff / 86400000;
    }
}
