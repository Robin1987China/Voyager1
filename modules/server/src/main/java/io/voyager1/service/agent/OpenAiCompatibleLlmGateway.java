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
import io.voyager1.configuration.LlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenAI 兼容协议的 LLM 网关实现（DeepSeek / OpenAI / vLLM / Ollama 等均适用）。
 * <p>
 * 只用 JDK 内置 {@link HttpClient}，不引入额外依赖；未配置时 {@link #available()} 为 false，
 * 调用方自动回落规则版。
 *
 * @since 2026/10/27
 */
@Service
@Slf4j
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final LlmConfig config;

    public OpenAiCompatibleLlmGateway(LlmConfig config) {
        this.config = config;
    }

    @Override
    public boolean available() {
        return config != null && config.callable();
    }

    @Override
    public JSONObject chat(JSONArray messages, JSONArray tools) {
        if (!this.available()) {
            throw new IllegalStateException("LLM 未启用或配置不完整（voyager1.llm.*）");
        }
        JSONObject body = new JSONObject();
        body.put("model", config.getModel());
        body.put("messages", messages == null ? new JSONArray() : messages);
        body.put("temperature", 0);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        String url = this.chatCompletionsUrl();
        long start = System.currentTimeMillis();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(config.getTimeoutSeconds(), 1)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8));
            if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + config.getApiKey().trim());
            }
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(config.getTimeoutSeconds(), 1)))
                .build();
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long cost = System.currentTimeMillis() - start;
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("LLM 调用失败: HTTP " + response.statusCode()
                    + " body=" + abbreviate(response.body()));
            }
            JSONObject json = JSON.parseObject(response.body());
            JSONArray choices = json == null ? null : json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("LLM 响应缺少 choices: " + abbreviate(response.body()));
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message == null) {
                throw new IllegalStateException("LLM 响应缺少 message: " + abbreviate(response.body()));
            }
            log.debug("LLM 调用完成: model={} cost={}ms", config.getModel(), cost);
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM 调用被中断", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("LLM 调用异常: " + e.getMessage(), e);
        }
    }

    private String chatCompletionsUrl() {
        String base = config.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
