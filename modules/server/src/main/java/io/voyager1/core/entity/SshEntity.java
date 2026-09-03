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
 * SSH 信息 JPA 实体。
 */
@Entity
@Table(name = "INFRA_SSH")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshEntity implements WorkspaceEntity {

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

    @Column(name = "host", length = 100)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "user", length = 100)
    private String user;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "charset", length = 100)
    private String charset;

    @Column(name = "fileDirs", columnDefinition = "TEXT")
    private String fileDirs;

    @Column(name = "privateKey", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "connectType", length = 10)
    private String connectType;

    @Column(name = "notAllowedCommand", columnDefinition = "TEXT")
    private String notAllowedCommand;

    @Column(name = "allowEditSuffix", columnDefinition = "TEXT")
    private String allowEditSuffix;

    @Column(name = "timeout")
    private Integer timeout;

    @Column(name = "\"GROUP\"", length = 50)
    private String group;

    @Column(name = "machineSshId", length = 50)
    private String machineSshId;
}
