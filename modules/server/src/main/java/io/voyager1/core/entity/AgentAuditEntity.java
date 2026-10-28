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
 * Agent 执行审计 JPA 实体：一次工具调用一条记录。
 *
 * @since 2026/10/27
 */
@Entity
@Table(name = "AGENT_AUDIT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentAuditEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "agentSessionId", length = 50)
    private String agentSessionId;

    @Column(name = "intentText", columnDefinition = "TEXT")
    private String intentText;

    @Column(name = "toolName", length = 50)
    private String toolName;

    @Column(name = "arguments", columnDefinition = "TEXT")
    private String arguments;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    /**
     * 1 成功 / 0 失败
     */
    @Column(name = "success")
    private Integer success;

    @Column(name = "durationMillis")
    private Long durationMillis;

    @Column(name = "operator", length = 50)
    private String operator;
}
