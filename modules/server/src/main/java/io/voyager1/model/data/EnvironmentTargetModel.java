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

package io.voyager1.model.data;

import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 环境-部署目标绑定（环境 → 节点/集群/SSH）。
 *
 * @since 2026/9/7
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ENVIRONMENT_TARGET", nameKey = "环境部署目标")
@Data
@Builder
public class EnvironmentTargetModel extends BaseDbModel {

    /**
     * 环境 id
     */
    private String environmentId;

    /**
     * 目标类型（NODE / K8S / SSH）
     */
    private String targetType;

    /**
     * 目标 id（节点 id / 集群 id / ssh id）
     */
    private String targetId;

    /**
     * 项目 id（NODE 目标时，部署到该节点上的哪个项目）
     */
    private String projectId;

    /**
     * 工作空间 id
     */
    private String workspaceId;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 排序
     */
    private Integer sortValue;
}
