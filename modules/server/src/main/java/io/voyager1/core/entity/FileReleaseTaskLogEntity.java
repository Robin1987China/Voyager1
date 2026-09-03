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
 * 文件发布任务记录 JPA 实体。
 */
@Entity
@Table(name = "OPS_FILE_RELEASE_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileReleaseTaskLogEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "taskId", length = 50)
    private String taskId;

    @Column(name = "fileId", length = 50)
    private String fileId;

    @Column(name = "taskDataId", length = 50)
    private String taskDataId;

    @Column(name = "taskType")
    private Integer taskType;

    @Column(name = "status")
    private Integer status;

    @Column(name = "statusMsg", columnDefinition = "TEXT")
    private String statusMsg;

    @Column(name = "releasePath", length = 255)
    private String releasePath;

    @Column(name = "beforeScript", columnDefinition = "TEXT")
    private String beforeScript;

    @Column(name = "afterScript", columnDefinition = "TEXT")
    private String afterScript;

    @Column(name = "fileType")
    private Integer fileType;
}
