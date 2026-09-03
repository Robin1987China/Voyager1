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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程工具，"" {@code io.voyager1.util.ThreadUtil} 的常用方法。
 */
public class ThreadUtil {

    private static volatile ExecutorService executor;

    private static ExecutorService getExecutor() {
        if (executor == null) {
            synchronized (ThreadUtil.class) {
                if (executor == null) {
                    executor = Executors.newCachedThreadPool(r -> {
                        Thread t = new Thread(r);
                        t.setDaemon(false);
                        return t;
                    });
                }
            }
        }
        return executor;
    }

    public static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }

    public static java.util.concurrent.Future<?> execAsync(Runnable runnable) {
        return getExecutor().submit(runnable);
    }

    public static <T> java.util.concurrent.Future<T> execAsync(java.util.concurrent.Callable<T> task) {
        return getExecutor().submit(task);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
