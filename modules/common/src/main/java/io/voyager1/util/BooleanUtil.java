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
 * 布尔工具，"" {@code io.voyager1.util.BooleanUtil}。
 */
public class BooleanUtil {

    public static boolean toBoolean(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value));
    }

    public static boolean toBoolean(Object value) {
        return value != null && toBoolean(value.toString());
    }

    public static Boolean toBooleanObject(String value) {
        if (value == null) {
            return null;
        }
        if ("1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "n".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
