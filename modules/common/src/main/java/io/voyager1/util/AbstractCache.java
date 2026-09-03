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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 缓存抽象基类，基于 Caffeine 实现 {@code io.voyager1.util.AbstractCache}。
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    private static final ScheduledExecutorService PRUNE_EXECUTOR = createPruneExecutor();

    protected com.github.benmanes.caffeine.cache.Cache<K, CacheObj<K, V>> raw;
    private volatile CacheListener<K, V> listener;
    private volatile boolean pruneScheduled;

    /**
     * 使用构造器配置（已含 maximumSize 等策略）完成通用配置并构建缓存。
     */
    protected com.github.benmanes.caffeine.cache.Cache<K, CacheObj<K, V>> finish(Caffeine<Object, Object> spec) {
        return spec.expireAfter(new TimeoutExpiry<K, V>())
                .removalListener((K key, CacheObj<K, V> obj, RemovalCause cause) -> onRemoval(key, obj, cause))
                .build();
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        CacheObj<K, V> obj = raw.getIfPresent(key);
        return obj == null ? null : obj.getValue();
    }

    @Override
    public void put(K key, V object) {
        put(key, object, defaultTimeout());
    }

    @Override
    public void put(K key, V object, long timeout) {
        if (key == null) {
            return;
        }
        if (object == null) {
            raw.invalidate(key);
            return;
        }
        raw.put(key, new CacheObj<>(key, object, timeout));
    }

    @Override
    public void remove(K key) {
        if (key != null) {
            raw.invalidate(key);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return key != null && raw.getIfPresent(key) != null;
    }

    @Override
    public int size() {
        return (int) raw.estimatedSize();
    }

    @Override
    public void clear() {
        raw.invalidateAll();
    }

    @Override
    public Iterator<CacheObj<K, V>> iterator() {
        return cacheObjIterator();
    }

    @Override
    public Iterator<CacheObj<K, V>> cacheObjIterator() {
        List<CacheObj<K, V>> snapshot = new ArrayList<>(raw.asMap().values());
        snapshot.removeIf(CacheObj::isExpired);
        return snapshot.iterator();
    }

    @Override
    public void setListener(CacheListener<K, V> listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void schedulePrune(long delay) {
        if (pruneScheduled) {
            return;
        }
        pruneScheduled = true;
        long d = Math.max(1, delay);
        PRUNE_EXECUTOR.scheduleWithFixedDelay(raw::cleanUp, d, d, TimeUnit.MILLISECONDS);
    }

    /**
     * 缓存默认过期时间（毫秒），0 或负数表示永不过期。
     */
    protected abstract long defaultTimeout();

    private void onRemoval(K key, CacheObj<K, V> obj, RemovalCause cause) {
        CacheListener<K, V> l = this.listener;
        if (l != null && obj != null && (cause == RemovalCause.EXPIRED || cause == RemovalCause.SIZE)) {
            l.onRemove(key, obj.getValue());
        }
    }

    private static ScheduledExecutorService createPruneExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "voyager1-cache-prune");
            t.setDaemon(true);
            return t;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    /**
     * 按缓存对象自身 TTL 计算过期时间。
     */
    protected static final class TimeoutExpiry<K, V> implements Expiry<K, CacheObj<K, V>> {

        @Override
        public long expireAfterCreate(K key, CacheObj<K, V> obj, long currentTime) {
            long ttl = obj.getTtlMillis();
            return ttl < 0 ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toNanos(ttl);
        }

        @Override
        public long expireAfterUpdate(K key, CacheObj<K, V> obj, long currentTime, long currentDuration) {
            return expireAfterCreate(key, obj, currentTime);
        }

        @Override
        public long expireAfterRead(K key, CacheObj<K, V> obj, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }
}
