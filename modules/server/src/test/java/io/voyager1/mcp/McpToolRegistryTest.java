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

package io.voyager1.mcp;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.ApplicationStartTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MCP 工具注册与调用测试
 *
 * @since 2026/8/21
 */
public class McpToolRegistryTest extends ApplicationStartTest {

    @Autowired
    private McpToolRegistry toolRegistry;

    @Test
    public void testInitialize() {
        JSONObject init = toolRegistry.initialize();
        Assertions.assertEquals("2024-11-05", init.getString("protocolVersion"));
        Assertions.assertNotNull(init.getJSONObject("capabilities").getJSONObject("tools"));
        Assertions.assertEquals("voyager1", init.getJSONObject("serverInfo").getString("name"));
    }

    @Test
    public void testListTools() {
        JSONObject result = toolRegistry.listTools();
        Assertions.assertEquals(12, result.getJSONArray("tools").size());
    }

    @Test
    public void testCallToolReadOnly() {
        JSONObject params = new JSONObject();
        params.put("name", "environment.list");
        params.put("arguments", new JSONObject());
        JSONObject resp = toolRegistry.callTool(1, params, "test-session");
        Assertions.assertNotNull(resp.getJSONObject("result"));
        String text = resp.getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text");
        Assertions.assertTrue(text.contains("dev"), "应包含 dev 环境");
    }

    @Test
    public void testCallToolUnknown() {
        JSONObject params = new JSONObject();
        params.put("name", "unknown.tool");
        params.put("arguments", new JSONObject());
        JSONObject resp = toolRegistry.callTool(2, params, "test-session");
        String text = resp.getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text");
        Assertions.assertTrue(text.contains("执行失败"), "未知 tool 应返回执行失败");
    }
}
