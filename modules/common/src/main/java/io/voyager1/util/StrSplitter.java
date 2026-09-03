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
import java.util.List;

/**
 * 字符串分割，"" {@code io.voyager1.util.StrSplitter}。
 */
public class StrSplitter {

    public static List<String> splitTrim(CharSequence str, char separator) {
        List<String> result = new ArrayList<>();
        if (str == null) {
            return result;
        }
        for (String s : str.toString().split(java.util.regex.Pattern.quote(String.valueOf(separator)))) {
            if (!s.trim().isEmpty()) {
                result.add(s.trim());
            }
        }
        return result;
    }

    public static List<String> splitTrim(CharSequence str, CharSequence separator, boolean ignoreEmpty) {
        List<String> result = new ArrayList<>();
        if (str == null) {
            return result;
        }
        for (String s : str.toString().split(java.util.regex.Pattern.quote(separator.toString()))) {
            if (!ignoreEmpty || !s.trim().isEmpty()) {
                result.add(s.trim());
            }
        }
        return result;
    }

    public static List<String> splitTrim(CharSequence str, CharSequence separator) {
        List<String> result = new ArrayList<>();
        if (str == null) {
            return result;
        }
        for (String s : str.toString().split(java.util.regex.Pattern.quote(separator.toString()))) {
            if (!s.trim().isEmpty()) {
                result.add(s.trim());
            }
        }
        return result;
    }
}
