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
import java.nio.file.WatchEvent;

/**
 * 文件/目录监听观察者{@code io.voyager1.util.Watcher}。
 */
public interface Watcher {

    /**
     * 文件创建时执行。
     *
     * @param event       事件
     * @param currentPath 事件发生的当前 Path
     */
    void onCreate(WatchEvent<?> event, Path currentPath);

    /**
     * 文件修改时执行。
     *
     * @param event       事件
     * @param currentPath 事件发生的当前 Path
     */
    void onModify(WatchEvent<?> event, Path currentPath);

    /**
     * 文件删除时执行。
     *
     * @param event       事件
     * @param currentPath 事件发生的当前 Path
     */
    void onDelete(WatchEvent<?> event, Path currentPath);

    /**
     * 事件丢失或出错时执行。
     *
     * @param event       事件
     * @param currentPath 事件发生的当前 Path
     */
    void onOverflow(WatchEvent<?> event, Path currentPath);
}
