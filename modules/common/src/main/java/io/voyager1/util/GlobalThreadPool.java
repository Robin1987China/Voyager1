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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 全局线程池，"" {@code io.voyager1.util.GlobalThreadPool}。
 */
public class GlobalThreadPool {

    private static volatile ExecutorService executor;

    /**
     * 获取全局线程池（懒加载单例）。
     *
     * @return ExecutorService
     */
    public static ExecutorService getExecutor() {
        if (executor == null) {
            synchronized (GlobalThreadPool.class) {
                if (executor == null) {
                    executor = Executors.newCachedThreadPool();
                }
            }
        }
        return executor;
    }

    /**
     * 直接执行任务。
     *
     * @param runnable 任务
     */
    public static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }

    /**
     * 提交任务。
     *
     * @param task 任务
     * @param <T>  返回值类型
     * @return Future
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return getExecutor().submit(task);
    }

    /**
     * 提交任务。
     *
     * @param task 任务
     * @return Future
     */
    public static Future<?> submit(Runnable task) {
        return getExecutor().submit(task);
    }

    /**
     * 关闭线程池。
     */
    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
