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
 * 机器节点信息 JPA 实体。
 * <p>
 * 模型层 templateNode 为 Boolean，DB 列为 TINYINT(Integer)。
 */
@Entity
@Table(name = "INFRA_MACHINE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineNodeEntity {

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

    @Column(name = "hostName", length = 255)
    private String hostName;

    @Column(name = "hostIpv4s", columnDefinition = "TEXT")
    private String hostIpv4s;

    @Column(name = "osLoadAverage", length = 100)
    private String osLoadAverage;

    @Column(name = "osSystemUptime")
    private Long osSystemUptime;

    @Column(name = "osVersion", length = 255)
    private String osVersion;

    @Column(name = "osHardwareVersion", length = 255)
    private String osHardwareVersion;

    @Column(name = "osCpuCores")
    private Integer osCpuCores;

    @Column(name = "osMoneyTotal")
    private Long osMoneyTotal;

    @Column(name = "osFileStoreTotal")
    private Long osFileStoreTotal;

    @Column(name = "osCpuIdentifierName", length = 255)
    private String osCpuIdentifierName;

    @Column(name = "osName", length = 50)
    private String osName;

    @Column(name = "status")
    private Integer status;

    @Column(name = "statusMsg", columnDefinition = "TEXT")
    private String statusMsg;

    @Column(name = "transportMode")
    private Integer transportMode;

    @Column(name = "voyager1Url", length = 100)
    private String voyager1Url;

    @Column(name = "voyager1Username", length = 100)
    private String voyager1Username;

    @Column(name = "voyager1Password", length = 100)
    private String voyager1Password;

    @Column(name = "voyager1Protocol", length = 10)
    private String voyager1Protocol;

    @Column(name = "voyager1Timeout")
    private Integer voyager1Timeout;

    @Column(name = "voyager1HttpProxy", length = 200)
    private String voyager1HttpProxy;

    @Column(name = "voyager1HttpProxyType", length = 20)
    private String voyager1HttpProxyType;

    @Column(name = "voyager1Version", length = 50)
    private String voyager1Version;

    @Column(name = "voyager1Uptime")
    private Long voyager1Uptime;

    @Column(name = "voyager1BuildTime", length = 50)
    private String voyager1BuildTime;

    @Column(name = "voyager1ProjectCount")
    private Integer voyager1ProjectCount;

    @Column(name = "voyager1ScriptCount")
    private Integer voyager1ScriptCount;

    @Column(name = "networkDelay")
    private Integer networkDelay;

    @Column(name = "javaVersion", length = 50)
    private String javaVersion;

    @Column(name = "jvmTotalMemory")
    private Long jvmTotalMemory;

    @Column(name = "jvmFreeMemory")
    private Long jvmFreeMemory;

    @Column(name = "osOccupyCpu")
    private Double osOccupyCpu;

    @Column(name = "osOccupyMemory")
    private Double osOccupyMemory;

    @Column(name = "osOccupyDisk")
    private Double osOccupyDisk;

    @Column(name = "templateNode")
    private Integer templateNode;

    @Column(name = "installId", length = 50)
    private String installId;

    @Column(name = "osSwapTotal")
    private Long osSwapTotal;

    @Column(name = "osVirtualMax")
    private Long osVirtualMax;

    @Column(name = "transportEncryption")
    private Integer transportEncryption;

    @Column(name = "extendInfo", columnDefinition = "TEXT")
    private String extendInfo;
}
