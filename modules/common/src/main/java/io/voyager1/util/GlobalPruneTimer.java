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

/**
 * 全局定时清理器，"" {@code io.voyager1.util.GlobalPruneTimer}。
 *
 * <p>当前实现不负责调度（实际清理由各缓存的 {@code schedulePrune} 自行安排），仅保留单例形式。
 */
public class GlobalPruneTimer {

    private static final GlobalPruneTimer INSTANCE = new GlobalPruneTimer();

    private GlobalPruneTimer() {
    }

    public static GlobalPruneTimer getInstance() {
        return INSTANCE;
    }
}
