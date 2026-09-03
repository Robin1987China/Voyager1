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
 * docker 信息 JPA 实体。
 * <p>
 * 模型层 tlsVerify 为 Boolean，DB 列为 TINYINT(Integer)，copyProperties 负责转换。
 */
@Entity
@Table(name = "INFRA_DOCKER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DockerInfoEntity implements WorkspaceEntity {

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

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "tlsVerify")
    private Integer tlsVerify;

    @Column(name = "heartbeatTimeout")
    private Integer heartbeatTimeout;

    @Column(name = "lastHeartbeatTime")
    private Long lastHeartbeatTime;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "swarmId", length = 50)
    private String swarmId;

    @Column(name = "swarmNodeId", length = 50)
    private String swarmNodeId;

    @Column(name = "registryUsername", length = 255)
    private String registryUsername;

    @Column(name = "registryPassword", length = 255)
    private String registryPassword;

    @Column(name = "registryEmail", length = 255)
    private String registryEmail;

    @Column(name = "registryUrl", length = 255)
    private String registryUrl;

    @Column(name = "machineDockerId", length = 50)
    private String machineDockerId;
}
