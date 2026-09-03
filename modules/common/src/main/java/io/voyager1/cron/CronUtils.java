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

package io.voyager1.cron;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.util.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
public class CronUtils {

    private static final Map<String, TaskStat> TASK_STAT = new ConcurrentHashMap<>(50);

    /**
     * 任务统计
     */
    public static class TaskStat {
        /**
         * 执行次数
         */
        private final AtomicInteger executeCount = new AtomicInteger(0);
        /**
         * 失败次数
         */
        private final AtomicInteger failedCount = new AtomicInteger(0);
        /**
         * 成功次数
         */
        private final AtomicInteger succeedCount = new AtomicInteger(0);
        /**
         * 最后执行时间
         */
        private Long lastExecuteTime;
        /**
         * 描述
         */
        private final String desc;

        public TaskStat(String desc) {
            this.desc = desc;
        }

        public void onStart() {
            this.lastExecuteTime = System.currentTimeMillis();
            this.executeCount.incrementAndGet();
        }

        public void onSucceeded() {
            this.succeedCount.incrementAndGet();
        }

        public void onFailed(String tag, Throwable exception) {
            this.failedCount.incrementAndGet();
            log.error("定时任务异常 {}", tag, exception);
        }
    }

    /**
     * 开始
     */
    public static void start() {
        //
        Scheduler scheduler = CronUtil.getScheduler();
        //
        boolean started = scheduler.isStarted();
        if (started) {
            return;
        }
        synchronized (CronUtils.class) {
            started = scheduler.isStarted();
            if (started) {
                return;
            }
            CronUtil.start();
            scheduler.addListener(new TaskListener() {
                @Override
                public void onStart(TaskExecutor executor) {
                    CronTask cronTask = executor.getCronTask();
                    TaskStat taskStat = CronUtils.getTaskStat(cronTask.getId(), null);
                    taskStat.onStart();
                }

                @Override
                public void onSucceeded(TaskExecutor executor) {
                    CronTask cronTask = executor.getCronTask();
                    TaskStat taskStat = CronUtils.getTaskStat(cronTask.getId(), null);
                    taskStat.onSucceeded();
                }

                @Override
                public void onFailed(TaskExecutor executor, Throwable exception) {
                    CronTask cronTask = executor.getCronTask();
                    TaskStat taskStat = CronUtils.getTaskStat(cronTask.getId(), null);
                    taskStat.onFailed(cronTask.getId(), exception);
                }
            });
        }
    }

    /**
     * 获取任务统计
     *
     * @param id 任务id
     * @return 统计对象
     */
    public static TaskStat getTaskStat(String id, String desc) {
        return TASK_STAT.computeIfAbsent(id, s -> new TaskStat(desc));
    }

    /**
     * 获取任务列表
     *
     * @return list
     */
    public static List<JSONObject> list() {
        Scheduler scheduler = CronUtil.getScheduler();
        Set<Map.Entry<String, TaskStat>> entries = TASK_STAT.entrySet();
        return entries.stream()
            .map(entry -> {
                TaskStat taskStat = entry.getValue();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("taskId", entry.getKey());
                CronPattern pattern = scheduler.getPattern(entry.getKey());
                Optional.ofNullable(pattern).ifPresent(cronPattern -> jsonObject.put("cron", cronPattern.toString()));
                if (taskStat != null) {
                    jsonObject.put("executeCount", taskStat.executeCount.get());
                    jsonObject.put("failedCount", taskStat.failedCount.get());
                    jsonObject.put("succeedCount", taskStat.succeedCount.get());
                    jsonObject.put("lastExecuteTime", taskStat.lastExecuteTime);
                    jsonObject.put("desc", taskStat.desc);
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    /**
     * 添加任务 已经存在则不添加
     *
     * @param id       任务ID
     * @param cron     表达式
     * @param supplier 创建任务回调
     */
    public static void add(String id, String cron, Supplier<Task> supplier) {
        Scheduler scheduler = CronUtil.getScheduler();
        Task task = scheduler.getTask(id);
        if (task != null) {
            return;
        }
        scheduler.schedule(id, cron, supplier.get());
        //
        CronUtils.start();
    }

    /**
     * 添加任务、自动去重
     *
     * @param id   任务ID
     * @param cron 表达式
     * @param task 任务作业
     */
    public static void upsert(String id, String cron, Task task) {
        Scheduler scheduler = CronUtil.getScheduler();
        Task schedulerTask = scheduler.getTask(id);
        if (schedulerTask != null) {
            CronUtil.remove(id);
        }
        // 创建任务
        CronUtil.schedule(id, cron, task);
        //
        CronUtils.start();
    }

    /**
     * 停止定时任务
     *
     * @param id ID
     */
    public static void remove(String id) {
        CronUtil.remove(id);
        TASK_STAT.remove(id);
    }
}
