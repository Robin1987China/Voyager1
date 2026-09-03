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

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 字符串工具，"" {@code io.voyager1.util.StrUtil} 的常用方法。
 */
public class StrUtil {

    public static final String EMPTY = "";
    public static final String COMMA = ",";
    public static final String DOT = ".";
    public static final String DOUBLE_DOT = "..";
    public static final String COLON = ":";
    public static final String SLASH = "/";
    public static final String LF = "\n";
    public static final String CR = "\r";
    public static final String TAB = "\t";
    public static final String SPACE = " ";
    public static final String DASHED = "-";
    public static final String UNDERLINE = "_";
    public static final String BACKSLASH = "\\";
    public static final String BRACKET = "[]";
    public static final String BRACKET_START = "[";
    public static final String BRACKET_END = "]";
    public static final String CRLF = "\r\n";
    public static final String AT = "@";

    // ---- 判断 ----

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return str != null && str.length() > 0;
    }

    public static boolean isBlank(CharSequence str) {
        return str == null || str.toString().trim().isEmpty();
    }

    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    public static boolean isAllNotEmpty(CharSequence... strs) {
        if (strs == null) {
            return false;
        }
        for (CharSequence s : strs) {
            if (isEmpty(s)) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        return Objects.equals(a, b);
    }

    public static boolean equalsIgnoreCase(CharSequence a, CharSequence b) {
        return a == null ? b == null : a.toString().equalsIgnoreCase(b == null ? null : b.toString());
    }

    public static boolean equalsAny(CharSequence str, CharSequence... candidates) {
        if (str == null) {
            return false;
        }
        for (CharSequence c : candidates) {
            if (str.equals(c)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalsAnyIgnoreCase(CharSequence str, CharSequence... candidates) {
        if (str == null) {
            return false;
        }
        for (CharSequence c : candidates) {
            if (str.toString().equalsIgnoreCase(c == null ? null : c.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean startWith(CharSequence str, CharSequence prefix) {
        return str != null && prefix != null && str.toString().startsWith(prefix.toString());
    }

    public static boolean startWithIgnoreCase(CharSequence str, CharSequence prefix) {
        return str != null && prefix != null && str.toString().toLowerCase().startsWith(prefix.toString().toLowerCase());
    }

    public static boolean startWithAny(CharSequence str, CharSequence... prefixes) {
        if (str == null) {
            return false;
        }
        for (CharSequence p : prefixes) {
            if (str.toString().startsWith(p.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean endWith(CharSequence str, CharSequence suffix) {
        return str != null && suffix != null && str.toString().endsWith(suffix.toString());
    }

    public static boolean endWithIgnoreCase(CharSequence str, CharSequence suffix) {
        return str != null && suffix != null && str.toString().toLowerCase().endsWith(suffix.toString().toLowerCase());
    }

    public static boolean endWithAny(CharSequence str, CharSequence... suffixes) {
        if (str == null) {
            return false;
        }
        for (CharSequence s : suffixes) {
            if (str.toString().endsWith(s.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean endWithAnyIgnoreCase(CharSequence str, CharSequence... suffixes) {
        if (str == null) {
            return false;
        }
        String s = str.toString().toLowerCase();
        for (CharSequence suf : suffixes) {
            if (s.endsWith(suf.toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(CharSequence str, CharSequence search) {
        return str != null && search != null && str.toString().contains(search);
    }

    public static boolean containsIgnoreCase(CharSequence str, CharSequence search) {
        return str != null && search != null && str.toString().toLowerCase().contains(search.toString().toLowerCase());
    }

    public static boolean containsAny(CharSequence str, CharSequence... searches) {
        if (str == null) {
            return false;
        }
        for (CharSequence s : searches) {
            if (str.toString().contains(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAnyIgnoreCase(CharSequence str, CharSequence... searches) {
        if (str == null) {
            return false;
        }
        String s = str.toString().toLowerCase();
        for (CharSequence search : searches) {
            if (s.contains(search.toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBlank(CharSequence... strs) {
        if (strs == null) {
            return true;
        }
        for (CharSequence s : strs) {
            if (isBlank(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasEmpty(CharSequence... strs) {
        if (strs == null) {
            return true;
        }
        for (CharSequence s : strs) {
            if (isEmpty(s)) {
                return true;
            }
        }
        return false;
    }

    // ---- 转换 ----

    public static String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public static String toStringOrNull(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public static String nullToDefault(CharSequence str, String defaultValue) {
        return str == null ? defaultValue : str.toString();
    }

    public static String emptyToDefault(CharSequence str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str.toString();
    }

    public static String blankToDefault(CharSequence str, String defaultValue) {
        return isBlank(str) ? defaultValue : str.toString();
    }

    public static String trim(CharSequence str) {
        return str == null ? null : str.toString().trim();
    }

    public static int length(CharSequence str) {
        return str == null ? 0 : str.length();
    }

    public static byte[] bytes(CharSequence str) {
        return str == null ? null : str.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] bytes(CharSequence str, Charset charset) {
        return str == null ? null : str.toString().getBytes(charset);
    }

    public static boolean isWrap(CharSequence str, CharSequence prefix, CharSequence suffix) {
        if (str == null) {
            return false;
        }
        return str.toString().startsWith(prefix.toString()) && str.toString().endsWith(suffix.toString());
    }

    public static byte[] utf(CharSequence str) {
        return str == null ? null : str.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String str(CharSequence str, Charset charset) {
        return str == null ? null : str.toString();
    }

    public static String fillBefore(String str, char filledChar, int len) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= len) {
            return str;
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = str.length(); i < len; i++) {
            sb.append(filledChar);
        }
        sb.append(str);
        return sb.toString();
    }

    public static String wrap(CharSequence str, CharSequence prefixAndSuffix) {
        return prefixAndSuffix + (str == null ? "" : str.toString()) + prefixAndSuffix;
    }

    public static String unWrap(CharSequence str, CharSequence prefix, CharSequence suffix) {
        if (str == null) {
            return null;
        }
        String s = str.toString();
        if (s.startsWith(prefix.toString()) && s.endsWith(suffix.toString())) {
            return s.substring(prefix.length(), s.length() - suffix.length());
        }
        return s;
    }

    public static String unWrap(CharSequence str, char prefixAndSuffix) {
        if (str == null) {
            return null;
        }
        String s = str.toString();
        if (s.length() >= 2 && s.charAt(0) == prefixAndSuffix && s.charAt(s.length() - 1) == prefixAndSuffix) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static String wrapIfMissing(CharSequence str, CharSequence prefixAndSuffix) {
        String s = str == null ? "" : str.toString();
        if (s.startsWith(prefixAndSuffix.toString()) && s.endsWith(prefixAndSuffix.toString())) {
            return s;
        }
        return prefixAndSuffix + s + prefixAndSuffix;
    }

    // ---- 子串 ----

    public static String sub(CharSequence str, int fromIndex, int toIndex) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (fromIndex < 0) {
            fromIndex = len + fromIndex;
        }
        if (toIndex < 0) {
            toIndex = len + toIndex;
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (toIndex > len) {
            toIndex = len;
        }
        return str.toString().substring(fromIndex, toIndex);
    }

    public static String subBefore(CharSequence str, CharSequence separator) {
        if (str == null || separator == null) {
            return null;
        }
        int idx = str.toString().indexOf(separator.toString());
        return idx < 0 ? str.toString() : str.toString().substring(0, idx);
    }

    public static String subAfter(CharSequence str, CharSequence separator) {
        return subAfter(str, separator, false);
    }

    public static String subAfter(CharSequence str, CharSequence separator, boolean isLastSeparator) {
        if (str == null || separator == null) {
            return null;
        }
        int idx = isLastSeparator ? str.toString().lastIndexOf(separator.toString())
            : str.toString().indexOf(separator.toString());
        return idx < 0 ? EMPTY : str.toString().substring(idx + separator.length());
    }

    public static String subPre(CharSequence str, int beforeLength) {
        if (str == null) {
            return null;
        }
        return beforeLength >= str.length() ? str.toString() : str.toString().substring(0, beforeLength);
    }

    public static String maxLength(CharSequence str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str.toString() : str.toString().substring(0, maxLength);
    }

    // ---- 移除 ----

    public static String removePrefix(CharSequence str, CharSequence prefix) {
        if (str == null || prefix == null) {
            return str == null ? null : str.toString();
        }
        return str.toString().startsWith(prefix.toString()) ? str.toString().substring(prefix.length()) : str.toString();
    }

    public static String removePrefixIgnoreCase(CharSequence str, CharSequence prefix) {
        if (str == null || prefix == null) {
            return str == null ? null : str.toString();
        }
        return str.toString().toLowerCase().startsWith(prefix.toString().toLowerCase()) ? str.toString().substring(prefix.length()) : str.toString();
    }

    public static String removeSuffix(CharSequence str, CharSequence suffix) {
        if (str == null || suffix == null) {
            return str == null ? null : str.toString();
        }
        return str.toString().endsWith(suffix.toString()) ? str.toString().substring(0, str.length() - suffix.length()) : str.toString();
    }

    public static String removeAll(CharSequence str, CharSequence remove) {
        if (str == null || remove == null) {
            return str == null ? null : str.toString();
        }
        return str.toString().replace(remove, "");
    }

    public static String removeAny(CharSequence str, CharSequence... removes) {
        if (str == null) {
            return null;
        }
        String s = str.toString();
        for (CharSequence r : removes) {
            s = s.replace(r, "");
        }
        return s;
    }

    public static String replace(CharSequence str, CharSequence search, CharSequence replacement) {
        return str == null ? null : str.toString().replace(search, replacement);
    }

    // ---- 拼接/分割 ----

    public static String concat(CharSequence... strs) {
        if (strs == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : strs) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String concat(boolean isNullToEmpty, Object... objs) {
        if (objs == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object o : objs) {
            sb.append(isNullToEmpty ? (o == null ? "" : o.toString()) : String.valueOf(o));
        }
        return sb.toString();
    }

    public static List<String> split(CharSequence str, CharSequence separator) {
        if (str == null) {
            return null;
        }
        String s = str.toString();
        if (separator == null || separator.length() == 0) {
            return new ArrayList<>(Arrays.asList(s.split("")));
        }
        String sep = java.util.regex.Pattern.quote(separator.toString());
        return new ArrayList<>(Arrays.asList(s.split(sep)));
    }

    public static List<String> splitTrim(CharSequence str, CharSequence separator) {
        List<String> list = split(str, separator);
        if (list == null) {
            return null;
        }
        list.removeIf(s -> s.trim().isEmpty());
        list.replaceAll(String::trim);
        return list;
    }

    public static String[] splitToArray(CharSequence str, CharSequence separator) {
        if (str == null) {
            return null;
        }
        return str.toString().split(java.util.regex.Pattern.quote(separator.toString()));
    }

    public static int[] splitToInt(CharSequence str, CharSequence separator) {
        String[] arr = splitToArray(str, separator);
        if (arr == null) {
            return null;
        }
        return Arrays.stream(arr).mapToInt(s -> Integer.parseInt(s.trim())).toArray();
    }

    public static long[] splitToLong(CharSequence str, CharSequence separator) {
        String[] arr = splitToArray(str, separator);
        if (arr == null) {
            return null;
        }
        return Arrays.stream(arr).mapToLong(s -> Long.parseLong(s.trim())).toArray();
    }

    // ---- 大小写/命名 ----

    public static String toCamelCase(CharSequence str) {
        if (str == null) {
            return null;
        }
        return toCamelCase(str.toString(), '_');
    }

    public static String toCamelCase(CharSequence str, char separator) {
        if (str == null) {
            return null;
        }
        String s = str.toString();
        String[] parts = s.split(java.util.regex.Pattern.quote(String.valueOf(separator)));
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    public static String toUnderlineCase(CharSequence str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toString().toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- 其他 ----

    public static int count(CharSequence str, CharSequence search) {
        if (str == null || search == null || search.length() == 0) {
            return 0;
        }
        return StringUtils.countMatches(str.toString(), search.toString());
    }

    public static int count(CharSequence str, char c) {
        if (str == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public static String uuid() {
        return java.util.UUID.randomUUID().toString();
    }

    public static String format(CharSequence template, Object... params) {
        if (template == null) {
            return null;
        }
        String tpl = template.toString().replace("{}", "%s");
        return String.format(tpl, params);
    }

    public static int compareVersion(CharSequence version1, CharSequence version2) {
        String[] v1 = version1.toString().split("\\.");
        String[] v2 = version2.toString().split("\\.");
        int len = Math.max(v1.length, v2.length);
        for (int i = 0; i < len; i++) {
            int a = i < v1.length ? Integer.parseInt(v1[i]) : 0;
            int b = i < v2.length ? Integer.parseInt(v2[i]) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    public static boolean endWith(CharSequence str, CharSequence suffix, boolean ignoreCase) {
        if (str == null || suffix == null) {
            return false;
        }
        return ignoreCase ? str.toString().toLowerCase().endsWith(suffix.toString().toLowerCase())
            : str.toString().endsWith(suffix.toString());
    }

    public static String str(byte[] data, Charset charset) {
        return data == null ? null : new String(data, charset);
    }

    public static String subBefore(CharSequence str, CharSequence separator, boolean isLastSeparator) {
        if (str == null || separator == null) {
            return null;
        }
        int idx = isLastSeparator ? str.toString().lastIndexOf(separator.toString())
            : str.toString().indexOf(separator.toString());
        return idx < 0 ? str.toString() : str.toString().substring(0, idx);
    }

    public static String addPrefixIfNot(CharSequence str, CharSequence prefix) {
        if (str == null) {
            return null;
        }
        return str.toString().startsWith(prefix.toString()) ? str.toString() : prefix.toString() + str;
    }

    public static String wrapIfMissing(CharSequence str, CharSequence prefix, CharSequence suffix) {
        String s = str == null ? "" : str.toString();
        if (!s.startsWith(prefix.toString())) {
            s = prefix + s;
        }
        if (!s.endsWith(suffix.toString())) {
            s = s + suffix;
        }
        return s;
    }

    public static boolean isBlankOrUndefined(CharSequence str) {
        return isBlank(str) || "undefined".equalsIgnoreCase(str == null ? null : str.toString());
    }

    public static boolean isAllBlank(CharSequence... strs) {
        if (strs == null) {
            return true;
        }
        for (CharSequence s : strs) {
            if (!isBlank(s)) {
                return false;
            }
        }
        return true;
    }

    public static int lastIndexOfIgnoreCase(CharSequence str, CharSequence search) {
        return str == null ? -1 : str.toString().toLowerCase().lastIndexOf(search.toString().toLowerCase());
    }

    public static int indexOfIgnoreCase(CharSequence str, CharSequence search) {
        return str == null ? -1 : str.toString().toLowerCase().indexOf(search.toString().toLowerCase());
    }

    public static String utf8Str(byte[] data) {
        return data == null ? null : new String(data, StandardCharsets.UTF_8);
    }

    public static String utf8Str(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof byte[]) {
            return utf8Str((byte[]) obj);
        }
        return obj.toString();
    }

    public static String join(CharSequence separator, Iterable<?> iterable) {
        if (iterable == null) {
            return null;
        }
        java.util.Iterator<?> it = iterable.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(String.valueOf(it.next()));
            if (it.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }
}
