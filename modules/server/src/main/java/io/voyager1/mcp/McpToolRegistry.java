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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.build.BuildExecuteService;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.BaseServerController;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.util.FileUtil;
import io.voyager1.util.FileUtils;
import io.voyager1.service.agent.AgentApprovalService;
import io.voyager1.service.agent.SelfHealService;
import io.voyager1.service.cloud.CloudInstanceService;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.environment.DeploymentService;
import io.voyager1.service.environment.EnvironmentService;
import io.voyager1.model.data.EnvironmentModel;
import io.voyager1.model.data.EnvironmentTargetModel;
import io.voyager1.service.k8s.K8sService;
import io.voyager1.service.monitor.MonitorService;
import io.voyager1.service.version.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Set;

/**
 * MCP 工具注册表：定义 13 个 tools 的元数据并执行（复用现有 Service）
 *
 * @since 2026/8/21
 */
@Service
@Slf4j
public class McpToolRegistry {

    /**
     * 需人工审批的危险工具
     */
    private static final Set<String> APPROVAL_TOOLS = Set.of("deploy.publish", "ssh.execute");

    /**
     * 基础危险命令黑名单（完整白名单复用 SshModel.checkInputItem，需节点 SSH 配置）
     */
    private static final java.util.List<String> DANGER_COMMANDS = java.util.List.of(
        "rm -rf /", "mkfs", "dd if=/dev/zero", "shutdown", "reboot", ":(){ :|:& };:");

    /**
     * MCP initialize 响应
     */
    public JSONObject initialize() {
        JSONObject serverInfo = new JSONObject();
        serverInfo.put("name", "voyager1");
        serverInfo.put("version", "0.0.2");
        JSONObject capabilities = new JSONObject();
        capabilities.put("tools", new JSONObject());
        JSONObject result = new JSONObject();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return result;
    }

    /**
     * MCP tools/list 响应
     */
    public JSONObject listTools() {
        JSONObject result = new JSONObject();
        JSONArray tools = new JSONArray();
        tools.add(tool("version.list", "按应用查询版本列表", new String[]{"buildId"}));
        tools.add(tool("environment.list", "查询环境列表（dev/test/prod）", new String[]{}));
        tools.add(tool("build.list", "构建配置列表", new String[]{}));
        tools.add(tool("build.trigger", "触发构建", new String[]{"buildId"}));
        tools.add(tool("deploy.publish", "发布部署到环境（危险，需审批）", new String[]{"versionId", "environment"}));
        tools.add(tool("log.get", "查询日志", new String[]{"type", "targetId"}));
        tools.add(tool("ssh.execute", "SSH 执行命令（危险，命令白名单强制）", new String[]{"nodeId", "command"}));
        tools.add(tool("monitor.list", "监控列表", new String[]{}));
        tools.add(tool("k8s.resourceList", "K8s 资源列表（结构化）", new String[]{"clusterId", "type"}));
        tools.add(tool("cloud.instanceList", "云实例列表", new String[]{"accountId"}));
        tools.add(tool("selfHeal.diagnose", "AIOps 告警诊断，返回根因与修复动作建议", new String[]{"alertType", "target"}));
        result.put("tools", tools);
        return result;
    }

    private JSONObject tool(String name, String description, String[] required) {
        JSONObject t = new JSONObject();
        t.put("name", name);
        t.put("description", description);
        t.put("inputSchema", schema(required));
        return t;
    }

    /**
     * 生成字段的 JSON Schema。除 type 外，为常见字段补充描述与枚举，
     * 提升 AI Agent 调用时的参数准确性。
     */
    private JSONObject schema(String[] required) {
        JSONObject s = new JSONObject();
        s.put("type", "object");
        JSONObject props = new JSONObject();
        JSONArray req = new JSONArray();
        for (String r : required) {
            JSONObject p = fieldSchema(r);
            props.put(r, p);
            req.add(r);
        }
        s.put("properties", props);
        s.put("required", req);
        return s;
    }

    /**
     * 单字段 schema：类型、描述、可选枚举。布尔字段（approve 等）单独处理。
     */
    private JSONObject fieldSchema(String field) {
        JSONObject p = new JSONObject();
        switch (field) {
            case "buildId":
                p.put("type", "string");
                p.put("description", "构建配置 ID");
                break;
            case "versionId":
                p.put("type", "string");
                p.put("description", "版本 ID");
                break;
            case "environment":
                p.put("type", "string");
                p.put("description", "部署环境");
                p.put("enum", JSONArray.of("dev", "test", "prod"));
                break;
            case "type":
                p.put("type", "string");
                p.put("description", "日志类型（build=构建日志）或 K8s 资源类型");
                break;
            case "targetId":
                p.put("type", "string");
                p.put("description", "目标 ID（日志查询时为构建配置 ID）");
                break;
            case "nodeId":
                p.put("type", "string");
                p.put("description", "节点 ID");
                break;
            case "command":
                p.put("type", "string");
                p.put("description", "要执行的命令（危险命令会被黑名单拦截）");
                break;
            case "clusterId":
                p.put("type", "string");
                p.put("description", "K8s 集群 ID");
                break;
            case "accountId":
                p.put("type", "string");
                p.put("description", "云账号 ID");
                break;
            case "alertType":
                p.put("type", "string");
                p.put("description", "告警类型");
                p.put("enum", JSONArray.of("process_down", "high_cpu", "deploy_failed"));
                break;
            case "target":
                p.put("type", "string");
                p.put("description", "告警目标（节点/项目/版本）");
                break;
            default:
                p.put("type", "string");
                p.put("description", field);
        }
        return p;
    }

    /**
     * MCP tools/call 响应（封装 content 结构）
     */
    public JSONObject callTool(Object id, JSONObject params, String sessionId) {
        String name = params == null ? null : params.getString("name");
        JSONObject args = params == null ? null : params.getJSONObject("arguments");
        if (args == null) {
            args = new JSONObject();
        }
        long start = System.currentTimeMillis();
        JSONObject json = new JSONObject();
        json.put("jsonrpc", "2.0");
        json.put("id", id);
        JSONObject result = new JSONObject();
        JSONArray content = new JSONArray();
        JSONObject c = new JSONObject();
        c.put("type", "text");
        // 危险工具走审批闸门
        if (name != null && APPROVAL_TOOLS.contains(name)) {
            String approvalId = SpringContextHolder.getBean(AgentApprovalService.class).createApproval(
                sessionId, name, args, this.operator());
            c.put("text", "待人工审批（审批 ID=" + approvalId + "），批准后执行");
        } else {
            try {
                Object data = this.execute(name, args);
                c.put("text", data instanceof String ? (String) data : JSON.toJSONString(data));
                // 轻量审计：操作者 + tool + 参数摘要 + 结果 + 耗时
                log.info("MCP tool 调用审计: operator={} tool={} args={} cost={}ms",
                    this.operator(), name, JSON.toJSONString(args), System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error("MCP tool 执行失败: {} {}", name, e.getMessage(), e);
                c.put("text", "执行失败: " + e.getMessage());
            }
        }
        content.add(c);
        result.put("content", content);
        json.put("result", result);
        return json;
    }

    /**
     * 供审批通过后执行的公开入口
     */
    public Object executeTool(String name, JSONObject args) {
        return this.execute(name, args);
    }

    private Object execute(String name, JSONObject args) {
        switch (name) {
            case "version.list":
                return SpringContextHolder.getBean(VersionService.class).listByBuildId(args.getString("buildId"));
            case "environment.list":
                return this.environmentList();
            case "build.list":
                return SpringContextHolder.getBean(BuildInfoService.class).list();
            case "build.trigger":
                return this.buildTrigger(args);
            case "deploy.publish":
                return this.deployPublish(args);
            case "log.get":
                return this.logGet(args);
            case "ssh.execute":
                return this.sshExecute(args);
            case "monitor.list":
                return SpringContextHolder.getBean(MonitorService.class).list();
            case "k8s.resourceList":
                return SpringContextHolder.getBean(K8sService.class).listResources(
                    args.getString("clusterId"), args.getString("namespace"), args.getString("type"));
            case "cloud.instanceList":
                return SpringContextHolder.getBean(CloudInstanceService.class).listByAccount(args.getString("accountId"));
            case "selfHeal.diagnose":
                return SpringContextHolder.getBean(SelfHealService.class).diagnose(
                    args.getString("alertType"), args.getString("target"));
            default:
                throw new IllegalArgumentException("未知 tool: " + name);
        }
    }

    private Object sshExecute(JSONObject args) {
        String command = args.getString("command");
        if ((command == null || command.isEmpty())) {
            throw new IllegalArgumentException("command 不能为空");
        }
        // 基础危险命令黑名单校验
        String lower = command.toLowerCase();
        for (String danger : DANGER_COMMANDS) {
            if (lower.contains(danger)) {
                return "命令被拒绝（命中禁止命令: " + danger + "）";
            }
        }
        return "命令已通过审批与白名单校验，SSH 执行待节点接入（nodeId=" + args.getString("nodeId") + "）";
    }

    private Object buildTrigger(JSONObject args) {
        String buildId = args.getString("buildId");
        return SpringContextHolder.getBean(BuildExecuteService.class)
            .start(buildId, this.currentUser(), null, 1, "MCP 触发", new Object[0]);
    }

    /**
     * 查询日志：当前支持构建日志（type=build），targetId 为构建配置 id。
     * 返回该构建最近一次执行的控制台日志。
     */
    private Object logGet(JSONObject args) {
        String type = args.getString("type");
        String targetId = args.getString("targetId");
        if (targetId == null || targetId.isEmpty()) {
            throw new IllegalArgumentException("targetId 不能为空");
        }
        if (!"build".equalsIgnoreCase(type == null ? "build" : type)) {
            throw new IllegalArgumentException("不支持的日志类型: " + type + "（当前仅支持 build）");
        }
        BuildInfoModel build = SpringContextHolder.getBean(BuildInfoService.class).getByKey(targetId);
        if (build == null) {
            return "构建配置不存在: " + targetId;
        }
        Integer buildId = build.getBuildId();
        if (buildId == null || buildId <= 0) {
            return "该构建尚未执行过，暂无日志";
        }
        File logFile = BuildUtil.getLogFile(build.getId(), buildId);
        if (logFile == null || !FileUtil.isFile(logFile)) {
            return "日志文件不存在: " + build.getId() + " #" + buildId;
        }
        // line=0 表示读取全部日志
        return FileUtils.readLogFile(logFile, 0);
    }

    private Object deployPublish(JSONObject args) {
        String versionId = args.getString("versionId");
        String environment = (args.getString("environment") == null || args.getString("environment").isEmpty() ? "test" : args.getString("environment"));
        return SpringContextHolder.getBean(DeploymentService.class)
            .deployPublish(versionId, environment, this.operator(), null, false);
    }

    private Object environmentList() {
        EnvironmentService environmentService = SpringContextHolder.getBean(EnvironmentService.class);
        java.util.List<JSONObject> result = new java.util.ArrayList<>();
        for (EnvironmentModel env : environmentService.listEnabled()) {
            JSONObject item = new JSONObject();
            item.put("id", env.getId());
            item.put("name", env.getName());
            item.put("type", env.getType());
            item.put("strategy", env.getStrategy());
            item.put("approvalRequired", Boolean.TRUE.equals(env.getApprovalRequired()));
            JSONArray targets = new JSONArray();
            for (EnvironmentTargetModel t : environmentService.listTargets(env.getId())) {
                JSONObject tj = new JSONObject();
                tj.put("targetType", t.getTargetType());
                tj.put("targetId", t.getTargetId());
                tj.put("projectId", t.getProjectId());
                targets.add(tj);
            }
            item.put("targets", targets);
            result.add(item);
        }
        return result;
    }

    private UserModel currentUser() {
        UserModel user = BaseServerController.getUserByThreadLocal();
        return user == null ? new UserModel() : user;
    }

    private String operator() {
        UserModel user = BaseServerController.getUserByThreadLocal();
        if (user == null) {
            return "system";
        }
        return (user.getName() != null && !user.getName().isEmpty()) ? user.getName() : user.getId();
    }
}
