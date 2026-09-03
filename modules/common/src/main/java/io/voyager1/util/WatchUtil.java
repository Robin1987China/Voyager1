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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 监听工具类，"" {@code io.voyager1.util.WatchUtil}。
 */
public class WatchUtil {

    private WatchUtil() {
    }

    /**
     * 创建并初始化监听，监听所有事件。
     *
     * @param path 路径
     * @param watcher {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createAll(String path, Watcher watcher) {
        return createAll(path, 0, watcher);
    }

    /**
     * 创建并初始化监听，监听所有事件。
     *
     * @param path     路径
     * @param maxDepth 递归目录最大深度，小于 1 时不递归子目录
     * @param watcher  {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createAll(String path, int maxDepth, Watcher watcher) {
        return createAll(Paths.get(path), maxDepth, watcher);
    }

    /**
     * 创建并初始化监听，监听所有事件。
     *
     * @param file    被监听文件
     * @param watcher {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createAll(File file, Watcher watcher) {
        return createAll(file.toPath(), 0, watcher);
    }

    /**
     * 创建并初始化监听，监听所有事件。
     *
     * @param file     被监听文件
     * @param maxDepth 递归目录最大深度
     * @param watcher  {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createAll(File file, int maxDepth, Watcher watcher) {
        return createAll(file.toPath(), maxDepth, watcher);
    }

    /**
     * 创建并初始化监听，监听所有事件。
     *
     * @param path     路径
     * @param maxDepth 递归目录最大深度
     * @param watcher  {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createAll(Path path, int maxDepth, Watcher watcher) {
        WatchMonitor monitor = new WatchMonitor(path, maxDepth, WatchMonitor.EVENTS_ALL);
        monitor.setWatcher(watcher);
        return monitor;
    }

    /**
     * 创建并初始化监听，监听修改事件。
     *
     * @param path    路径
     * @param watcher {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createModify(String path, Watcher watcher) {
        return createModify(path, 0, watcher);
    }

    /**
     * 创建并初始化监听，监听修改事件。
     *
     * @param path     路径
     * @param maxDepth 递归目录最大深度
     * @param watcher  {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createModify(String path, int maxDepth, Watcher watcher) {
        return createModify(Paths.get(path), maxDepth, watcher);
    }

    /**
     * 创建并初始化监听，监听修改事件。
     *
     * @param file    被监听文件
     * @param watcher {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createModify(File file, Watcher watcher) {
        return createModify(file.toPath(), 0, watcher);
    }

    /**
     * 创建并初始化监听，监听修改事件。
     *
     * @param path     路径
     * @param maxDepth 递归目录最大深度
     * @param watcher  {@link Watcher}
     * @return {@link WatchMonitor}
     */
    public static WatchMonitor createModify(Path path, int maxDepth, Watcher watcher) {
        WatchMonitor monitor = new WatchMonitor(path, maxDepth, WatchMonitor.ENTRY_MODIFY);
        monitor.setWatcher(watcher);
        return monitor;
    }
}
