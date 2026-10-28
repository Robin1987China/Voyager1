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

/**
 * LLM 网关抽象（OpenAI 兼容）。
 * <p>
 * 未配置模型时 {@link #available()} 返回 false，调用方回落到规则实现。
 *
 * @since 2026/10/27
 */
public interface LlmGateway {

    /**
     * 是否已具备调用条件（开关开启 + base-url/model 配置完整）。
     */
    boolean available();

    /**
     * 调用 chat/completions，返回 assistant message。
     *
     * @param messages 消息列表（OpenAI 格式）
     * @param tools    工具定义（OpenAI function calling 格式），可为 null
     * @return assistant message：{@code {role, content, tool_calls?}}
     * @throws IllegalStateException 调用失败（网络/HTTP/协议错误）
     */
    JSONObject chat(JSONArray messages, JSONArray tools);

    /**
     * 把 MCP 的 {@code tools/list} 结果转换为 OpenAI function calling 的 tools 数组。
     *
     * @param mcpTools {@code McpToolRegistry.listTools()} 的返回（含 tools 数组）
     * @return OpenAI tools 数组
     */
    static JSONArray toOpenAiTools(JSONObject mcpTools) {
        JSONArray out = new JSONArray();
        if (mcpTools == null) {
            return out;
        }
        JSONArray tools = mcpTools.getJSONArray("tools");
        if (tools == null) {
            return out;
        }
        for (int i = 0; i < tools.size(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            JSONObject fn = new JSONObject();
            fn.put("name", tool.getString("name"));
            fn.put("description", tool.getString("description"));
            JSONObject schema = tool.getJSONObject("inputSchema");
            fn.put("parameters", schema == null ? new JSONObject() : schema);
            JSONObject item = new JSONObject();
            item.put("type", "function");
            item.put("function", fn);
            out.add(item);
        }
        return out;
    }

    /**
     * 从 assistant message 中取第一个工具调用。
     *
     * @return {@code {name, arguments}}；无工具调用返回 null
     */
    static JSONObject firstToolCall(JSONObject message) {
        JSONArray all = allToolCalls(message);
        return all.isEmpty() ? null : all.getJSONObject(0);
    }

    /**
     * 从 assistant message 中取出全部工具调用（多步意图）。
     *
     * @return {@code [{name, arguments}]}
     */
    static JSONArray allToolCalls(JSONObject message) {
        JSONArray out = new JSONArray();
        if (message == null) {
            return out;
        }
        JSONArray toolCalls = message.getJSONArray("tool_calls");
        if (toolCalls == null || toolCalls.isEmpty()) {
            return out;
        }
        for (int i = 0; i < toolCalls.size(); i++) {
            JSONObject call = toolCalls.getJSONObject(i);
            JSONObject fn = call == null ? null : call.getJSONObject("function");
            if (fn == null || fn.getString("name") == null) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("name", fn.getString("name"));
            String arguments = fn.getString("arguments");
            JSONObject args;
            try {
                args = (arguments == null || arguments.isEmpty()) ? new JSONObject() : JSONObject.parseObject(arguments);
            } catch (Exception e) {
                args = new JSONObject();
            }
            item.put("arguments", args);
            out.add(item);
        }
        return out;
    }
}
