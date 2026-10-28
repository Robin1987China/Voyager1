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

package io.voyager1.service.agent;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.ApplicationStartTest;
import io.voyager1.mcp.McpToolRegistry;
import io.voyager1.model.data.AgentAuditModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

/**
 * Agent 执行审计测试：每次 MCP 工具调用都要留痕（含失败与待审批）。
 *
 * @since 2026/10/27
 */
public class AgentAuditServiceTest extends ApplicationStartTest {

    @Autowired
    private McpToolRegistry toolRegistry;
    @Autowired
    private AgentAuditService auditService;

    @Test
    public void testSuccessfulToolCallIsAudited() {
        String session = "audit-" + UUID.randomUUID();
        JSONObject params = new JSONObject();
        params.put("name", "environment.list");
        params.put("arguments", new JSONObject());
        toolRegistry.callTool(1, params, session, "查看环境");

        List<AgentAuditModel> rows = auditService.listBySession(session);
        Assertions.assertEquals(1, rows.size(), "一次工具调用应产生一条审计");
        AgentAuditModel row = rows.get(0);
        Assertions.assertEquals("environment.list", row.getToolName());
        Assertions.assertEquals("查看环境", row.getIntentText());
        Assertions.assertTrue(row.isSuccess());
        Assertions.assertNotNull(row.getDurationMillis());
        Assertions.assertNotNull(row.getArguments());
        Assertions.assertNotNull(row.getResult());
    }

    @Test
    public void testFailedToolCallIsAudited() {
        String session = "audit-" + UUID.randomUUID();
        JSONObject params = new JSONObject();
        params.put("name", "no.such.tool");
        params.put("arguments", new JSONObject());
        toolRegistry.callTool(2, params, session, null);

        List<AgentAuditModel> rows = auditService.listBySession(session);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertFalse(rows.get(0).isSuccess(), "失败调用应标记 success=0");
        Assertions.assertTrue(rows.get(0).getResult().contains("error"));
    }

    @Test
    public void testApprovalToolIsAuditedAsPending() {
        String session = "audit-" + UUID.randomUUID();
        JSONObject params = new JSONObject();
        params.put("name", "ssh.execute");
        JSONObject args = new JSONObject();
        args.put("nodeId", "n1");
        args.put("command", "echo hi");
        params.put("arguments", args);
        toolRegistry.callTool(3, params, session, "执行命令");

        List<AgentAuditModel> rows = auditService.listBySession(session);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertTrue(rows.get(0).isSuccess(), "待审批的调用本身是成功受理");
        Assertions.assertTrue(rows.get(0).getResult().contains("pendingApproval"));
    }
}
