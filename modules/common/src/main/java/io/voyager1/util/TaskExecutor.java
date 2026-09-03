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
 * 任务执行器，等价于  的 {@code io.voyager1.util.TaskExecutor}。
 * 包裹一个 {@link CronTask} 并负责实际执行其中的原始 {@link Task}。
 */
public class TaskExecutor implements Runnable {

    private final Scheduler scheduler;
    private final CronTask task;

    public TaskExecutor(Scheduler scheduler, CronTask task) {
        this.scheduler = scheduler;
        this.task = task;
    }

    /**
     * 获取原始任务
     *
     * @return 原始任务
     */
    public Task getTask() {
        return task.getRaw();
    }

    /**
     * 获取 Cron 任务包装对象
     *
     * @return Cron 任务
     */
    public CronTask getCronTask() {
        return task;
    }

    /**
     * 获取所属调度器
     *
     * @return 调度器
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void run() {
        task.execute();
    }
}
