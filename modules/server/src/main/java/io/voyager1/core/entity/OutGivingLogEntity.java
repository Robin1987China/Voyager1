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

import io.voyager1.core.jpa.WorkspaceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分发日志 JPA 实体。
 */
@Entity
@Table(name = "OPS_RELEASE_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutGivingLogEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "outGivingId", length = 50)
    private String outGivingId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "startTime")
    private Long startTime;

    @Column(name = "endTime")
    private Long endTime;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "nodeId", length = 100)
    private String nodeId;

    @Column(name = "projectId", length = 100)
    private String projectId;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "fileSize")
    private Long fileSize;

    @Column(name = "progressSize")
    private Long progressSize;

    @Column(name = "mode", length = 50)
    private String mode;

    @Column(name = "modeData", length = 500)
    private String modeData;
}
