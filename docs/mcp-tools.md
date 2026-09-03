# Voyager1 MCP Server 工具清单与安全边界

## 概述

Voyager1 通过 MCP（Model Context Protocol）把核心运维能力暴露为标准的 **MCP tools**，供 AI Agent（Cursor、Claude Code 等）安全调用。

- **端点**：`POST /mcp`（复用 Voyager1 Server 2122 端口）
- **协议**：JSON-RPC 2.0（`initialize` / `ping` / `tools/list` / `tools/call` / `notifications/*`）
- **鉴权**：JWT（`Authorization` 头，复用现有 `LoginInterceptor`）
- **授权**：`@Feature(cls = SYSTEM_ASSETS_MACHINE, method = EXECUTE)`（复用现有 `PermissionInterceptor`）
- **审计**：复用现有 `WebAopLog` 操作日志（调用者/参数/结果）

## 工具清单（12 个）

| Tool | 用途 | 入参 | 危险等级 |
|---|---|---|---|
| `version.list` | 按应用查询版本列表 | buildId | 只读 |
| `environment.list` | 环境列表（dev/test/prod） | — | 只读 |
| `build.list` | 构建配置列表 | — | 只读 |
| `build.trigger` | 触发构建 | buildId | 低 |
| `deploy.publish` | 发布部署到环境 | versionId, environment | 高（审批链路待 Phase2） |
| `pipeline.trigger` | 触发流水线 | pipelineId | 中 |
| `pipeline.approval` | 流水线审批（人工闸门） | executeId, approve | 高（HITL） |
| `log.get` | 查询日志 | type, targetId | 只读（真实日志读取待接入） |
| `ssh.execute` | SSH 执行命令 | nodeId, command | 高（白名单执行待接入） |
| `monitor.list` | 监控列表 | — | 只读 |
| `k8s.resourceList` | K8s 资源列表（结构化） | clusterId, type, namespace | 只读 |
| `cloud.instanceList` | 云实例列表 | accountId | 只读 |

## 安全边界

| 层 | 实现 |
|---|---|
| 认证 | 全局 JWT（`LoginInterceptor`） |
| 授权 | `@Feature` + `PermissionInterceptor`（MCP 端点统一 `EXECUTE` 权限） |
| 审计 | `WebAopLog` 操作日志（`@Feature` 自动记录请求/结果） |
| 工作空间隔离 | 各 Service 现有 `workspaceId` 过滤 |
| 命令白名单 | `ssh.execute` 待接入（复用 `SshCommandService` 白名单） |
| 审批 | 危险操作审批链路待 Phase2（`phase2-agent-trust`）落地 |

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
```

## 技术选型说明

采用**自实现符合 MCP 规范核心的轻量 Server**（JSON-RPC 2.0），不引入第三方里程碑版 SDK：
- spring-ai-mcp-server 为 milestone 版（需额外仓库），官方 java-sdk 演进快（模块还在拆 `mcp`→`mcp-core`），引入后依赖/API 风险高；
- MCP 协议本质是 JSON-RPC 2.0，自实现可控，且能最自然复用现有 JWT、`@Feature`、审计框架。
