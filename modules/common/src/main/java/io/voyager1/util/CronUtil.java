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
 * Cron 工具类，提供全局唯一的 {@link Scheduler} {@code io.voyager1.util.CronUtil}。
 */
public class CronUtil {

    private static volatile Scheduler scheduler;
    private static final Object LOCK = new Object();

    private CronUtil() {
    }

    /**
     * 获取全局调度器
     *
     * @return 调度器
     */
    public static Scheduler getScheduler() {
        if (scheduler == null) {
            synchronized (LOCK) {
                if (scheduler == null) {
                    scheduler = new Scheduler();
                }
            }
        }
        return scheduler;
    }

    /**
     * 设置全局调度器
     *
     * @param scheduler 调度器
     */
    public static void setScheduler(Scheduler scheduler) {
        CronUtil.scheduler = scheduler;
    }

    /**
     * 设置是否匹配秒
     *
     * @param isMatchSecond 是否匹配秒
     */
    public static void setMatchSecond(boolean isMatchSecond) {
        getScheduler().setMatchSecond(isMatchSecond);
    }

    /**
     * 是否匹配秒
     *
     * @return 是否匹配秒
     */
    public static boolean isMatchSecond() {
        return getScheduler().isMatchSecond();
    }

    /**
     * 添加定时任务
     *
     * @param id      任务 id
     * @param pattern cron 表达式
     * @param task    任务
     */
    public static void schedule(String id, String pattern, Task task) {
        getScheduler().schedule(id, pattern, task);
    }

    /**
     * 添加定时任务
     *
     * @param id      任务 id
     * @param pattern cron 表达式
     * @param task    任务
     */
    public static void schedule(String id, String pattern, Runnable task) {
        getScheduler().schedule(id, pattern, task);
    }

    /**
     * 移除定时任务
     *
     * @param id 任务 id
     */
    public static void remove(String id) {
        getScheduler().remove(id);
    }

    /**
     * 启动调度器
     */
    public static void start() {
        getScheduler().start();
    }

    /**
     * 停止调度器
     */
    public static void stop() {
        getScheduler().stop();
    }

    /**
     * 重启调度器
     */
    public static void restart() {
        stop();
        start();
    }
}
