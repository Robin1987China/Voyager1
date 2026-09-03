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
 * 监控通知日志 JPA 实体。
 */
@Entity
@Table(name = "OPS_MONITOR_NOTIFY_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorNotifyLogEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "monitorId", length = 50)
    private String monitorId;

    @Column(name = "nodeId", length = 50)
    private String nodeId;

    @Column(name = "projectId", length = 30)
    private String projectId;

    @Column(name = "createTime")
    private Long createTime;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status")
    private Integer status;

    @Column(name = "notifyStyle")
    private Integer notifyStyle;

    @Column(name = "notifyStatus")
    private Integer notifyStatus;

    @Column(name = "notifyObject", columnDefinition = "TEXT")
    private String notifyObject;

    @Column(name = "notifyError", columnDefinition = "TEXT")
    private String notifyError;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;
}
