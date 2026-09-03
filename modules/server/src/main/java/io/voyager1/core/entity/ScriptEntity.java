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
 * 脚本模版 JPA 实体。
 */
@Entity
@Table(name = "OPS_SERVER_SCRIPT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptEntity implements WorkspaceEntity {

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

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "lastRunUser", length = 50)
    private String lastRunUser;

    @Column(name = "autoExecCron", length = 100)
    private String autoExecCron;

    @Column(name = "defArgs", length = 100)
    private String defArgs;

    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "nodeIds", columnDefinition = "TEXT")
    private String nodeIds;

    @Column(name = "triggerToken", length = 200)
    private String triggerToken;

    @Column(name = "createUser", length = 50)
    private String createUser;
}
