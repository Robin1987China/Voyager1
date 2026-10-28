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
import io.voyager1.configuration.AgentAutonomyConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * 自愈执行闭环集成测试：危险动作落审批、熔断后停止、执行留审计。
 *
 * @since 2026/10/27
 */
public class SelfHealExecuteTest extends ApplicationStartTest {

    @Autowired
    private SelfHealService selfHealService;
    @Autowired
    private AgentAuditService agentAuditService;
    @Autowired
    private AgentAutonomyConfig agentAutonomyConfig;

    @Test
    public void testDangerousActionGoesToApproval() {
        String session = "heal-" + UUID.randomUUID();
        JSONObject args = new JSONObject();
        args.put("nodeId", "node-1");
        args.put("command", "echo restart");
        JSONObject r = selfHealService.execute("process_down", "node-1", session, args);

        Assertions.assertEquals("pending_approval", r.getString("status"));
        Assertions.assertNotNull(r.getString("result"));
        Assertions.assertTrue(r.getString("result").contains("待人工审批"));
        // 执行动作同样留审计
        Assertions.assertEquals(1, agentAuditService.listBySession(session).size());
        Assertions.assertEquals("ssh.execute", agentAuditService.listBySession(session).get(0).getToolName());
    }

    @Test
    public void testCircuitOpenStopsExecution() {
        String target = "circuit-" + UUID.randomUUID();
        for (int i = 0; i < agentAutonomyConfig.getSelfHealFailureThreshold(); i++) {
            selfHealService.recordFailure("process_down", target);
        }
        Assertions.assertTrue(selfHealService.isCircuitOpen("process_down", target));

        JSONObject r = selfHealService.execute("process_down", target, "s-" + UUID.randomUUID(), new JSONObject());
        Assertions.assertEquals("circuit_open", r.getString("status"));
        selfHealService.resetCircuit("process_down", target);
    }
}
