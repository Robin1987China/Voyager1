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
 * 监控信息 JPA 实体。
 * <p>
 * 模型层 autoRestart/status/alarm 为 Boolean，DB 列为 TINYINT(Integer)，copyProperties 负责布尔&lt;-&gt;整数转换。
 */
@Entity
@Table(name = "OPS_MONITOR")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorEntity implements WorkspaceEntity {

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

    @Column(name = "autoRestart")
    private Integer autoRestart;

    @Column(name = "status")
    private Integer status;

    @Column(name = "alarm")
    private Integer alarm;

    @Column(name = "cycle")
    private Integer cycle;

    @Column(name = "notifyUser", columnDefinition = "TEXT")
    private String notifyUser;

    @Column(name = "projects", columnDefinition = "TEXT")
    private String projects;

    @Column(name = "execCron", length = 100)
    private String execCron;

    @Column(name = "webhook", length = 255)
    private String webhook;

    @Column(name = "useLanguage", length = 20)
    private String useLanguage;

    @Column(name = "silenceTime")
    private Integer silenceTime;

    @Column(name = "silenceUnit", length = 20)
    private String silenceUnit;
}
