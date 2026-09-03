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
 * Pipeline 执行记录
 *
 * @since 2026/8/7
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "PIPELINE_EXECUTE_RECORD", nameKey = "Pipeline执行记录")
@Data
@Builder
public class PipelineExecuteRecordModel extends BaseDbModel {

    /**
     * 关联 Pipeline 配置 id
     */
    private String pipelineId;

    /**
     * 触发类型：manual/cron/webhook
     */
    private String triggerType;

    /**
     * 执行状态：0 等待 1 运行中 2 成功 3 失败 4 已取消 5 等待审批
     */
    private Integer status;

    /**
     * 当前阶段 id
     */
    private String currentStage;

    /**
     * 阶段状态快照 JSON：[{id, type, status, startTime, endTime, logRef}]
     */
    private String stages;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 触发人
     */
    private String operator;
}
