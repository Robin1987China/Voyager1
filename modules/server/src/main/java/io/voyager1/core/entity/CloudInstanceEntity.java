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
 * 云实例 JPA 实体（可导入为机器部署目标）。
 * <p>
 * 映射表 {@code CLOUD_INSTANCE}，列名与旧 CSV 建表一致（camelCase）。
 */
@Entity
@Table(name = "CLOUD_INSTANCE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudInstanceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "accountId", length = 50)
    private String accountId;

    @Column(name = "instanceId", length = 50)
    private String instanceId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "publicIp", length = 50)
    private String publicIp;

    @Column(name = "privateIp", length = 50)
    private String privateIp;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "groupName", length = 50)
    private String groupName;

    @Column(name = "machineId", length = 50)
    private String machineId;

    @Column(name = "regionId", length = 50)
    private String regionId;

    @Column(name = "zoneId", length = 50)
    private String zoneId;

    @Column(name = "instanceType", length = 50)
    private String instanceType;

    @Column(name = "cpu")
    private Integer cpu;

    @Column(name = "memory")
    private Integer memory;

    @Column(name = "osName", length = 200)
    private String osName;

    @Column(name = "expireTime", length = 50)
    private String expireTime;

    @Column(name = "chargeType", length = 30)
    private String chargeType;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;
}
