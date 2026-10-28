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
import io.voyager1.mcp.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 意图解析服务（LLM 网关的规则降级版）
 *
 * <p>把自然语言意图（如「把 v1.2.3 部署到 test」）拆解为 MCP 工具调用序列，
 * 并可按序执行（{@link #execute}）。规则版基于关键词匹配，LLM 实现作为可插拔扩展。</p>
 *
 * @since 2026/8/25
 */
@Service
@Slf4j
public class AgentIntentService {

    private static final Pattern ENV_PATTERN = Pattern.compile("(dev|test|prod|staging|production)");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(v\\d+\\.\\d+\\.\\d+[\\w.-]*)");

    private final McpToolRegistry mcpToolRegistry;
    private final LlmGateway llmGateway;

    @Autowired
    public AgentIntentService(McpToolRegistry mcpToolRegistry, LlmGateway llmGateway) {
        this.mcpToolRegistry = mcpToolRegistry;
        this.llmGateway = llmGateway;
    }

    /**
     * 解析意图 → 工具调用序列。
     * <p>
     * 已配置 LLM 时走 function calling（工具 schema 直接取 MCP 注册表）；
     * 未配置、超时、无工具调用或解析异常时**回落到规则版**，保证可用性。
     * </p>
     *
     * @param intent 自然语言意图
     * @return 工具调用序列（按执行顺序）
     */
    public List<JSONObject> parseIntent(String intent) {
        // 防御性校验：controller 已用 @ValidatorItem 拦截，这里兜底避免空指针
        org.springframework.util.Assert.hasText(intent, "意图描述不能为空");
        if (llmGateway != null && llmGateway.available()) {
            try {
                JSONArray tools = mcpToolRegistry == null ? null : LlmGateway.toOpenAiTools(mcpToolRegistry.listTools());
                JSONArray messages = new JSONArray();
                messages.add(this.systemMessage());
                messages.add(this.userMessage(intent));
                JSONObject message = llmGateway.chat(messages, tools);
                JSONArray calls = LlmGateway.allToolCalls(message);
                if (!calls.isEmpty()) {
                    log.info("意图解析(LLM): {} -> {} 个步骤", intent, calls.size());
                    return calls.toJavaList(JSONObject.class);
                }
                log.warn("LLM 未返回工具调用，回落规则解析: {}", intent);
            } catch (Exception e) {
                log.warn("LLM 意图解析失败，回落规则解析: {}", e.getMessage());
            }
        }
        return this.parseIntentByRules(intent);
    }

    /**
     * 规则版意图解析（关键词匹配），作为 LLM 不可用时的兜底。
     */
    public List<JSONObject> parseIntentByRules(String intent) {
        String lower = intent.toLowerCase();
        List<JSONObject> steps = new ArrayList<>();
        String env = extractEnv(intent);
        String version = extractVersion(intent);

        if (containsAny(lower, "构建", "build")) {
            steps.add(tool("build.trigger", new JSONObject()));
        }
        if (containsAny(lower, "部署", "deploy", "发布", "release")) {
            JSONObject args = new JSONObject();
            if (version != null) {
                args.put("versionId", version);
            }
            if (env != null) {
                args.put("environment", env);
            }
            steps.add(tool("deploy.publish", args));
        }
        if (containsAny(lower, "环境", "environment")) {
            steps.add(tool("environment.list", new JSONObject()));
        }
        if (containsAny(lower, "日志", "log")) {
            steps.add(tool("log.get", new JSONObject()));
        }
        if (containsAny(lower, "监控", "monitor", "状态")) {
            steps.add(tool("monitor.list", new JSONObject()));
        }
        if (containsAny(lower, "k8s", "kubernetes", "容器")) {
            steps.add(tool("k8s.resourceList", new JSONObject()));
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("无法解析意图，请使用更明确的描述（如「把 v1.2.3 部署到 test」）");
        }
        log.info("意图解析: {} -> {} 个步骤", intent, steps.size());
        return steps;
    }

    /**
     * 解析意图并**逐工具执行**，汇总每步结果。
     * <p>
     * 安全约束：遇到需审批的危险工具即停止后续步骤（返回 {@code pending_approval}），
     * 避免在未获人工批准时继续动作生产环境。
     * </p>
     *
     * @param intent    自然语言意图
     * @param sessionId Agent 会话 ID（用于审计与审批关联）
     * @return {@code {intent, sessionId, status, steps[], totalDurationMillis}}
     */
    public JSONObject execute(String intent, String sessionId) {
        List<JSONObject> plan = this.parseIntent(intent);
        JSONArray steps = new JSONArray();
        long totalStart = System.currentTimeMillis();
        String status = "completed";
        for (int i = 0; i < plan.size(); i++) {
            JSONObject step = plan.get(i);
            String name = step.getString("name");
            JSONObject args = step.getJSONObject("arguments");
            JSONObject params = new JSONObject();
            params.put("name", name);
            params.put("arguments", args);
            long start = System.currentTimeMillis();
            JSONObject resp = mcpToolRegistry.callTool(i + 1, params, sessionId, intent);
            String text = extractText(resp);
            JSONObject stepResult = new JSONObject();
            stepResult.put("tool", name);
            stepResult.put("arguments", args);
            stepResult.put("result", text);
            stepResult.put("durationMillis", System.currentTimeMillis() - start);
            if (text != null && text.contains("待人工审批")) {
                stepResult.put("status", "pending_approval");
                status = "pending_approval";
            } else if (text != null && text.startsWith("执行失败")) {
                stepResult.put("status", "failed");
                status = "failed";
            } else {
                stepResult.put("status", "success");
            }
            steps.add(stepResult);
            if (!"success".equals(stepResult.getString("status"))) {
                // 审批未通过 / 步骤失败：停止后续动作
                break;
            }
        }
        JSONObject out = new JSONObject();
        out.put("intent", intent);
        out.put("sessionId", sessionId);
        out.put("status", status);
        out.put("steps", steps);
        out.put("totalDurationMillis", System.currentTimeMillis() - totalStart);
        log.info("意图执行完成: intent={} status={} steps={}", intent, status, steps.size());
        return out;
    }

    /**
     * 从 MCP JSON-RPC 响应中取出 content[0].text。
     */
    private String extractText(JSONObject resp) {
        if (resp == null) {
            return null;
        }
        JSONObject result = resp.getJSONObject("result");
        JSONArray content = result == null ? null : result.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return null;
        }
        JSONObject first = content.getJSONObject(0);
        return first == null ? null : first.getString("text");
    }

    /**
     * 系统提示：约束模型只输出结构化工具调用，禁止自由命令。
     */
    private JSONObject systemMessage() {
        JSONObject msg = new JSONObject();
        msg.put("role", "system");
        msg.put("content", "你是 Voyager1 的运维 Agent。只能通过提供的 tools 表达动作，"
            + "不得输出任意 shell 命令。用户提出多步需求时可一次返回多个 tool_calls，按执行顺序排列。"
            + "若无法映射到任何工具，则不要返回 tool_calls。");
        return msg;
    }

    private JSONObject userMessage(String intent) {
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", intent);
        return msg;
    }

    private JSONObject tool(String name, JSONObject args) {
        JSONObject step = new JSONObject();
        step.put("name", name);
        step.put("arguments", args);
        return step;
    }

    private String extractEnv(String intent) {
        Matcher m = ENV_PATTERN.matcher(intent.toLowerCase());
        return m.find() ? m.group(1) : null;
    }

    private String extractVersion(String intent) {
        Matcher m = VERSION_PATTERN.matcher(intent);
        return m.find() ? m.group(1) : null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
