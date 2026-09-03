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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本比较器 {@code io.voyager1.util.VersionComparator}。
 */
public class VersionComparator implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final VersionComparator INSTANCE = new VersionComparator();

    private static final Pattern SPLIT = Pattern.compile("(\\d+|\\.+)");

    @Override
    public int compare(String o1, String o2) {
        return compare((CharSequence) o1, (CharSequence) o2);
    }

    /**
     * 比较两个版本号。
     *
     * @param v1 版本1
     * @param v2 版本2
     * @return 比较结果
     */
    public static int compare(CharSequence v1, CharSequence v2) {
        if (v1 == v2) {
            return 0;
        }
        if (isBlank(v1)) {
            return isBlank(v2) ? 0 : -1;
        }
        if (isBlank(v2)) {
            return 1;
        }
        List<String> list1 = split(v1.toString());
        List<String> list2 = split(v2.toString());
        int size = Math.min(list1.size(), list2.size());
        for (int i = 0; i < size; i++) {
            int result = compareToken(list1.get(i), list2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(list1.size(), list2.size());
    }

    private static boolean isBlank(CharSequence cs) {
        if (cs == null) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> split(String version) {
        Matcher matcher = SPLIT.matcher(version);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    private static int compareToken(String a, String b) {
        boolean aNumber = isNumber(a);
        boolean bNumber = isNumber(b);
        if (aNumber && bNumber) {
            return new BigDecimal(a).compareTo(new BigDecimal(b));
        }
        return CompareUtil.compare(a, b);
    }

    private static boolean isNumber(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
