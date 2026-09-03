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

import java.lang.management.ManagementFactory;

/**
 * 系统工具，"" {@code io.voyager1.util.SystemUtil} 的常用方法。
 */
public class SystemUtil {

    private SystemUtil() {
    }

    public static String get(String name, String defaultValue) {
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv(name);
        }
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }

    public static String get(String name) {
        return get(name, null);
    }

    public static String set(String name, String value) {
        return System.setProperty(name, value);
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        String v = get(name);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static long getCurrentPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int idx = name.indexOf('@');
        return Long.parseLong(idx < 0 ? name : name.substring(0, idx));
    }

    public static String getUserInfo() {
        return System.getProperty("user.name");
    }

    public static java.lang.management.RuntimeMXBean getRuntimeMXBean() {
        return ManagementFactory.getRuntimeMXBean();
    }

    public static OsInfo getOsInfo() {
        return new OsInfo();
    }

    public static JavaInfo getJavaInfo() {
        return new JavaInfo();
    }

    public static JavaInfo getJavaRuntimeInfo() {
        return new JavaInfo();
    }
}
