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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cron 任务调度器，等价于  的 {@code io.voyager1.util.Scheduler}。
 * 使用 {@link ScheduledExecutorService} 调度各任务的最近一次执行，并在每次执行后重新计算下一次匹配时间。
 */
public class Scheduler {

    private final Map<String, CronTask> taskMap = new ConcurrentHashMap<>();
    private final List<TaskListener> listeners = new CopyOnWriteArrayList<>();

    private final Object lock = new Object();
    private volatile ScheduledExecutorService threadPool;
    private volatile boolean started = false;
    private volatile boolean matchSecond = false;

    public Scheduler() {
    }

    /**
     * 是否已启动
     *
     * @return 是否已启动
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * 设置是否匹配秒
     *
     * @param matchSecond 是否匹配秒
     */
    public void setMatchSecond(boolean matchSecond) {
        this.matchSecond = matchSecond;
    }

    /**
     * 是否匹配秒
     *
     * @return 是否匹配秒
     */
    public boolean isMatchSecond() {
        return matchSecond;
    }

    /**
     * 添加任务监听器
     *
     * @param listener 监听器
     */
    public void addListener(TaskListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 获取指定 id 的 cron 表达式对象
     *
     * @param id 任务 id
     * @return cron 表达式对象，不存在返回 null
     */
    public CronPattern getPattern(String id) {
        CronTask task = taskMap.get(id);
        return task == null ? null : task.getPattern();
    }

    /**
     * 获取指定 id 的原始任务
     *
     * @param id 任务 id
     * @return 原始任务，不存在返回 null
     */
    public Task getTask(String id) {
        CronTask task = taskMap.get(id);
        return task == null ? null : task.getRaw();
    }

    /**
     * 添加任务（已存在则替换）
     *
     * @param id      任务 id
     * @param pattern cron 表达式字符串
     * @param task    任务
     */
    public void schedule(String id, String pattern, Task task) {
        schedule(id, new CronPattern(pattern), task);
    }

    /**
     * 添加任务（已存在则替换）
     *
     * @param id      任务 id
     * @param pattern cron 表达式对象
     * @param task    任务
     */
    public void schedule(String id, CronPattern pattern, Task task) {
        addTask(new CronTask(id, pattern, task));
    }

    /**
     * 添加任务（已存在则替换）
     *
     * @param id      任务 id
     * @param pattern cron 表达式字符串
     * @param task    任务
     */
    public void schedule(String id, String pattern, Runnable task) {
        schedule(id, new CronPattern(pattern), task);
    }

    /**
     * 添加任务（已存在则替换）
     *
     * @param id      任务 id
     * @param pattern cron 表达式对象
     * @param task    任务
     */
    public void schedule(String id, CronPattern pattern, Runnable task) {
        addTask(new CronTask(id, pattern, task));
    }

    private void addTask(CronTask cronTask) {
        synchronized (lock) {
            CronTask old = taskMap.put(cronTask.getId(), cronTask);
            if (old != null) {
                old.cancel();
            }
            if (started && threadPool != null) {
                scheduleNext(cronTask);
            }
        }
    }

    /**
     * 移除任务
     *
     * @param id 任务 id
     */
    public void remove(String id) {
        CronTask task = taskMap.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 启动调度器
     */
    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            threadPool = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "voyager1-cron-scheduler");
                thread.setDaemon(true);
                return thread;
            });
            started = true;
            for (CronTask task : taskMap.values()) {
                scheduleNext(task);
            }
        }
    }

    /**
     * 停止调度器
     */
    public void stop() {
        synchronized (lock) {
            if (!started) {
                return;
            }
            started = false;
            if (threadPool != null) {
                threadPool.shutdownNow();
                threadPool = null;
            }
        }
    }

    private void scheduleNext(CronTask task) {
        Date now = new Date();
        Date next = task.getPattern().nextMatchAfter(now, matchSecond);
        if (next == null) {
            return;
        }
        long delay = Math.max(0, next.getTime() - now.getTime());
        ScheduledFuture<?> future = threadPool.schedule(() -> executeTask(task), delay, TimeUnit.MILLISECONDS);
        task.setFuture(future);
    }

    private void executeTask(CronTask cronTask) {
        TaskExecutor taskExecutor = new TaskExecutor(this, cronTask);
        try {
            for (TaskListener listener : listeners) {
                listener.onStart(taskExecutor);
            }
        } catch (Throwable ignored) {
            // 监听器异常不应影响任务执行
        }
        try {
            taskExecutor.run();
            for (TaskListener listener : listeners) {
                listener.onSucceeded(taskExecutor);
            }
        } catch (Throwable exception) {
            for (TaskListener listener : listeners) {
                listener.onFailed(taskExecutor, exception);
            }
        } finally {
            synchronized (lock) {
                if (started && threadPool != null && taskMap.containsKey(cronTask.getId())) {
                    scheduleNext(cronTask);
                }
            }
        }
    }
}
