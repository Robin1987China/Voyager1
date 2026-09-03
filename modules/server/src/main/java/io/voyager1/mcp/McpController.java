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

import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP（Model Context Protocol）HTTP 端点：JSON-RPC 2.0 分发
 *
 * <p>复用现有 JWT 鉴权（全局 LoginInterceptor），路由 /mcp。
 * 支持方法：initialize / ping / tools/list / tools/call / notifications/*</p>
 *
 * @since 2026/8/21
 */
@RestController
@RequestMapping(value = "/mcp")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE, method = MethodFeature.EXECUTE)
public class McpController extends BaseServerController {

    @PostMapping(produces = "application/json")
    public JSONObject handle(@RequestBody JSONObject request, HttpServletRequest httpRequest) {
        String method = request.getString("method");
        Object id = request.get("id");
        // notification（无 id）不返回 result
        if (id == null || "notifications/initialized".equals(method) || "notifications/cancelled".equals(method)) {
            return new JSONObject();
        }
        McpToolRegistry registry = SpringContextHolder.getBean(McpToolRegistry.class);
        switch (method) {
            case "initialize":
                return rpcResult(id, registry.initialize());
            case "ping":
                return rpcResult(id, new JSONObject());
            case "tools/list":
                return rpcResult(id, registry.listTools());
            case "tools/call":
                return registry.callTool(id, request.getJSONObject("params"), this.agentSessionId(httpRequest));
            default:
                return rpcError(id, -32601, "Method not found: " + method);
        }
    }

    private String agentSessionId(HttpServletRequest request) {
        String sessionId = request.getHeader("X-Agent-Session-Id");
        return (sessionId != null && !sessionId.isEmpty()) ? sessionId : java.util.UUID.randomUUID().toString();
    }

    private JSONObject rpcResult(Object id, Object result) {
        JSONObject json = new JSONObject();
        json.put("jsonrpc", "2.0");
        json.put("id", id);
        json.put("result", result);
        return json;
    }

    private JSONObject rpcError(Object id, int code, String message) {
        JSONObject json = new JSONObject();
        json.put("jsonrpc", "2.0");
        json.put("id", id);
        JSONObject err = new JSONObject();
        err.put("code", code);
        err.put("message", message);
        json.put("error", err);
        return json;
    }
}
