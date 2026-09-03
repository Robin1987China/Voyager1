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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 延迟观察者 {@code io.voyager1.util.DelayWatcher}。
 * <p>将短时间内同一路径的多次 modify 事件合并处理，延迟仅针对 modify 事件。</p>
 */
public class DelayWatcher implements Watcher {

    private final Set<Path> eventSet = ConcurrentHashMap.newKeySet();
    private final Watcher watcher;
    private final long delay;

    /**
     * 构造。
     *
     * @param watcher 实际处理事件的观察者
     * @param delay   延迟时间（毫秒）
     */
    public DelayWatcher(Watcher watcher, long delay) {
        if (watcher == null) {
            throw new IllegalArgumentException("Watcher must not be null");
        }
        if (watcher instanceof DelayWatcher) {
            throw new IllegalArgumentException("Watcher must not be a DelayWatcher");
        }
        this.watcher = watcher;
        this.delay = delay;
    }

    @Override
    public void onModify(WatchEvent<?> event, Path currentPath) {
        if (this.delay < 1) {
            this.watcher.onModify(event, currentPath);
        } else {
            onDelayModify(event, currentPath);
        }
    }

    @Override
    public void onCreate(WatchEvent<?> event, Path currentPath) {
        watcher.onCreate(event, currentPath);
    }

    @Override
    public void onDelete(WatchEvent<?> event, Path currentPath) {
        watcher.onDelete(event, currentPath);
    }

    @Override
    public void onOverflow(WatchEvent<?> event, Path currentPath) {
        watcher.onOverflow(event, currentPath);
    }

    private void onDelayModify(WatchEvent<?> event, Path currentPath) {
        Path eventPath = eventPath(currentPath, event);
        if (eventSet.contains(eventPath)) {
            return;
        }
        eventSet.add(eventPath);
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(delay);
            eventSet.remove(eventPath(currentPath, event));
            watcher.onModify(event, currentPath);
        });
    }

    private static Path eventPath(Path currentPath, WatchEvent<?> event) {
        Object context = event.context();
        if (context instanceof Path) {
            return Paths.get(currentPath.toString(), context.toString());
        }
        return currentPath;
    }
}
