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
 * 定时任务监听器，等价于  的 {@code io.voyager1.util.TaskListener}。
 */
public interface TaskListener {

    /**
     * 任务开始执行前触发
     *
     * @param executor 任务执行器
     */
    void onStart(TaskExecutor executor);

    /**
     * 任务执行成功后触发
     *
     * @param executor 任务执行器
     */
    void onSucceeded(TaskExecutor executor);

    /**
     * 任务执行失败后触发
     *
     * @param executor  任务执行器
     * @param exception 异常
     */
    void onFailed(TaskExecutor executor, Throwable exception);
}
