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
 * 项目信息缓存 JPA 实体。
 * <p>
 * 模型层 autoStart/outGivingProject 为 Boolean，DB 列为 TINYINT(Integer)，copyProperties 负责转换。
 */
@Entity
@Table(name = "CI_PROJECT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInfoCacheEntity implements WorkspaceEntity {

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

    @Column(name = "projectId", length = 50)
    private String projectId;

    @Column(name = "nodeId", length = 50)
    private String nodeId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "mainClass", length = 100)
    private String mainClass;

    @Column(name = "lib", length = 100)
    private String lib;

    @Column(name = "whitelistDirectory", length = 100)
    private String whitelistDirectory;

    @Column(name = "logPath", length = 100)
    private String logPath;

    @Column(name = "jvm", columnDefinition = "TEXT")
    private String jvm;

    @Column(name = "args", columnDefinition = "TEXT")
    private String args;

    @Column(name = "javaCopyItemList", columnDefinition = "TEXT")
    private String javaCopyItemList;

    @Column(name = "token", length = 255)
    private String token;

    @Column(name = "runMode", length = 20)
    private String runMode;

    @Column(name = "outGivingProject")
    private Integer outGivingProject;

    @Column(name = "javaExtDirsCp", columnDefinition = "TEXT")
    private String javaExtDirsCp;

    @Column(name = "sortValue")
    private Float sortValue;

    @Column(name = "triggerToken", length = 100)
    private String triggerToken;

    @Column(name = "\"GROUP\"", length = 50)
    private String group;

    @Column(name = "dslContent", columnDefinition = "TEXT")
    private String dslContent;

    @Column(name = "autoStart")
    private Integer autoStart;

    @Column(name = "nodeName", length = 50)
    private String nodeName;

    @Column(name = "workspaceName", length = 50)
    private String workspaceName;
}
