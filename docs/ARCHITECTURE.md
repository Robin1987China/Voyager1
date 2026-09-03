# Voyager1 架构说明

Voyager1 是一款轻量级的原生运维平台（在线构建、自动部署、日常运维、项目监控），采用 **Server + Agent** 架构。

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         浏览器 (Vue 3)                        │
│  构建 / 部署 / 监控 / SSH / Docker / 权限 / K8s / 云资产       │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP + WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│                     Server（端口 2122）                       │
│  Web 控制台 · 构建编排 · 流水线 · 权限 · 监控 · OAuth2         │
└──────────────────────────┬──────────────────────────────────┘
                           │ agent-transport（HTTP + WebSocket，SPI）
┌──────────────────────────▼──────────────────────────────────┐
│                     Agent（端口 2123）                        │
│  构建执行 · 脚本 · 进程管理 · 文件 · 日志流                     │
└─────────────────────────────────────────────────────────────┘
```

- **Server**：Web 控制台，负责管理构建/监控/SSH/Docker/权限，以及可视化流水线编排。
- **Agent**：部署在被管主机上的节点端，执行构建、脚本、进程管理，并通过 WebSocket 回传终端/日志流。

## 模块结构（`modules/`）

| 模块 | 职责 |
|---|---|
| `server` | 服务端主程序（controller / service / socket / 权限 / oauth2 / monitor） |
| `agent` | 节点端主程序 |
| `common` | 共享基础类（含 vendor 基础契约类） |
| `agent-transport` | Server ↔ Agent 传输层（SPI，http 实现） |
| `storage-module` | 数据库方言实现（h2 / mysql / mariadb / postgresql） |
| `sub-plugin` | 功能插件（git-clone / svn-clone / docker-cli / ssh-jsch / email / webhook / encrypt） |

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.5.x + Maven 多模块 |
| 前端 | Vue 3 + Vite + TypeScript（`web-vue/`），i18n 四语言 |
| 存储 | H2 默认（`storage-module` SPI 可切换 MySQL / MariaDB / PostgreSQL） |
| 通信 | HTTP + WebSocket（终端/日志流）+ JSch SSH + JGit / SvnKit |
| 依赖 | 以 Guava、Apache Commons、Spring 生态、Jackson、Caffeine、fastjson2 为主 |

## 核心设计

### 存储 SPI
数据访问以 **JPA**（`io.voyager1.core.jpa`，`JpaBaseService` / `JpaQuerySupport`）为主，`io.voyager1.core.entity` 下为 JPA 实体（54+ 个）；通过 `storage-module` 的方言适配不同数据库。默认使用 H2，切换数据库时激活对应 profile（`application-mysql.yml` 等）。部分历史代码仍保留少量 JdbcTemplate，逐步收敛到 JPA。

### 插件机制
`sub-plugin` 下的各功能插件通过 `@PluginConfig(name = "...")` 声明，由 `PluginFactory` 在启动时扫描 `io.voyager1` 包并加载，支持按需扩展构建/部署能力。

### 通信
Server 与 Agent 之间通过 `agent-transport` 抽象（HTTP + WebSocket）通信，WebSocket 用于终端和日志的实时回传。

### 关键业务能力
- 构建流水线：可视化编排、手动 / cron / WebHook 触发、构建记录与流水线双向关联
- 发布版本状态机：构建产物版本、发布状态流转（提测冻结 / 打回）
- 环境晋升：dev → test → prod 泳道视图、自动 CD、审批邮件、失败自动打回
- 云资产：阿里云 / 腾讯云 / AWS 账号与实例管理，一键导入为 SSH 机器
- K8s 集群：kubeconfig 接入、结构化资源列表 / 详情 / 删除 / 扩缩容 / 日志 / 事件 / manifest 部署

### AI 能力（MCP / Agent / AIOps）
- **MCP Server**：`/mcp` 端点（JSON-RPC 2.0），复用 JWT 鉴权 + `@Feature` 权限 + `WebAopLog` 审计，把 12 个运维能力暴露为 MCP 工具（`build.*` / `deploy.publish` / `pipeline.*` / `ssh.execute` / `k8s.*` / `cloud.*` 等），供 AI Agent（Cursor、Claude Code 等）调用。详见 [mcp-tools.md](mcp-tools.md)。
- **Agent 意图解析**：`service/agent/AgentIntentService`，把自然语言拆解为 MCP 工具调用序列（规则版，LLM 可插拔）。
- **Agent 审批闸门**：`service/agent/AgentApprovalService` + `AgentApprovalController`，高危工具（`deploy.publish` / `ssh.execute`）需人工审批；内置危险命令黑名单。
- **AIOps 自愈**：`service/agent/SelfHealService`，告警 → 根因 → 修复动作映射（规则版，后续接入 LLM 网关）。

## 目录速览

```
├── modules/           # 后端多模块
├── web-vue/           # 前端（Vue 3 + Vite）
├── docs/              # 文档
├── script/            # 构建 / 部署 / UI 巡检脚本
├── openspec/          # OpenSpec 规格（capability 权威来源）
└── docker-compose*.yml
```
