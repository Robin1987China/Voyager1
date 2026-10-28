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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Agent 审批闸门测试
 *
 * @since 2026/8/25
 */
public class AgentApprovalServiceTest extends ApplicationStartTest {

    @Autowired
    private AgentApprovalService agentApprovalService;

    @Test
    public void testCreateAndReject() {
        String id = agentApprovalService.createApproval("test-session", "deploy.publish", new JSONObject(), "admin");
        Assertions.assertNotNull(id);
        agentApprovalService.reject(id, "admin", "测试拒绝");
        // 已拒绝后再次操作应报错
        Assertions.assertThrows(IllegalStateException.class, () -> agentApprovalService.reject(id, "admin", "再次拒绝"));
    }

    @Test
    public void testCreateAndApproveReadOnly() {
        String id = agentApprovalService.createApproval("test-session", "monitor.list", new JSONObject(), "admin");
        JSONObject resp = agentApprovalService.approve(id, "admin");
        Assertions.assertEquals("approved", resp.getString("status"));
        Assertions.assertNotNull(resp.getString("result"));
    }

    @Test
    public void testRejectExpiredOnlyTouchesExpiredOnes() {
        String expiredId = agentApprovalService.createApproval("expired-session", "deploy.publish", new JSONObject(), "admin");
        String aliveId = agentApprovalService.createApproval("alive-session", "deploy.publish", new JSONObject(), "admin");

        // 把其中一条的过期时间改到过去
        io.voyager1.model.data.AgentApprovalModel expired = agentApprovalService.getByKey(expiredId);
        expired.setExpireTimeMillis(System.currentTimeMillis() - 1000L);
        agentApprovalService.updateById(expired);

        int rejected = agentApprovalService.rejectExpired();
        Assertions.assertTrue(rejected >= 1, "至少应拒绝本次构造的过期单");

        Assertions.assertEquals(io.voyager1.model.data.AgentApprovalModel.STATUS_EXPIRED,
            agentApprovalService.getByKey(expiredId).getStatus().intValue());
        Assertions.assertEquals("审批超时自动拒绝", agentApprovalService.getByKey(expiredId).getRemark());
        Assertions.assertEquals(io.voyager1.model.data.AgentApprovalModel.STATUS_PENDING,
            agentApprovalService.getByKey(aliveId).getStatus().intValue(), "未过期的不应被拒绝");

        // 清理未过期的那条，避免污染后续测试
        agentApprovalService.reject(aliveId, "admin", "测试清理");
    }
}
