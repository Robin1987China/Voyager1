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
 * 云账号 JPA 实体（Phase 1 PoC）
 * <p>
 * 映射旧表 CLOUD_ACCOUNT，列名与旧 CSV 建表保持一致（camelCase）。
 * 用于验证新持久层（JPA）与旧存储层（JdbcTemplate）对同一张表读写一致。
 */
@Entity
@Table(name = "CLOUD_ACCOUNT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudAccountEntity {

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

    @Column(name = "vendor", length = 20)
    private String vendor;

    @Column(name = "accessKey", columnDefinition = "TEXT")
    private String accessKey;

    @Column(name = "secretKey", columnDefinition = "TEXT")
    private String secretKey;

    @Column(name = "extraKey", columnDefinition = "TEXT")
    private String extraKey;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "remark", length = 200)
    private String remark;
}
