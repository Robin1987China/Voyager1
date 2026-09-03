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

package io.voyager1.build;

/**
 * @since 2023/1/8
 */
public interface IProcessItem {

    /**
     * 流程名称
     *
     * @return 名称
     */
    String name();

    /**
     * 执行流程
     *
     * @return 执行异常消息，存在异常消息需要中断构建
     */
    String execute();
}
