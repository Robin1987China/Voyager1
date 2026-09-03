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

package io.voyager1.transport;

import java.util.Map;

/**
 * @since 2022/12/23
 */
public interface IUrlItem {

    /**
     * 请求路径
     *
     * @return path
     */
    String path();

    /**
     * 请求超时时间
     * 单位秒
     *
     * @return 超时时间
     */
    Integer timeout();

    /**
     * 当前工作空间id
     *
     * @return 工作空间
     */
    String workspaceId();

    /**
     * 请求类型
     *
     * @return contentType
     */
    DataContentType contentType();

    /**
     * 请求头
     *
     * @return 请求头
     */
    Map<String, String> header();
}
