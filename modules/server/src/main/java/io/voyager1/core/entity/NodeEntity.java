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
 * 节点信息 JPA 实体。
 */
@Entity
@Table(name = "INFRA_NODE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeEntity implements WorkspaceEntity {

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

    @Column(name = "url", length = 100)
    private String url;

    @Column(name = "loginName", length = 100)
    private String loginName;

    @Column(name = "loginPwd", length = 100)
    private String loginPwd;

    @Column(name = "protocol", length = 10)
    private String protocol;

    @Column(name = "openStatus")
    private Integer openStatus;

    @Column(name = "timeOut")
    private Integer timeOut;

    @Column(name = "sshId", length = 50)
    private String sshId;

    @Column(name = "\"GROUP\"", length = 50)
    private String group;

    @Column(name = "httpProxy", length = 200)
    private String httpProxy;

    @Column(name = "httpProxyType", length = 20)
    private String httpProxyType;

    @Column(name = "sortValue")
    private Float sortValue;

    @Column(name = "machineId", length = 50)
    private String machineId;

    @Column(name = "voyager1ProjectCount")
    private Integer voyager1ProjectCount;

    @Column(name = "voyager1ScriptCount")
    private Integer voyager1ScriptCount;
}
