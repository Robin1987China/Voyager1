# Voyager1 MCP Server 工具清单与安全边界

## 概述

Voyager1 通过 MCP（Model Context Protocol）把核心运维能力暴露为标准的 **MCP tools**，供 AI Agent（Cursor、Claude Code 等）安全调用。

- **端点**：`POST /mcp`（复用 Voyager1 Server 2122 端口）
- **协议**：JSON-RPC 2.0（`initialize` / `ping` / `tools/list` / `tools/call` / `notifications/*`）
- **鉴权**：JWT（`Authorization` 头，复用现有 `LoginInterceptor`）
- **授权**：`@Feature(cls = SYSTEM_ASSETS_MACHINE, method = EXECUTE)`（复用现有 `PermissionInterceptor`）
- **审计**：`AGENT_AUDIT` 表（会话 / 意图 / 工具 / 入参 / 结果 / 耗时）+ 现有 `WebAopLog`

## 工具清单（11 个）

| Tool | 用途 | 入参 | 危险等级 |
|---|---|---|---|
| `version.list` | 按应用查询版本列表 | buildId | 只读 |
| `environment.list` | 环境列表（dev/test/prod） | — | 只读 |
| `build.list` | 构建配置列表 | — | 只读 |
| `build.trigger` | 触发构建 | buildId | 低 |
| `deploy.publish` | 发布部署到环境 | versionId, environment | **高（需人工审批）** |
| `log.get` | 查询构建日志 | type, targetId | 只读 |
| `ssh.execute` | SSH 执行命令 | nodeId, command | **高（需人工审批 + 危险命令黑名单）** |
| `monitor.list` | 监控列表 | — | 只读 |
| `k8s.resourceList` | K8s 资源列表（结构化） | clusterId, type, namespace | 只读 |
| `cloud.instanceList` | 云实例列表 | accountId | 只读 |
| `selfHeal.diagnose` | AIOps 告警诊断（根因 + 修复动作） | alertType, target | 只读 |

> 注：`pipeline.trigger` / `pipeline.approval` 已随旧 Pipeline 功能一并移除（旧端点返回 404），
> 版本与环境化 CD 由 `version.*` / `environment.*` / `deploy.publish` 承担。

## 安全边界

| 层 | 实现 |
|---|---|
| 认证 | 全局 JWT（`LoginInterceptor`） |
| 授权 | `@Feature` + `PermissionInterceptor`（MCP 端点统一 `EXECUTE` 权限） |
| 审批闸门 | `APPROVAL_TOOLS = {deploy.publish, ssh.execute}` → 落 `AGENT_APPROVAL` 待审批单，批准后才执行 |
| 审批超时 | 每分钟扫描，过期未处理自动置为「已超时」 |
| 命令安全 | `ssh.execute` 危险命令黑名单（`rm -rf /`、`mkfs`、`dd if=/dev/zero`、`shutdown`、`reboot`、fork bomb） |
| 执行审计 | `AGENT_AUDIT`（`POST /agent/audit/list`、`POST /agent/audit/session`） |
| 工作空间隔离 | 各 Service 现有 `workspaceId` 过滤 |

## Agent 相关 API（非 MCP 通道）

| 接口 | 用途 |
|---|---|
| `POST /agent/intent/parse` | 自然语言 → 工具调用序列（不执行） |
| `POST /agent/intent/execute` | 解析并**逐工具执行**；危险工具停在待审批 |
| `POST /agent/approval/list` | 待审批列表 |
| `POST /agent/approval/approve` / `reject` | 批准（并执行）/ 拒绝 |
| `POST /agent/audit/list` / `session` | 审计分页 / 按会话查轨迹 |
| `POST /agent/self-heal/execute` | 执行自愈动作（危险动作落审批） |
| `POST /agent/self-heal/abort` | 人工中止某会话的自愈 |
| `POST /agent/self-heal/reset` | 复位熔断 |

## 使用示例（curl）

```bash
# 1. 登录拿 token
TOKEN=$(curl -s -X POST "http://127.0.0.1:2122/userLogin?loginName=admin&userPwd=<sha1>" \
  | jq -r '.data.token')

# 2. initialize
curl -s -X POST http://127.0.0.1:2122/mcp \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"cli","version":"1.0"}}}'

# 3. tools/list
curl -s -X POST http://127.0.0.1:2122/mcp \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

# 4. tools/call（只读）
curl -s -X POST http://127.0.0.1:2122/mcp \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"environment.list","arguments":{}}}'

# 5. 意图执行（危险工具会停待审批）
curl -s -X POST "http://127.0.0.1:2122/agent/intent/execute" \
  -H "Authorization: $TOKEN" -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "intent=把 v1.2.3 部署到 test"
```

## 技术选型说明

采用**自实现符合 MCP 规范核心的轻量 Server**（JSON-RPC 2.0），不引入第三方里程碑版 SDK：
- spring-ai-mcp-server 为 milestone 版（需额外仓库），官方 java-sdk 演进快（模块还在拆 `mcp`→`mcp-core`），引入后依赖/API 风险高；
- MCP 协议本质是 JSON-RPC 2.0，自实现可控，且能最自然复用现有 JWT、`@Feature`、审计框架。
