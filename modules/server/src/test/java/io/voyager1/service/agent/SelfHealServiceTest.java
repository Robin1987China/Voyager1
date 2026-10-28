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
import io.voyager1.configuration.AgentAutonomyConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * AIOps 自愈诊断 / 熔断 / 中止测试（纯单元，不依赖 Spring 上下文）。
 *
 * @since 2026/8/25
 */
public class SelfHealServiceTest {

    private final AgentAutonomyConfig config = new AgentAutonomyConfig();
    private final SelfHealService service = new SelfHealService(config, null);

    @Test
    public void testDiagnoseProcessDown() {
        JSONObject r = service.diagnose("process_down", "node1");
        Assertions.assertEquals("restart", r.getString("action"));
        Assertions.assertEquals(Boolean.TRUE, r.getBoolean("approval"));
        Assertions.assertEquals(Boolean.FALSE, r.getBoolean("circuitOpen"));
        Assertions.assertEquals(Boolean.TRUE, r.getBoolean("allowed"));
    }

    @Test
    public void testDiagnoseDeployFailed() {
        JSONObject r = service.diagnose("deploy_failed", "v1.2.3");
        Assertions.assertEquals("rollback", r.getString("action"));
    }

    @Test
    public void testDiagnoseUnknown() {
        JSONObject r = service.diagnose("unknown", "x");
        Assertions.assertEquals("manual", r.getString("action"));
        Assertions.assertEquals(Boolean.FALSE, r.getBoolean("allowed"));
    }

    @Test
    public void testCircuitOpensAfterConsecutiveFailures() {
        String target = "circuit-" + System.nanoTime();
        int threshold = config.getSelfHealFailureThreshold();
        for (int i = 0; i < threshold; i++) {
            Assertions.assertFalse(service.isCircuitOpen("process_down", target), "未达阈值不应熔断");
            service.recordFailure("process_down", target);
        }
        Assertions.assertTrue(service.isCircuitOpen("process_down", target));
        JSONObject r = service.diagnose("process_down", target);
        Assertions.assertEquals(Boolean.TRUE, r.getBoolean("circuitOpen"));
        Assertions.assertEquals("manual", r.getString("action"));
    }

    @Test
    public void testRecordSuccessResetsCircuit() {
        String target = "reset-" + System.nanoTime();
        service.recordFailure("process_down", target);
        service.recordFailure("process_down", target);
        Assertions.assertEquals(2, service.failures("process_down", target));
        service.recordSuccess("process_down", target);
        Assertions.assertEquals(0, service.failures("process_down", target));
    }

    @Test
    public void testAbortStopsExecute() {
        String session = "session-" + System.nanoTime();
        service.abort(session);
        JSONObject r = service.execute("process_down", "node1", session, new JSONObject());
        Assertions.assertEquals("aborted", r.getString("status"));
    }

    @Test
    public void testNotAllowedAlertTypeSkipsExecution() {
        JSONObject r = service.execute("unknown", "x", "s-" + System.nanoTime(), new JSONObject());
        Assertions.assertEquals("not_allowed", r.getString("status"));
    }

    @Test
    public void testDiagnoseUsesLlmWhenAvailable() {
        LlmGateway stub = new LlmGateway() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public com.alibaba.fastjson2.JSONObject chat(com.alibaba.fastjson2.JSONArray messages,
                                                         com.alibaba.fastjson2.JSONArray tools) {
                JSONObject msg = new JSONObject();
                msg.put("role", "assistant");
                msg.put("content", "{\"rootCause\":\"LLM 判定进程崩溃\",\"action\":\"restart\","
                    + "\"tool\":\"ssh.execute\",\"approval\":true,\"confidence\":0.82}");
                return msg;
            }
        };
        SelfHealService llmService = new SelfHealService(new AgentAutonomyConfig(), stub);
        JSONObject r = llmService.diagnose("process_down", "node-llm");
        Assertions.assertEquals("LLM 判定进程崩溃", r.getString("rootCause"));
        Assertions.assertEquals("restart", r.getString("action"));
        Assertions.assertEquals("llm", r.getString("source"));
        Assertions.assertEquals(0.82, r.getDoubleValue("confidence"), 0.0001);
    }

    @Test
    public void testDiagnoseFallsBackWhenLlmOutputInvalid() {
        LlmGateway bad = new LlmGateway() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public com.alibaba.fastjson2.JSONObject chat(com.alibaba.fastjson2.JSONArray messages,
                                                         com.alibaba.fastjson2.JSONArray tools) {
                JSONObject msg = new JSONObject();
                msg.put("role", "assistant");
                msg.put("content", "这不是 JSON");
                return msg;
            }
        };
        SelfHealService llmService = new SelfHealService(new AgentAutonomyConfig(), bad);
        JSONObject r = llmService.diagnose("process_down", "node-bad");
        Assertions.assertEquals("restart", r.getString("action"));
        Assertions.assertEquals("rules", r.getString("source"));
    }
}
