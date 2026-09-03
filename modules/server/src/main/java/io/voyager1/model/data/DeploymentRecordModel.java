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
 * 部署记录（环境/版本/方式/操作者）
 *
 * @since 2026/8/7
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "DEPLOYMENT_RECORD", nameKey = "部署记录")
@Data
@Builder
public class DeploymentRecordModel extends BaseDbModel {

    /**
     * 关联构建配置 id（应用）
     */
    private String buildId;

    /**
     * 版本 id
     */
    private String versionId;

    /**
     * 版本号快照
     */
    private String version;

    /**
     * 目标环境（dev/test/prod）
     */
    private String environment;

    /**
     * 触发方式：auto（自动 CD）/ manual（人工 CD）
     */
    private String mode;

    /**
     * 操作者
     */
    private String operator;

    /**
     * 部署状态（0 成功 1 失败 2 进行中）
     */
    private Integer status;

    /**
     * 日志引用
     */
    private String logRef;

    /**
     * 备注
     */
    private String remark;
}
