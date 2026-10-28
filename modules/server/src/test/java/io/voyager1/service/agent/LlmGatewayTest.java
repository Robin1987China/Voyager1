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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;
import io.voyager1.configuration.LlmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 网关测试：用 JDK 内置 HttpServer 起本地桩，验证 OpenAI 兼容请求/响应、失败处理与工具转换。
 *
 * @since 2026/10/27
 */
public class LlmGatewayTest {

    private HttpServer server;
    private final List<String> receivedBodies = new ArrayList<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "{}";

    @BeforeEach
    public void startStub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            receivedBodies.add(new String(request, StandardCharsets.UTF_8));
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    public void stopStub() {
        if (server != null) {
            server.stop(0);
        }
    }

    private LlmConfig config() {
        LlmConfig config = new LlmConfig();
        config.setEnabled(true);
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        config.setApiKey("test-key");
        config.setModel("test-model");
        config.setTimeoutSeconds(5);
        return config;
    }

    private static String successBody(String assistantJson) {
        JSONObject message = JSON.parseObject(assistantJson);
        JSONObject choice = new JSONObject();
        choice.put("message", message);
        JSONArray choices = new JSONArray();
        choices.add(choice);
        JSONObject body = new JSONObject();
        body.put("choices", choices);
        return body.toJSONString();
    }

    @Test
    public void testAvailableRequiresConfig() {
        LlmConfig disabled = new LlmConfig();
        Assertions.assertFalse(new OpenAiCompatibleLlmGateway(disabled).available());

        LlmConfig enabledButNoUrl = new LlmConfig();
        enabledButNoUrl.setEnabled(true);
        Assertions.assertFalse(new OpenAiCompatibleLlmGateway(enabledButNoUrl).available());

        Assertions.assertTrue(new OpenAiCompatibleLlmGateway(config()).available());
    }

    @Test
    public void testChatParsesToolCall() {
        responseBody = successBody("{\"role\":\"assistant\",\"content\":null,"
            + "\"tool_calls\":[{\"id\":\"c1\",\"type\":\"function\","
            + "\"function\":{\"name\":\"environment.list\",\"arguments\":\"{}\"}}]}");
        OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway(config());

        JSONArray messages = new JSONArray();
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", "查看环境");
        messages.add(user);

        JSONObject message = gateway.chat(messages, null);
        Assertions.assertEquals("assistant", message.getString("role"));

        JSONObject call = LlmGateway.firstToolCall(message);
        Assertions.assertNotNull(call);
        Assertions.assertEquals("environment.list", call.getString("name"));
        Assertions.assertTrue(call.getJSONObject("arguments").isEmpty());

        // 请求体符合 OpenAI 兼容格式
        Assertions.assertEquals(1, receivedBodies.size());
        JSONObject sent = JSON.parseObject(receivedBodies.get(0));
        Assertions.assertEquals("test-model", sent.getString("model"));
        Assertions.assertEquals(1, sent.getJSONArray("messages").size());
    }

    @Test
    public void testChatThrowsOnHttpError() {
        responseStatus = 500;
        responseBody = "{\"error\":\"boom\"}";
        OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway(config());
        Assertions.assertThrows(IllegalStateException.class, () -> gateway.chat(new JSONArray(), null));
    }

    @Test
    public void testChatThrowsWhenDisabled() {
        LlmConfig disabled = new LlmConfig();
        OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway(disabled);
        Assertions.assertThrows(IllegalStateException.class, () -> gateway.chat(new JSONArray(), null));
        Assertions.assertTrue(receivedBodies.isEmpty(), "禁用时不应发起任何 HTTP 请求");
    }

    @Test
    public void testToOpenAiToolsConversion() {
        JSONObject mcpTools = new JSONObject();
        JSONArray tools = new JSONArray();
        JSONObject tool = new JSONObject();
        tool.put("name", "environment.list");
        tool.put("description", "查询环境");
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        tool.put("inputSchema", schema);
        tools.add(tool);
        mcpTools.put("tools", tools);

        JSONArray converted = LlmGateway.toOpenAiTools(mcpTools);
        Assertions.assertEquals(1, converted.size());
        JSONObject item = converted.getJSONObject(0);
        Assertions.assertEquals("function", item.getString("type"));
        JSONObject fn = item.getJSONObject("function");
        Assertions.assertEquals("environment.list", fn.getString("name"));
        Assertions.assertEquals("查询环境", fn.getString("description"));
        Assertions.assertEquals("object", fn.getJSONObject("parameters").getString("type"));
    }
}
