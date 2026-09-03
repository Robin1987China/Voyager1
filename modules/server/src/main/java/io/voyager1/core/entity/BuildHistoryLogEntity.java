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
 * 构建历史记录 JPA 实体。
 */
@Entity
@Table(name = "CI_BUILD_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildHistoryLogEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "buildDataId", length = 50)
    private String buildDataId;

    @Column(name = "buildNumberId")
    private Integer buildNumberId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "startTime")
    private Long startTime;

    @Column(name = "endTime")
    private Long endTime;

    @Column(name = "resultDirFile", length = 200)
    private String resultDirFile;

    @Column(name = "releaseMethod")
    private Integer releaseMethod;

    @Column(name = "buildName", length = 100)
    private String buildName;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "triggerBuildType")
    private Integer triggerBuildType;

    @Column(name = "buildRemark", length = 255)
    private String buildRemark;

    @Column(name = "extraData", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "buildEnvCache", columnDefinition = "TEXT")
    private String buildEnvCache;

    @Column(name = "resultFileSize")
    private Long resultFileSize;

    @Column(name = "buildLogFileSize")
    private Long buildLogFileSize;

    @Column(name = "statusMsg", columnDefinition = "TEXT")
    private String statusMsg;

    @Column(name = "fromBuildNumberId")
    private Integer fromBuildNumberId;

    @Column(name = "repositoryLastCommitId", length = 255)
    private String repositoryLastCommitId;

    @Column(name = "repositoryLastCommitMsg", length = 255)
    private String repositoryLastCommitMsg;
}
