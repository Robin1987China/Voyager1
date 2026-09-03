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
 * 构建信息 JPA 实体。
 */
@Entity
@Table(name = "CI_BUILD")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildInfoEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "repositoryId", length = 50)
    private String repositoryId;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "buildId")
    private Integer buildId;

    @Column(name = "\"GROUP\"", length = 50)
    private String group;

    @Column(name = "branchName", length = 50)
    private String branchName;

    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    @Column(name = "resultDirFile", length = 200)
    private String resultDirFile;

    @Column(name = "releaseMethod")
    private Integer releaseMethod;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "status")
    private Integer status;

    @Column(name = "triggerToken", length = 100)
    private String triggerToken;

    @Column(name = "extraData", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "releaseMethodDataId", columnDefinition = "TEXT")
    private String releaseMethodDataId;

    @Column(name = "branchTagName", length = 50)
    private String branchTagName;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "webhook", length = 255)
    private String webhook;

    @Column(name = "autoBuildCron", length = 100)
    private String autoBuildCron;

    @Column(name = "buildMode")
    private Integer buildMode;

    @Column(name = "repositoryLastCommitId", length = 255)
    private String repositoryLastCommitId;

    @Column(name = "sortValue")
    private Float sortValue;

    @Column(name = "buildEnvParameter", columnDefinition = "TEXT")
    private String buildEnvParameter;

    @Column(name = "aliasCode", length = 50)
    private String aliasCode;

    @Column(name = "statusMsg", columnDefinition = "TEXT")
    private String statusMsg;

    @Column(name = "resultKeepDay")
    private Integer resultKeepDay;

    @Column(name = "createUser", length = 50)
    private String createUser;
}
