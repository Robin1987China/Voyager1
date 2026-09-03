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
 * 流拷贝进度回调，"" {@code io.voyager1.util.StreamProgress}。
 */
public interface StreamProgress {

    /**
     * 开始。
     */
    void start();

    /**
     * 进行中。
     *
     * @param total        总大小，未知为 -1 或 {@link Long#MAX_VALUE}
     * @param progressSize 已完成大小
     */
    void progress(long total, long progressSize);

    /**
     * 结束。
     */
    void finish();
}
