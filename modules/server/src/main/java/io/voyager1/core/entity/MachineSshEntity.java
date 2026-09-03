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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 机器 SSH 信息 JPA 实体。
 */
@Entity
@Table(name = "INFRA_MACHINE_SSH")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineSshEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "groupName", length = 50)
    private String groupName;

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

    @Column(name = "privateKey", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "connectType", length = 10)
    private String connectType;

    @Column(name = "timeout")
    private Integer timeout;

    @Column(name = "status")
    private Integer status;

    @Column(name = "statusMsg", columnDefinition = "TEXT")
    private String statusMsg;

    @Column(name = "allowEditSuffix", columnDefinition = "TEXT")
    private String allowEditSuffix;

    @Column(name = "osName", length = 50)
    private String osName;

    @Column(name = "hostName", length = 255)
    private String hostName;

    @Column(name = "osLoadAverage", length = 100)
    private String osLoadAverage;

    @Column(name = "osSystemUptime")
    private Long osSystemUptime;

    @Column(name = "osVersion", length = 255)
    private String osVersion;

    @Column(name = "osCpuCores")
    private Integer osCpuCores;

    @Column(name = "osMoneyTotal")
    private Long osMoneyTotal;

    @Column(name = "osFileStoreTotal")
    private Long osFileStoreTotal;

    @Column(name = "osCpuIdentifierName", length = 255)
    private String osCpuIdentifierName;

    @Column(name = "osOccupyCpu")
    private Double osOccupyCpu;

    @Column(name = "osOccupyMemory")
    private Double osOccupyMemory;

    @Column(name = "osMaxOccupyDisk")
    private Double osMaxOccupyDisk;

    @Column(name = "osMaxOccupyDiskName", length = 255)
    private String osMaxOccupyDiskName;

    @Column(name = "javaVersion", length = 255)
    private String javaVersion;

    @Column(name = "voyager1AgentPid")
    private Integer voyager1AgentPid;

    @Column(name = "dockerInfo", length = 255)
    private String dockerInfo;
}
