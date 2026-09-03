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

package io.voyager1.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pipeline 执行记录 JPA 实体。
 * <p>
 * 映射表 {@code PIPELINE_EXECUTE_RECORD}，列名与旧 CSV 建表一致（camelCase）。
 */
@Entity
@Table(name = "PIPELINE_EXECUTE_RECORD")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineExecuteRecordEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "pipelineId", length = 50)
    private String pipelineId;

    @Column(name = "triggerType", length = 20)
    private String triggerType;

    @Column(name = "status")
    private Integer status;

    @Column(name = "currentStage", length = 50)
    private String currentStage;

    @Column(name = "stages", columnDefinition = "TEXT")
    private String stages;

    @Column(name = "startTime")
    private Long startTime;

    @Column(name = "endTime")
    private Long endTime;

    @Column(name = "operator", length = 50)
    private String operator;
}
