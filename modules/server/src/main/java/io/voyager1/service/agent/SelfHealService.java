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
import io.voyager1.common.SpringContextHolder;
import io.voyager1.configuration.AgentAutonomyConfig;
import io.voyager1.mcp.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIOps 自愈服务（规则版：告警 → 根因 → 修复动作映射）。
 *
 * <p>具备三项自治保障：</p>
 * <ul>
 *   <li><b>白名单</b>：仅配置内的告警类型允许自动执行，其余只诊断；</li>
 *   <li><b>熔断</b>：同一 {@code alertType:target} 连续失败达阈值即停止自愈并转人工；</li>
 *   <li><b>人工中止</b>：{@link #abort(String)} 后该会话的自愈不再执行。</li>
 * </ul>
 *
 * <p>诊断根因可由 LLM 网关增强；未配置模型时走规则映射。</p>
 *
 * @since 2026/8/25
 */
@Service
@Slf4j
public class SelfHealService {

    private final AgentAutonomyConfig config;
    private final LlmGateway llmGateway;

    /**
     * 熔断计数：alertType:target -> 连续失败次数
     */
    private final ConcurrentHashMap<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();

    /**
     * 已被人工中止的 Agent 会话
     */
    private final Set<String> abortedSessions = ConcurrentHashMap.newKeySet();

    public SelfHealService(AgentAutonomyConfig config, LlmGateway llmGateway) {
        this.config = config;
        this.llmGateway = llmGateway;
    }

    /**
     * 告警诊断 + 修复动作建议。
     * <p>
     * 已配置 LLM 时由模型输出结构化根因；未配置/调用失败/输出不合法时**回落规则映射**。
     * </p>
     *
     * @param alertType 告警类型（process_down/high_cpu/deploy_failed/...）
     * @param target    告警目标
     * @return 根因 + 修复动作
     */
    public JSONObject diagnose(String alertType, String target) {
        JSONObject result = new JSONObject();
        result.put("alertType", alertType);
        result.put("target", target);
        String key = key(alertType, target);
        if (this.isCircuitOpen(key)) {
            result.put("rootCause", "自愈连续失败已达阈值，熔断转人工处理");
            result.put("action", "manual");
            result.put("tool", null);
            result.put("approval", true);
            result.put("circuitOpen", true);
            result.put("allowed", false);
            log.warn("自愈熔断: alertType={} target={} failures={}", alertType, target, this.failures(key));
            return result;
        }
        JSONObject byLlm = this.diagnoseByLlm(alertType, target);
        if (byLlm != null) {
            byLlm.put("alertType", alertType);
            byLlm.put("target", target);
            byLlm.put("circuitOpen", false);
            byLlm.put("allowed", this.isAllowed(alertType));
            byLlm.put("source", "llm");
            log.info("自愈诊断(LLM): alertType={} action={}", alertType, byLlm.getString("action"));
            return byLlm;
        }
        this.diagnoseByRules(alertType, target, result);
        result.put("circuitOpen", false);
        result.put("allowed", this.isAllowed(alertType));
        result.put("source", "rules");
        log.info("自愈诊断: alertType={} action={}", alertType, result.getString("action"));
        return result;
    }

    /**
     * 规则映射（LLM 不可用时的兜底）。
     */
    private void diagnoseByRules(String alertType, String target, JSONObject result) {
        switch (alertType == null ? "" : alertType.toLowerCase()) {
            case "process_down":
                result.put("rootCause", "进程停止");
                result.put("action", "restart");
                result.put("tool", "ssh.execute");
                result.put("approval", true);
                break;
            case "high_cpu":
                result.put("rootCause", "CPU 使用率过高");
                result.put("action", "scale");
                result.put("tool", "k8s.resourceList");
                result.put("approval", false);
                break;
            case "deploy_failed":
                result.put("rootCause", "部署失败");
                result.put("action", "rollback");
                result.put("tool", "deploy.publish");
                result.put("approval", true);
                break;
            default:
                result.put("rootCause", "未知告警类型，转人工处理");
                result.put("action", "manual");
                result.put("tool", null);
                result.put("approval", true);
        }
    }

    /**
     * 由 LLM 生成结构化根因；不可用或输出不合法时返回 null（调用方回落规则）。
     */
    private JSONObject diagnoseByLlm(String alertType, String target) {
        if (llmGateway == null || !llmGateway.available()) {
            return null;
        }
        try {
            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", "你是 Voyager1 的 AIOps 诊断助手。根据告警给出根因与修复动作，"
                + "只输出 JSON 对象，字段：rootCause(string)、action(string)、tool(string 或 null)、"
                + "approval(boolean)、confidence(0~1 的数字)。tool 只能取以下之一："
                + "ssh.execute、deploy.publish、k8s.resourceList、monitor.list、null。");
            messages.add(system);
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", "alertType=" + alertType + ", target=" + target);
            messages.add(user);

            JSONObject message = llmGateway.chat(messages, null);
            String content = message == null ? null : message.getString("content");
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            String json = stripCodeFence(content);
            JSONObject parsed = JSONObject.parseObject(json);
            if (parsed == null || parsed.getString("rootCause") == null) {
                log.warn("LLM 诊断输出不合法，回落规则: {}", content);
                return null;
            }
            if (!parsed.containsKey("approval")) {
                parsed.put("approval", true);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("LLM 诊断失败，回落规则: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 去掉模型可能包裹的 ```json 代码围栏。
     */
    private static String stripCodeFence(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            if (firstLine > 0) {
                text = text.substring(firstLine + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        return text.trim();
    }

    /**
     * 执行自愈动作：诊断 → 调 MCP 工具（危险工具自动落审批）。
     *
     * @param alertType 告警类型
     * @param target    告警目标
     * @param sessionId Agent 会话 ID（用于中止判定与审计）
     * @param arguments 工具入参（不同工具要求不同，由调用方提供）
     * @return 执行结果：{@code executed / pending_approval / failed / manual / circuit_open / aborted / not_allowed}
     */
    public JSONObject execute(String alertType, String target, String sessionId, JSONObject arguments) {
        JSONObject out = new JSONObject();
        out.put("alertType", alertType);
        out.put("target", target);
        out.put("sessionId", sessionId);
        if (sessionId != null && abortedSessions.contains(sessionId)) {
            out.put("status", "aborted");
            out.put("message", "该会话的自愈已被人工中止");
            return out;
        }
        if (!this.isAllowed(alertType)) {
            out.put("status", "not_allowed");
            out.put("message", "告警类型不在自愈白名单内，仅允许诊断");
            return out;
        }
        JSONObject diagnose = this.diagnose(alertType, target);
        out.put("diagnose", diagnose);
        if (Boolean.TRUE.equals(diagnose.getBoolean("circuitOpen"))) {
            out.put("status", "circuit_open");
            return out;
        }
        String tool = diagnose.getString("tool");
        if (tool == null) {
            out.put("status", "manual");
            return out;
        }
        JSONObject args = arguments == null ? new JSONObject() : arguments;
        JSONObject params = new JSONObject();
        params.put("name", tool);
        params.put("arguments", args);
        String text;
        try {
            JSONObject resp = SpringContextHolder.getBean(McpToolRegistry.class)
                .callTool(1, params, sessionId, "self-heal:" + alertType + ":" + target);
            text = extractText(resp);
        } catch (Exception e) {
            log.warn("自愈执行异常: {} {}", alertType, e.getMessage());
            text = "执行失败: " + e.getMessage();
        }
        out.put("result", text);
        if (text != null && text.contains("待人工审批")) {
            out.put("status", "pending_approval");
            return out;
        }
        if (text == null || text.startsWith("执行失败")) {
            this.recordFailure(alertType, target);
            out.put("status", "failed");
            return out;
        }
        this.recordSuccess(alertType, target);
        out.put("status", "executed");
        return out;
    }

    /**
     * 人工中止某会话的自愈。
     */
    public void abort(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            abortedSessions.add(sessionId);
            log.warn("自愈已被人工中止: session={}", sessionId);
        }
    }

    /**
     * 记录一次自愈失败（连续失败达阈值即熔断）。
     */
    public void recordFailure(String alertType, String target) {
        String key = key(alertType, target);
        int count = failureCounters.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        log.warn("自愈失败计数: {} -> {}", key, count);
        if (count >= config.getSelfHealFailureThreshold()) {
            log.warn("自愈熔断触发: {} 连续失败 {} 次", key, count);
        }
    }

    /**
     * 记录一次自愈成功（清零连续失败计数）。
     */
    public void recordSuccess(String alertType, String target) {
        failureCounters.remove(key(alertType, target));
    }

    /**
     * 手动复位熔断（运维/测试用）。
     */
    public void resetCircuit(String alertType, String target) {
        failureCounters.remove(key(alertType, target));
    }

    public boolean isCircuitOpen(String alertType, String target) {
        return this.isCircuitOpen(key(alertType, target));
    }

    public int failures(String alertType, String target) {
        return this.failures(key(alertType, target));
    }

    private boolean isCircuitOpen(String key) {
        return this.failures(key) >= config.getSelfHealFailureThreshold();
    }

    private int failures(String key) {
        AtomicInteger counter = failureCounters.get(key);
        return counter == null ? 0 : counter.get();
    }

    private boolean isAllowed(String alertType) {
        if (alertType == null || alertType.isEmpty()) {
            return false;
        }
        String allowed = config.getSelfHealAllowedAlertTypes();
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        for (String item : allowed.split(",")) {
            if (item.trim().equalsIgnoreCase(alertType)) {
                return true;
            }
        }
        return false;
    }

    private static String key(String alertType, String target) {
        return (alertType == null ? "" : alertType.toLowerCase()) + ":" + (target == null ? "" : target);
    }

    private static String extractText(JSONObject resp) {
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
}
