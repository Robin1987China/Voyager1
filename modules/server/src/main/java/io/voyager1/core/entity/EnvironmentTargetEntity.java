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
 * 环境-部署目标 JPA 实体。
 * <p>
 * 映射表 {@code ENVIRONMENT_TARGET}，列名与 CSV 建表一致（camelCase）。
 */
@Entity
@Table(name = "ENVIRONMENT_TARGET")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentTargetEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "environmentId", length = 50)
    private String environmentId;

    @Column(name = "targetType", length = 20)
    private String targetType;

    @Column(name = "targetId", length = 100)
    private String targetId;

    @Column(name = "projectId", length = 100)
    private String projectId;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "enabled")
    private Integer enabled;

    @Column(name = "sortValue")
    private Integer sortValue;
}
