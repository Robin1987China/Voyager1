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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

/**
 * Cron 表达式解析与匹配 的 {@code io.voyager1.util.CronPattern}。
 * 支持 5 段（分 时 日 月 周）与 6 段（秒 分 时 日 月 周）表达式，
 * 字段值支持 {@code *}、{@code ?}、{@code ,}、{@code -}、{@code /} 以及数字和名称（月/周）。
 */
public class CronPattern {

    private static final String[] MONTH_NAMES = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String[] WEEK_NAMES = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private final String pattern;
    private final boolean hasSecond;

    private final boolean[] seconds = new boolean[60];
    private final boolean[] minutes = new boolean[60];
    private final boolean[] hours = new boolean[24];
    private final boolean[] daysOfMonth = new boolean[32];
    private final boolean[] months = new boolean[13];
    private final boolean[] daysOfWeek = new boolean[8];

    public CronPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("cron 表达式为空");
        }
        this.pattern = pattern.trim();
        String[] fields = this.pattern.split("\\s+");
        if (fields.length == 5) {
            this.hasSecond = false;
            this.seconds[0] = true;
            parseField(fields[0], 0, 59, minutes, null);
            parseField(fields[1], 0, 23, hours, null);
            parseField(fields[2], 1, 31, daysOfMonth, null);
            parseField(fields[3], 1, 12, months, MONTH_NAMES);
            parseField(fields[4], 0, 7, daysOfWeek, WEEK_NAMES);
            normalizeWeek();
        } else if (fields.length == 6) {
            this.hasSecond = true;
            parseField(fields[0], 0, 59, seconds, null);
            parseField(fields[1], 0, 59, minutes, null);
            parseField(fields[2], 0, 23, hours, null);
            parseField(fields[3], 1, 31, daysOfMonth, null);
            parseField(fields[4], 1, 12, months, MONTH_NAMES);
            parseField(fields[5], 0, 7, daysOfWeek, WEEK_NAMES);
            normalizeWeek();
        } else {
            throw new IllegalArgumentException("cron 表达式必须为 5 段或 6 段: " + pattern);
        }
    }

    private void normalizeWeek() {
        if (daysOfWeek[7]) {
            // 7 与 0 都表示周日
            daysOfWeek[0] = true;
        }
    }

    private void parseField(String field, int min, int max, boolean[] target, String[] names) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("cron 字段为空");
        }
        String[] parts = field.split(",");
        for (String part : parts) {
            parsePart(part, min, max, target, names);
        }
    }

    private void parsePart(String part, int min, int max, boolean[] target, String[] names) {
        part = part.trim();
        if (part.equals("*") || part.equals("?")) {
            for (int i = min; i <= max; i++) {
                target[i] = true;
            }
            return;
        }
        int step = 1;
        String base = part;
        int slash = part.indexOf('/');
        if (slash >= 0) {
            base = part.substring(0, slash);
            try {
                step = Integer.parseInt(part.substring(slash + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("非法的 cron 步长: " + part);
            }
            if (step <= 0) {
                throw new IllegalArgumentException("非法的 cron 步长: " + part);
            }
        }
        int start;
        int end;
        int dash = base.indexOf('-');
        if (dash >= 0) {
            start = parseValue(base.substring(0, dash), min, max, names);
            end = parseValue(base.substring(dash + 1), min, max, names);
        } else if (base.equals("*") || base.equals("?") || base.isEmpty()) {
            start = min;
            end = max;
        } else if (slash >= 0) {
            // a/b 形式表示从 a 开始以步长 b 递增到字段最大值
            start = parseValue(base, min, max, names);
            end = max;
        } else {
            start = parseValue(base, min, max, names);
            end = start;
        }
        if (start > end) {
            throw new IllegalArgumentException("非法的 cron 范围: " + part);
        }
        for (int i = start; i <= end; i += step) {
            target[i] = true;
        }
    }

    private int parseValue(String value, int min, int max, String[] names) {
        String upper = value.trim().toUpperCase(Locale.ROOT);
        int result;
        try {
            result = Integer.parseInt(upper);
        } catch (NumberFormatException e) {
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(upper)) {
                        return min + i;
                    }
                }
            }
            throw new IllegalArgumentException("非法的 cron 字段值: " + value);
        }
        if (result < min || result > max) {
            throw new IllegalArgumentException("cron 字段值超出范围 [" + min + "," + max + "]: " + value);
        }
        return result;
    }

    /**
     * 判断指定时间是否匹配该表达式（含秒）
     *
     * @param date 时间
     * @return 是否匹配
     */
    public boolean match(Date date) {
        return match(date.getTime());
    }

    /**
     * 判断指定时间戳是否匹配该表达式（含秒）
     *
     * @param millis 时间戳
     * @return 是否匹配
     */
    public boolean match(long millis) {
        return match(millis, true);
    }

    /**
     * 判断指定时间戳是否匹配该表达式
     *
     * @param millis        时间戳
     * @param isMatchSecond 是否匹配秒
     * @return 是否匹配
     */
    public boolean match(long millis, boolean isMatchSecond) {
        LocalDateTime time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (!months[time.getMonthValue()]) {
            return false;
        }
        if (!matchDay(time.toLocalDate())) {
            return false;
        }
        if (!hours[time.getHour()]) {
            return false;
        }
        if (!minutes[time.getMinute()]) {
            return false;
        }
        if (isMatchSecond && hasSecond) {
            if (!seconds[time.getSecond()]) {
                return false;
            }
        } else if (time.getSecond() != 0) {
            return false;
        }
        return true;
    }

    /**
     * 计算指定时间之后的下一次匹配时间
     *
     * @param date          起始时间（不包含）
     * @param isMatchSecond 是否匹配秒
     * @return 下一次匹配时间，找不到返回 null
     */
    public Date nextMatchAfter(Date date, boolean isMatchSecond) {
        return nextMatchAfter(date.getTime(), isMatchSecond);
    }

    /**
     * 计算指定时间戳之后的下一次匹配时间
     *
     * @param millis        起始时间戳（不包含）
     * @param isMatchSecond 是否匹配秒
     * @return 下一次匹配时间，找不到返回 null
     */
    public Date nextMatchAfter(long millis, boolean isMatchSecond) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime base = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime().withNano(0);
        boolean matchSeconds = isMatchSecond && hasSecond;
        LocalDateTime candidate;
        if (matchSeconds) {
            candidate = base.plusSeconds(1);
        } else {
            candidate = base.plusMinutes(1).withSecond(0);
        }
        LocalDateTime result = findNext(candidate, matchSeconds);
        if (result == null) {
            return null;
        }
        return Date.from(result.atZone(zone).toInstant());
    }

    private LocalDateTime findNext(LocalDateTime start, boolean matchSeconds) {
        int startYear = start.getYear();
        for (int year = startYear; year <= startYear + 4; year++) {
            int monthStart = (year == startYear) ? start.getMonthValue() : 1;
            for (int month = monthStart; month <= 12; month++) {
                if (!months[month]) {
                    continue;
                }
                int dayStart = (year == startYear && month == start.getMonthValue()) ? start.getDayOfMonth() : 1;
                int maxDay = YearMonth.of(year, month).lengthOfMonth();
                for (int day = dayStart; day <= maxDay; day++) {
                    if (!matchDay(LocalDate.of(year, month, day))) {
                        continue;
                    }
                    boolean sameDay = year == startYear && month == start.getMonthValue() && day == start.getDayOfMonth();
                    int hourStart = sameDay ? start.getHour() : 0;
                    for (int hour = hourStart; hour <= 23; hour++) {
                        if (!hours[hour]) {
                            continue;
                        }
                        boolean sameHour = sameDay && hour == start.getHour();
                        int minuteStart = sameHour ? start.getMinute() : 0;
                        for (int minute = minuteStart; minute <= 59; minute++) {
                            if (!minutes[minute]) {
                                continue;
                            }
                            if (matchSeconds) {
                                boolean sameMinute = sameHour && minute == start.getMinute();
                                int secondStart = sameMinute ? start.getSecond() : 0;
                                for (int second = secondStart; second <= 59; second++) {
                                    if (!seconds[second]) {
                                        continue;
                                    }
                                    LocalDateTime time = LocalDateTime.of(year, month, day, hour, minute, second);
                                    if (!time.isBefore(start)) {
                                        return time;
                                    }
                                }
                            } else {
                                LocalDateTime time = LocalDateTime.of(year, month, day, hour, minute, 0);
                                if (!time.isBefore(start)) {
                                    return time;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean matchDay(LocalDate date) {
        boolean domMatch = daysOfMonth[date.getDayOfMonth()];
        boolean dowMatch = daysOfWeek[date.getDayOfWeek().getValue() % 7];
        boolean domRestricted = !isAll(daysOfMonth, 1, 31);
        boolean dowRestricted = !isAll(daysOfWeek, 0, 7);
        if (domRestricted && dowRestricted) {
            return domMatch || dowMatch;
        }
        if (domRestricted) {
            return domMatch;
        }
        if (dowRestricted) {
            return dowMatch;
        }
        return true;
    }

    private boolean isAll(boolean[] values, int min, int max) {
        for (int i = min; i <= max; i++) {
            if (!values[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return pattern;
    }
}
