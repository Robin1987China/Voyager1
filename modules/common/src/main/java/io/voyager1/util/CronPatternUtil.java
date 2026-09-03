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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cron 表达式工具 {@code io.voyager1.util.CronPatternUtil}。
 */
public class CronPatternUtil {

    private CronPatternUtil() {
    }

    /**
     * 获取指定 cron 表达式接下来的匹配时间列表
     *
     * @param patternStr    cron 表达式
     * @param start         起始时间（不包含）
     * @param count         数量
     * @param isMatchSecond 是否匹配秒
     * @return 匹配时间列表
     */
    public static List<Date> matchedDates(String patternStr, Date start, int count, boolean isMatchSecond) {
        CronPattern pattern = new CronPattern(patternStr);
        List<Date> result = new ArrayList<>(Math.max(0, count));
        long time = start.getTime();
        for (int i = 0; i < count; i++) {
            Date next = pattern.nextMatchAfter(time, isMatchSecond);
            if (next == null) {
                break;
            }
            result.add(next);
            time = next.getTime();
        }
        return result;
    }

    /**
     * 获取指定 cron 表达式在时间区间内的匹配时间列表
     *
     * @param patternStr    cron 表达式
     * @param start         起始时间（不包含）
     * @param end           结束时间（包含）
     * @param count         数量
     * @param isMatchSecond 是否匹配秒
     * @return 匹配时间列表
     */
    public static List<Date> matchedDates(String patternStr, Date start, Date end, int count, boolean isMatchSecond) {
        CronPattern pattern = new CronPattern(patternStr);
        List<Date> result = new ArrayList<>(Math.max(0, count));
        long time = start.getTime();
        long endTime = end.getTime();
        for (int i = 0; i < count; i++) {
            Date next = pattern.nextMatchAfter(time, isMatchSecond);
            if (next == null) {
                break;
            }
            if (next.getTime() > endTime) {
                break;
            }
            result.add(next);
            time = next.getTime();
        }
        return result;
    }
}
