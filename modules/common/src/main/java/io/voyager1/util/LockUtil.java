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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 锁工具，"" {@code io.voyager1.util.LockUtil}。
 */
public class LockUtil {

    private static final ConcurrentHashMap<Object, Lock> STATIC_LOCKS = new ConcurrentHashMap<>();

    /**
     * 创建可重入锁。
     *
     * @return ReentrantLock
     */
    public static ReentrantLock createLock() {
        return new ReentrantLock();
    }

    /**
     * 创建读写锁。
     *
     * @return ReentrantReadWriteLock
     */
    public static ReentrantReadWriteLock createReadWriteLock() {
        return new ReentrantReadWriteLock();
    }

    /**
     * 创建基于 key 的静态锁（同一 key 返回同一把锁）。
     *
     * @param key key
     * @return Lock
     */
    public static Lock createStaticLock(Object key) {
        return STATIC_LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * 创建 StampedLock 包装。
     *
     * @return StampedLock 包装
     */
    public static StampedLock createStampLock() {
        return new StampedLock();
    }

    /**
     * StampedLock 包装，"" {@code io.voyager1.util.StampedLock}。
     */
    public static class StampedLock {

        private final java.util.concurrent.locks.StampedLock lock = new java.util.concurrent.locks.StampedLock();

        public Lock asWriteLock() {
            return lock.asWriteLock();
        }

        public Lock asReadLock() {
            return lock.asReadLock();
        }

        public long readLock() {
            return lock.readLock();
        }

        public long writeLock() {
            return lock.writeLock();
        }

        public void unlockRead(long stamp) {
            lock.unlockRead(stamp);
        }

        public void unlockWrite(long stamp) {
            lock.unlockWrite(stamp);
        }
    }
}
