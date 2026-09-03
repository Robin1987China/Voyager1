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

package io.voyager1.plugin;

import io.voyager1.util.Tuple;

/**
 * Git处理
 * <br>
 * Created By Hong on 2023/3/31
 *
 */
public interface GitProcess {

    /**
     * 分支和标签列表
     *
     * @return tuple
     * @throws Exception 异常
     */
    Tuple branchAndTagList() throws Exception;

    /**
     * 拉取指定分支
     *
     * @return 拉取结果
     * @throws Exception 异常
     */
    String[] pull() throws Exception;

    /**
     * 拉取指定标签
     *
     * @return 拉取结果
     * @throws Exception 异常
     */
    String[] pullByTag() throws Exception;

}
