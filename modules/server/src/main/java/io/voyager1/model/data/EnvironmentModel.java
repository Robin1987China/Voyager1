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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;

/**
 * 环境定义（dev/test/prod）
 *
 * @since 2026/8/8
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ENVIRONMENT_INFO", nameKey = "环境信息")
@Data
@Builder
public class EnvironmentModel extends BaseDbModel {

    /**
     * 环境名称（dev/test/prod）
     */
    private String name;

    /**
     * 排序
     */
    private Integer sortValue;

    /**
     * 是否启用
     */
    private Integer enabled;
}
