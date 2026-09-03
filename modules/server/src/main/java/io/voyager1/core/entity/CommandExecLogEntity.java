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
 * 命令执行记录 JPA 实体。
 */
@Entity
@Table(name = "OPS_COMMAND_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandExecLogEntity implements WorkspaceEntity {

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

    @Column(name = "commandId", length = 50)
    private String commandId;

    @Column(name = "sshId", length = 50)
    private String sshId;

    @Column(name = "batchId", length = 50)
    private String batchId;

    @Column(name = "commandName", length = 100)
    private String commandName;

    @Column(name = "sshName", length = 100)
    private String sshName;

    @Column(name = "status")
    private Integer status;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "triggerExecType")
    private Integer triggerExecType;

    @Column(name = "exitCode")
    private Integer exitCode;
}
