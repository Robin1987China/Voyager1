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
 * 部署记录 JPA 实体（环境/版本/方式/操作者）。
 * <p>
 * 映射表 {@code DEPLOYMENT_RECORD}，列名与旧 CSV 建表一致（camelCase）。
 */
@Entity
@Table(name = "DEPLOYMENT_RECORD")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRecordEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "buildId", length = 50)
    private String buildId;

    @Column(name = "versionId", length = 50)
    private String versionId;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "mode", length = 20)
    private String mode;

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "status")
    private Integer status;

    @Column(name = "logRef", length = 200)
    private String logRef;

    @Column(name = "remark", length = 200)
    private String remark;
}
