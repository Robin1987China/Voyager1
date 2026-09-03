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

import java.io.Closeable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * 线程同步结束器，"" {@code io.voyager1.util.SyncFinisher}。
 */
public class SyncFinisher implements Closeable {

    private final Set<Worker> workers = new LinkedHashSet<>();
    private final int threadSize;
    private boolean isBeginAtSameTime;
    private final CountDownLatch beginLatch = new CountDownLatch(1);
    private CountDownLatch endLatch;
    private ExecutorService executorService;

    public SyncFinisher(int threadSize) {
        this.threadSize = threadSize;
    }

    /**
     * 设置是否所有 worker 线程同时开始。
     *
     * @param isBeginAtSameTime 是否所有 worker 线程同时开始
     * @return this
     */
    public SyncFinisher setBeginAtSameTime(boolean isBeginAtSameTime) {
        this.isBeginAtSameTime = isBeginAtSameTime;
        return this;
    }

    /**
     * 增加工作线程。
     *
     * @param runnable 工作线程
     * @return this
     */
    public SyncFinisher addWorker(final Runnable runnable) {
        return addWorker(new Worker() {
            @Override
            public void work() {
                runnable.run();
            }
        });
    }

    /**
     * 增加工作线程。
     *
     * @param worker 工作线程
     * @return this
     */
    public synchronized SyncFinisher addWorker(Worker worker) {
        workers.add(worker);
        return this;
    }

    /**
     * 开始工作（阻塞等待完成）。
     */
    public void start() {
        start(true);
    }

    /**
     * 开始工作。
     *
     * @param sync 是否阻塞等待
     */
    public void start(boolean sync) {
        endLatch = new CountDownLatch(workers.size());

        if (null == this.executorService || this.executorService.isShutdown()) {
            this.executorService = ExecutorBuilder.create()
                    .setCorePoolSize(threadSize)
                    .setMaxPoolSize(threadSize)
                    .build();
        }
        for (Worker worker : workers) {
            executorService.submit(worker);
        }
        this.beginLatch.countDown();

        if (sync) {
            try {
                this.endLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UtilException(e);
            }
        }
    }

    /**
     * 结束线程池。
     */
    public void stop() {
        if (null != this.executorService) {
            this.executorService.shutdown();
            this.executorService = null;
        }
        clearWorker();
    }

    /**
     * 立即结束线程池所有线程。
     */
    public void stopNow() {
        if (null != this.executorService) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
        clearWorker();
    }

    /**
     * 清空工作线程对象。
     */
    public void clearWorker() {
        workers.clear();
    }

    /**
     * 剩余任务数。
     *
     * @return 剩余任务数
     */
    public long count() {
        return endLatch == null ? 0 : endLatch.getCount();
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * 工作者，为一个线程。
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
         * 任务内容。
         */
        public abstract void work();
    }
}
