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
 * 应用（逻辑服务，跨环境，每个环境部署一份实例）。
 *
 * @since 2026/9/7
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "APPLICATION", nameKey = "应用")
@Data
@Builder
public class ApplicationModel extends BaseDbModel {

    /**
     * 应用名（如 order-svc）
     */
    private String name;

    /**
     * 代码仓库 id
     */
    private String repositoryId;

    /**
     * 构建配置 id（可选）
     */
    private String buildId;

    /**
     * 备注
     */
    private String remark;
}
