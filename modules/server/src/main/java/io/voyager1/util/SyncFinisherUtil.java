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

import io.voyager1.util.IoUtil;

import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 线程同步器 工具类
 *
 * @since 2023/3/18
 */
public final class SyncFinisherUtil {

    private static final Map<String, StrictSyncFinisher> SYNC_FINISHER_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 任务列表
     *
     * @return 任务列表
     */
    public static Set<String> keys() {
        return SYNC_FINISHER_MAP.keySet();
    }

    /**
     * 创建线程同步器
     *
     * @param core          线程核心数
     * @param queueCapacity 任务队列数
     * @return 线程同步器
     */
    private static StrictSyncFinisher create(int core, int queueCapacity) {
        int threadSize = Math.min(core, Runtime.getRuntime().availableProcessors());
        return new StrictSyncFinisher(threadSize, queueCapacity);
    }

    /**
     * 创建线程同步器
     * 核心任务数 为 cpu 核心数
     *
     * @param queueCapacity 任务队列数
     * @param name          任务名
     * @return 线程同步器
     */
    public static StrictSyncFinisher create(String name, int queueCapacity) {
        int threadSize = Math.min(Runtime.getRuntime().availableProcessors(), queueCapacity);
        StrictSyncFinisher strictSyncFinisher = new StrictSyncFinisher(threadSize, queueCapacity);
        put(name, strictSyncFinisher);
        return strictSyncFinisher;
    }

    /**
     * 添加任务
     *
     * @param name         任务名
     * @param syncFinisher 同步器
     */
    public static void put(String name, StrictSyncFinisher syncFinisher) {
        Assert.state(!SYNC_FINISHER_MAP.containsKey(name), "任务已经存在啦");
        SYNC_FINISHER_MAP.put(name, syncFinisher);
    }

    /**
     * 取消 任务
     *
     * @param name 任务名
     */
    public static boolean cancel(String name) {
        StrictSyncFinisher syncFinisher = SYNC_FINISHER_MAP.remove(name);
        Optional.ofNullable(syncFinisher).ifPresent(StrictSyncFinisher::stopNow);
        return syncFinisher != null;
    }

    /**
     * 关闭任务
     *
     * @param name 任务名
     */
    public static void close(String name) {
        IoUtil.close(SYNC_FINISHER_MAP.remove(name));
    }
}
