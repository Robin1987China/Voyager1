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
 * 发布版本（部署单位）
 *
 * @since 2026/8/7
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "VERSION_INFO", nameKey = "版本信息")
@Data
@Builder
public class VersionModel extends BaseDbModel {

    /**
     * 关联构建配置 id（应用）
     */
    private String buildId;

    /**
     * 构建记录编号（产物来源）
     */
    private Integer buildNumberId;

    /**
     * 版本号（如 v1.2.3）
     */
    private String version;

    /**
     * 状态（VersionStatus）
     */
    private Integer status;

    /**
     * 产物引用（构建产物路径/标识）
     */
    private String artifactRef;

    /**
     * 备注
     */
    private String remark;

    /**
     * 归属分组
     */
    private String groupName;
}
