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

import io.voyager1.util.PropIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;

/**
 * Pipeline 配置（阶段编排）
 *
 * @since 2026/8/7
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "PIPELINE_CONFIG", nameKey = "Pipeline配置")
@Data
@Builder
public class PipelineConfigModel extends BaseDbModel {

    /**
     * 名称
     */
    private String name;

    /**
     * 关联构建配置 id（应用）
     */
    private String buildId;

    /**
     * 触发配置 JSON：[{type: manual|cron|webhook, cron: "..."}]
     */
    private String triggers;

    /**
     * 阶段配置 JSON：[{id, type: build|exec|publish|approval, params, needs}]
     */
    private String stages;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;

    /**
     * 阶段解析缓存（非持久化）
     */
    @PropIgnore
    private transient Object stageConfig;
}
