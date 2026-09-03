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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;

/**
 * Agent 审批记录（MCP 危险操作的人工审批闸门）
 *
 * @since 2026/8/25
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "AGENT_APPROVAL", nameKey = "Agent审批")
@Data
@Builder
public class AgentApprovalModel extends BaseWorkspaceModel {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_EXPIRED = 3;

    /**
     * Agent 会话 ID
     */
    private String agentSessionId;

    /**
     * 工具名（如 deploy.publish）
     */
    private String toolName;

    /**
     * 工具参数（JSON 字符串）
     */
    private String arguments;

    /**
     * 状态：0 待审批 / 1 已批准 / 2 已拒绝 / 3 已超时
     */
    private Integer status;

    /**
     * 发起人
     */
    private String operator;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 执行结果（JSON 字符串）
     */
    private String result;

    /**
     * 备注
     */
    private String remark;

    /**
     * 超时时间（毫秒），超过自动拒绝
     */
    private Long expireTimeMillis;
}
