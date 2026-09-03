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

import io.voyager1.util.UtilException;
import io.voyager1.util.ExecutorBuilder;
import io.voyager1.util.SyncFinisher;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 线程同步结束器<br>
 * 在完成一组正在其他线程中执行的操作之前，它允许一个或多个线程一直等待。
 *
 * @see SyncFinisher
 * @since 2023/1/10
 */
public class StrictSyncFinisher implements Closeable {

    private final Set<StrictSyncFinisher.Worker> workers;
    private final int threadSize;
    private final int queueCapacity;
    private ExecutorService executorService;

    private boolean isBeginAtSameTime;
    /**
     * 启动同步器，用于保证所有worker线程同时开始
     */
    private final CountDownLatch beginLatch;
    /**
     * 结束同步器，用于等待所有worker线程同时结束
     */
    private CountDownLatch endLatch;

    /**
     * 构造
     *
     * @param threadSize    线程数
     * @param queueCapacity 队列数
     */
    public StrictSyncFinisher(int threadSize, int queueCapacity) {
        this.beginLatch = new CountDownLatch(1);
        this.threadSize = threadSize;
        this.queueCapacity = queueCapacity;
        this.workers = new LinkedHashSet<>();
    }

    /**
     * 设置是否所有worker线程同时开始
     *
     * @param isBeginAtSameTime 是否所有worker线程同时开始
     * @return this
     */
    public StrictSyncFinisher setBeginAtSameTime(boolean isBeginAtSameTime) {
        this.isBeginAtSameTime = isBeginAtSameTime;
        return this;
    }

    /**
     * 增加定义的线程数同等数量的worker
     *
     * @param runnable 工作线程
     * @return this
     */
    public StrictSyncFinisher addRepeatWorker(final Runnable runnable) {
        for (int i = 0; i < this.threadSize; i++) {
            addWorker(new StrictSyncFinisher.Worker() {
                @Override
                public void work() {
                    runnable.run();
                }
            });
        }
        return this;
    }

    /**
     * 增加工作线程
     *
     * @param runnable 工作线程
     * @return this
     */
    public StrictSyncFinisher addWorker(final Runnable runnable) {
        return addWorker(new StrictSyncFinisher.Worker() {
            @Override
            public void work() {
                runnable.run();
            }
        });
    }

    /**
     * 增加工作线程
     *
     * @param worker 工作线程
     * @return this
     */
    synchronized public StrictSyncFinisher addWorker(StrictSyncFinisher.Worker worker) {
        workers.add(worker);
        return this;
    }

    /**
     * 开始工作<br>
     * 执行此方法后如果不再重复使用此对象，需调用{@link #stop()}关闭回收资源。
     */
    public void start() {
        start(true);
    }

    /**
     * 开始工作<br>
     * 执行此方法后如果不再重复使用此对象，需调用{@link #stop()}关闭回收资源。
     *
     * @param sync 是否阻塞等待
     * @since 4.5.8
     */
    public void start(boolean sync) {
        endLatch = new CountDownLatch(workers.size());

        if (null == this.executorService || this.executorService.isShutdown()) {
            this.executorService = ExecutorBuilder.create()
                .setCorePoolSize(threadSize)
                .setMaxPoolSize(threadSize)
                .setWorkQueue(new LinkedBlockingQueue<>(queueCapacity))
                .build();
        }
        for (StrictSyncFinisher.Worker worker : workers) {
            executorService.submit(worker);
        }
        // 保证所有worker同时开始
        this.beginLatch.countDown();

        if (sync) {
            try {
                this.endLatch.await();
            } catch (InterruptedException e) {
                throw new UtilException(e);
            }
        }
    }

    /**
     * 结束线程池。此方法执行两种情况：
     * <ol>
     *     <li>执行start(true)后，调用此方法结束线程池回收资源</li>
     *     <li>执行start(false)后，用户自行判断结束点执行此方法</li>
     * </ol>
     *
     * @since 5.6.6
     */
    public void stop() {
        if (null != this.executorService) {
            this.executorService.shutdown();
            this.executorService = null;
        }

        clearWorker();
    }

    /**
     * 立即结束线程池所有线程。此方法执行两种情况：
     * <ol>
     *     <li>执行start(true)后，调用此方法结束线程池回收资源</li>
     *     <li>执行start(false)后，用户自行判断结束点执行此方法</li>
     * </ol>
     *
     * @since 5.8.11
     */
    public void stopNow() {
        if (null != this.executorService) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }

        clearWorker();
    }

    /**
     * 清空工作线程对象
     */
    public void clearWorker() {
        workers.clear();
    }

    /**
     * 剩余任务数
     *
     * @return 剩余任务数
     */
    public long count() {
        return endLatch.getCount();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    /**
     * 工作者，为一个线程
     *
     */
    public abstract class Worker implements Runnable {

        @Override
        public void run() {
            if (isBeginAtSameTime) {
                try {
                    beginLatch.await();
                } catch (InterruptedException e) {
                    throw new UtilException(e);
                }
            }
            try {
                work();
            } finally {
                endLatch.countDown();
            }
        }

        /**
         * 任务内容
         */
        public abstract void work();
    }
}
