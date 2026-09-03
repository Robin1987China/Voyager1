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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;

/**
 * 标签分摊规则（标签 → 项目）
 *
 * @since 2026/8/31
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "COST_TAG_RULE", nameKey = "标签分摊规则")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostTagRuleModel extends BaseDbModel {

    /**
     * 云厂商（空 = 所有厂商）
     */
    private String vendor;

    /**
     * 标签 key
     */
    private String tagKey;

    /**
     * 标签 value
     */
    private String tagValue;

    /**
     * 项目 ID
     */
    private String projectId;

    /**
     * 项目名称
     */
    private String projectName;
}
