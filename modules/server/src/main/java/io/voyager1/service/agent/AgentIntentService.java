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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 意图解析服务（LLM 网关的规则降级版）
 *
 * <p>把自然语言意图（如「把 v1.2.3 部署到 test」）拆解为 MCP 工具调用序列。
 * 规则版基于关键词匹配，LLM 实现作为可插拔扩展（后续接入 OpenAI 兼容接口）。</p>
 *
 * @since 2026/8/25
 */
@Service
@Slf4j
public class AgentIntentService {

    private static final Pattern ENV_PATTERN = Pattern.compile("(dev|test|prod|staging|production)");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(v\\d+\\.\\d+\\.\\d+[\\w.-]*)");

    /**
     * 解析意图 → 工具调用序列
     *
     * @param intent 自然语言意图
     * @return 工具调用序列（按执行顺序）
     */
    public List<JSONObject> parseIntent(String intent) {
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
        if (containsAny(lower, "流水线", "pipeline")) {
            steps.add(tool("pipeline.trigger", new JSONObject()));
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
