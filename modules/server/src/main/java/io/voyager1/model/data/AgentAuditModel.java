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

package io.voyager1.model.data;

import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 执行审计模型。
 *
 * @since 2026/10/27
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "AGENT_AUDIT", nameKey = "Agent审计")
@Data
@Builder
public class AgentAuditModel extends BaseWorkspaceModel {

    /**
     * Agent 会话 ID
     */
    private String agentSessionId;

    /**
     * 触发本次调用的意图原文（可为空，例如 MCP 直连调用）
     */
    private String intentText;

    /**
     * 工具名（如 environment.list）
     */
    private String toolName;

    /**
     * 工具入参（JSON 字符串）
     */
    private String arguments;

    /**
     * 工具结果（JSON 字符串）
     */
    private String result;

    /**
     * 是否成功：1 成功 / 0 失败
     */
    private Integer success;

    /**
     * 耗时（毫秒）
     */
    private Long durationMillis;

    /**
     * 发起人
     */
    private String operator;

    public boolean isSuccess() {
        return this.success != null && this.success == 1;
    }
}
