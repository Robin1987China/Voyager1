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

import java.util.List;

/**
 * Agent 意图解析测试
 *
 * @since 2026/8/25
 */
public class AgentIntentServiceTest {

    private final AgentIntentService service = new AgentIntentService();

    @Test
    public void testParseDeployIntent() {
        List<JSONObject> steps = service.parseIntent("把 v1.2.3 部署到 test");
        Assertions.assertTrue(steps.stream().anyMatch(s -> "deploy.publish".equals(s.getString("name"))));
        JSONObject deploy = steps.stream().filter(s -> "deploy.publish".equals(s.getString("name"))).findFirst().get();
        Assertions.assertEquals("test", deploy.getJSONObject("arguments").getString("environment"));
    }

    @Test
    public void testParseBuildIntent() {
        List<JSONObject> steps = service.parseIntent("构建项目");
        Assertions.assertTrue(steps.stream().anyMatch(s -> "build.trigger".equals(s.getString("name"))));
    }

    @Test
    public void testParseMonitorIntent() {
        List<JSONObject> steps = service.parseIntent("查看监控状态");
        Assertions.assertTrue(steps.stream().anyMatch(s -> "monitor.list".equals(s.getString("name"))));
    }

    @Test
    public void testParseEmptyIntent() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.parseIntent("你好"));
    }
}
