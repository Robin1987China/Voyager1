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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池构建器，"" {@code io.voyager1.util.ExecutorBuilder}。
 */
public class ExecutorBuilder {

    private int corePoolSize = 0;
    private int maxPoolSize = Integer.MAX_VALUE;
    private long keepAliveTime = 60L;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private BlockingQueue<Runnable> workQueue = new SynchronousQueue<>();
    private ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();

    public static ExecutorBuilder create() {
        return new ExecutorBuilder();
    }

    public ExecutorBuilder setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public ExecutorBuilder setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public ExecutorBuilder setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    public ExecutorBuilder setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        return this;
    }

    public ExecutorBuilder setWorkQueue(BlockingQueue<Runnable> workQueue) {
        this.workQueue = workQueue;
        return this;
    }

    public ExecutorBuilder useArrayBlockingQueue(int capacity) {
        return setWorkQueue(new ArrayBlockingQueue<>(capacity));
    }

    public ExecutorBuilder useSynchronousQueue() {
        return setWorkQueue(new SynchronousQueue<>());
    }

    public ExecutorBuilder setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public ExecutorBuilder setHandler(RejectedExecutionHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * 构建线程池。
     *
     * @return ThreadPoolExecutor
     */
    public ThreadPoolExecutor build() {
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, timeUnit,
                workQueue, threadFactory, handler);
    }
}
