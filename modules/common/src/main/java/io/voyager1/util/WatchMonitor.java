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
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 路径监听器 {@code io.voyager1.util.WatchMonitor}。
 */
public class WatchMonitor extends Thread implements Closeable {

    /**
     * 事件丢失
     */
    public static final WatchEvent.Kind<?> OVERFLOW = StandardWatchEventKinds.OVERFLOW;
    /**
     * 修改事件
     */
    public static final WatchEvent.Kind<?> ENTRY_MODIFY = StandardWatchEventKinds.ENTRY_MODIFY;
    /**
     * 创建事件
     */
    public static final WatchEvent.Kind<?> ENTRY_CREATE = StandardWatchEventKinds.ENTRY_CREATE;
    /**
     * 删除事件
     */
    public static final WatchEvent.Kind<?> ENTRY_DELETE = StandardWatchEventKinds.ENTRY_DELETE;
    /**
     * 全部事件
     */
    public static final WatchEvent.Kind<?>[] EVENTS_ALL = {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE, OVERFLOW};

    private WatchService watchService;
    private WatchEvent.Kind<?>[] events;
    private final Map<WatchKey, Path> watchKeyPathMap = new HashMap<>();

    private Path path;
    private int maxDepth;
    private Path filePath;
    private Watcher watcher;
    private volatile boolean closed;

    public WatchMonitor(File file, WatchEvent.Kind<?>... events) {
        this(file.toPath(), events);
    }

    public WatchMonitor(String path, WatchEvent.Kind<?>... events) {
        this(Paths.get(path), events);
    }

    public WatchMonitor(Path path, WatchEvent.Kind<?>... events) {
        this(path, 0, events);
    }

    public WatchMonitor(Path path, int maxDepth, WatchEvent.Kind<?>... events) {
        this.path = path;
        this.maxDepth = maxDepth;
        this.events = (events != null && events.length > 0) ? events : EVENTS_ALL;
        init();
    }

    private void init() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("创建 WatchService 失败", e);
        }
        if (!Files.exists(this.path, LinkOption.NOFOLLOW_LINKS)) {
            String last = String.valueOf(this.path.getFileName());
            if (last.contains(".") && !last.toLowerCase().endsWith(".d")) {
                this.filePath = this.path;
                this.path = this.filePath.getParent();
            }
            try {
                Files.createDirectories(this.path);
            } catch (IOException e) {
                throw new RuntimeException("创建监听目录失败: " + this.path, e);
            }
        } else if (Files.isRegularFile(this.path, LinkOption.NOFOLLOW_LINKS)) {
            this.filePath = this.path;
            this.path = this.filePath.getParent();
        }
        this.closed = false;
    }

    public WatchMonitor setWatcher(Watcher watcher) {
        this.watcher = watcher;
        return this;
    }

    public WatchMonitor setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    /**
     * 通过 path 获取 watchKey。
     *
     * @param path path
     * @return 不存在返回 {@code null}
     */
    public WatchKey getWatchKey(Path path) {
        for (Map.Entry<WatchKey, Path> entry : watchKeyPathMap.entrySet()) {
            if (Objects.equals(path, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void run() {
        watch();
    }

    /**
     * 开始监听事件，阻塞当前进程。
     */
    public void watch() {
        watch(this.watcher);
    }

    /**
     * 开始监听事件，阻塞当前进程。
     *
     * @param watcher 监听
     */
    public void watch(Watcher watcher) {
        if (closed) {
            return;
        }
        registerPath(this.path, (null != this.filePath) ? 0 : this.maxDepth);
        while (!closed) {
            WatchKey wk;
            try {
                wk = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                close();
                return;
            }
            Path currentPath = watchKeyPathMap.get(wk);
            for (WatchEvent<?> event : wk.pollEvents()) {
                if (filePath != null && !filePath.getFileName().toString().equals(String.valueOf(event.context()))) {
                    continue;
                }
                dispatch(watcher, event, currentPath);
            }
            wk.reset();
        }
    }

    private void dispatch(Watcher watcher, WatchEvent<?> event, Path currentPath) {
        if (watcher == null) {
            return;
        }
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            watcher.onCreate(event, currentPath);
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            watcher.onModify(event, currentPath);
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            watcher.onDelete(event, currentPath);
        } else if (kind == StandardWatchEventKinds.OVERFLOW) {
            watcher.onOverflow(event, currentPath);
        }
    }

    private void registerPath(Path path, int maxDepth) {
        try {
            WatchKey key = path.register(this.watchService, this.events);
            watchKeyPathMap.put(key, path);
            if (maxDepth > 1) {
                Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        registerPath(dir, 0);
                        return super.postVisitDirectory(dir, exc);
                    }
                });
            }
        } catch (IOException ignore) {
            // 对禁止访问的目录，跳过监听
        }
    }

    @Override
    public void close() {
        this.closed = true;
        if (this.watchService != null) {
            try {
                this.watchService.close();
            } catch (IOException ignore) {
                // 忽略
            }
        }
    }
}
