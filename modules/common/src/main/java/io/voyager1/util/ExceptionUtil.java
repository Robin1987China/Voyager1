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
 * 异常工具，"" {@code io.voyager1.util.ExceptionUtil} 的常用方法。
 */
public class ExceptionUtil {

    public static boolean isCausedBy(Throwable throwable, Class<? extends Throwable> causeClass) {
        Throwable cause = throwable;
        while (cause != null) {
            if (causeClass.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T getCausedBy(Throwable throwable, Class<T> causeClass) {
        Throwable cause = throwable;
        while (cause != null) {
            if (causeClass.isInstance(cause)) {
                return (T) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }

    public static List<Throwable> getThrowableList(Throwable throwable) {
        List<Throwable> list = new ArrayList<>();
        Throwable cause = throwable;
        while (cause != null) {
            list.add(cause);
            cause = cause.getCause();
        }
        return list;
    }

    public static String stacktraceToString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
