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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * AIOps 自愈诊断测试
 *
 * @since 2026/8/25
 */
public class SelfHealServiceTest {

    private final SelfHealService service = new SelfHealService();

    @Test
    public void testDiagnoseProcessDown() {
        JSONObject r = service.diagnose("process_down", "node1");
        Assertions.assertEquals("restart", r.getString("action"));
        Assertions.assertEquals(Boolean.TRUE, r.getBoolean("approval"));
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
    }
}
