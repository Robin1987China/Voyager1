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
 * SSH 终端操作记录 JPA 实体。
 */
@Entity
@Table(name = "INFRA_SSH_SESSION_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshTerminalExecuteLogEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "ip", length = 80)
    private String ip;

    @Column(name = "userId", length = 30)
    private String userId;

    @Column(name = "userAgent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "commands", columnDefinition = "TEXT")
    private String commands;

    @Column(name = "sshId", length = 50)
    private String sshId;

    @Column(name = "sshName", length = 50)
    private String sshName;

    @Column(name = "refuse")
    private Integer refuse;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "machineSshId", length = 50)
    private String machineSshId;

    @Column(name = "machineSshName", length = 50)
    private String machineSshName;
}
