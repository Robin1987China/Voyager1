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
 * 用户操作日志 JPA 实体。
 */
@Entity
@Table(name = "SYS_OPERATION_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOperateLogEntity implements WorkspaceEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "ip", length = 80)
    private String ip;

    @Column(name = "userId", length = 50)
    private String userId;

    @Column(name = "resultMsg", columnDefinition = "TEXT")
    private String resultMsg;

    @Column(name = "optStatus")
    private Integer optStatus;

    @Column(name = "optTime")
    private Long optTime;

    @Column(name = "nodeId", length = 50)
    private String nodeId;

    @Column(name = "dataId", length = 200)
    private String dataId;

    @Column(name = "userAgent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "reqData", columnDefinition = "TEXT")
    private String reqData;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "classFeature", length = 100)
    private String classFeature;

    @Column(name = "methodFeature", length = 100)
    private String methodFeature;

    @Column(name = "dataName", length = 200)
    private String dataName;

    @Column(name = "workspaceName", length = 50)
    private String workspaceName;

    @Column(name = "username", length = 50)
    private String username;
}
