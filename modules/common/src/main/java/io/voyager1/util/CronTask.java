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

import java.util.concurrent.ScheduledFuture;

/**
 * Cron 任务包装对象 {@code io.voyager1.util.CronTask}。
 * 将原始 {@link Task} 或 {@link Runnable} 与任务 id 及 cron 表达式绑定。
 */
public class CronTask implements Task {

    private final String id;
    private final CronPattern pattern;
    private final Task task;

    private volatile ScheduledFuture<?> future;

    public CronTask(String id, CronPattern pattern, Task task) {
        this.id = id;
        this.pattern = pattern;
        this.task = task;
    }

    public CronTask(String id, CronPattern pattern, Runnable runnable) {
        this(id, pattern, (Task) () -> runnable.run());
    }

    /**
     * 获取任务 id
     *
     * @return 任务 id
     */
    public String getId() {
        return id;
    }

    /**
     * 获取 cron 表达式对象
     *
     * @return cron 表达式对象
     */
    public CronPattern getPattern() {
        return pattern;
    }

    /**
     * 获取原始任务
     *
     * @return 原始任务
     */
    public Task getRaw() {
        return task;
    }

    /**
     * 获取已提交的执行计划
     *
     * @return 执行计划
     */
    public ScheduledFuture<?> getFuture() {
        return future;
    }

    /**
     * 设置执行计划
     *
     * @param future 执行计划
     */
    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    /**
     * 取消已提交的执行计划
     */
    public void cancel() {
        ScheduledFuture<?> scheduledFuture = future;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public void execute() {
        task.execute();
    }
}
