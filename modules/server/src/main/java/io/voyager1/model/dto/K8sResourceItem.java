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

package io.voyager1.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * K8s 资源统一视图（列表用）
 *
 * @since 2026/8/14
 */
@Data
@Builder
public class K8sResourceItem {

    /**
     * 资源名称
     */
    private String name;

    /**
     * 命名空间（集群级资源为空）
     */
    private String namespace;

    /**
     * 资源类型标识（pods/deployments/...）
     */
    private String type;

    /**
     * 资源 Kind（Pod/Deployment/...）
     */
    private String kind;

    /**
     * 状态文本
     */
    private String status;

    /**
     * 就绪文本（如 1/1）
     */
    private String ready;

    /**
     * 创建时间（ISO8601 字符串）
     */
    private String createdAt;
}
