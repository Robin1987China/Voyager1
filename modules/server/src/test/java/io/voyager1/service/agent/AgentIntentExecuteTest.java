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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.ApplicationStartTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * 意图执行闭环测试：解析 → 逐工具执行 → 汇总；危险工具停在待审批。
 *
 * @since 2026/10/27
 */
public class AgentIntentExecuteTest extends ApplicationStartTest {

    @Autowired
    private AgentIntentService agentIntentService;
    @Autowired
    private AgentAuditService agentAuditService;

    @Test
    public void testExecuteReadOnlyIntent() {
        String session = "intent-" + UUID.randomUUID();
        JSONObject result = agentIntentService.execute("查看环境列表", session);

        Assertions.assertEquals("completed", result.getString("status"));
        JSONArray steps = result.getJSONArray("steps");
        Assertions.assertEquals(1, steps.size());
        JSONObject step = steps.getJSONObject(0);
        Assertions.assertEquals("environment.list", step.getString("tool"));
        Assertions.assertEquals("success", step.getString("status"));
        Assertions.assertNotNull(step.getString("result"));
        Assertions.assertTrue(step.getString("result").contains("dev"), "应返回真实环境数据");
        Assertions.assertTrue(result.getLongValue("totalDurationMillis") >= 0);
        // 执行同时落审计（意图原文与工具名可追溯）
        Assertions.assertEquals(1, agentAuditService.listBySession(session).size());
        Assertions.assertEquals("查看环境列表", agentAuditService.listBySession(session).get(0).getIntentText());
    }

    @Test
    public void testExecuteDangerousIntentStopsAtApproval() {
        String session = "intent-" + UUID.randomUUID();
        JSONObject result = agentIntentService.execute("把 v1.2.3 部署到 prod", session);

        Assertions.assertEquals("pending_approval", result.getString("status"));
        JSONArray steps = result.getJSONArray("steps");
        Assertions.assertEquals(1, steps.size());
        Assertions.assertEquals("deploy.publish", steps.getJSONObject(0).getString("tool"));
        Assertions.assertEquals("pending_approval", steps.getJSONObject(0).getString("status"));
        // 危险操作未审批前不得继续后续步骤
        Assertions.assertFalse(result.getString("status").equals("completed"));
    }

    @Test
    public void testExecuteFailedStepStops() {
        String session = "intent-" + UUID.randomUUID();
        // log.get 缺 targetId → 工具执行失败，应在该步停止
        JSONObject result = agentIntentService.execute("查看日志并查看监控状态", session);
        Assertions.assertEquals("failed", result.getString("status"));
        JSONArray steps = result.getJSONArray("steps");
        Assertions.assertEquals(1, steps.size(), "失败后不应继续执行后续步骤");
        Assertions.assertEquals("log.get", steps.getJSONObject(0).getString("tool"));
    }
}
