# Changelog

本文件记录 Voyager1 的主要变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Added

- **Agent 执行审计**：新增 `AGENT_AUDIT` 表与 `POST /agent/audit/list`、`POST /agent/audit/session`，
  每次 MCP 工具调用留痕（会话 / 意图 / 工具 / 入参 / 结果 / 耗时），审计写入失败不影响工具调用
- **意图执行闭环**：`POST /agent/intent/execute` 按序执行解析出的工具序列；遇到需审批的危险工具即停止
- **自愈自治能力**：告警类型白名单、连续失败熔断、人工中止（`POST /agent/self-heal/abort`）、
  自愈执行入口（`POST /agent/self-heal/execute`）
- **部署失败自动回滚**：`DeploymentService` 失败回写时把已提测版本打回并解锁 CI，
  开关 `voyager1.agent.auto-rollback-enabled`（默认开启）
- **审批超时自动拒绝**：每分钟扫描过期待审批单并置为「已超时」
- **LLM 可插拔网关**：`voyager1.llm.*` 配置；未配置时沿用规则版，行为不变

### Fixed

- 意图解析移除了指向已删除 `pipeline.trigger` 的映射（旧 Pipeline 功能已下线）
- 意图解析补充「环境」关键词，`environment.list` 可被自然语言命中
- `docs/mcp-tools.md` 与实际 11 个工具对齐（删除不存在的 `pipeline.*`，补充 `selfHeal.diagnose`）
- 补齐 `CHANGELOG.md` / `CHANGELOG-BETA.md`（`modules/server/pom.xml` 已引用但文件缺失）

## [0.0.2] - 2026-09-10

### Added

- **环境化 CD**：dev/test/prod 环境定义（策略 `CI_CD` / `CD_ONLY`、审批开关）+ 目标绑定，
  支持 **NODE / K8S / SSH** 三类部署目标（含工作区权限校验）
- **部署审批**：需审批环境落「待审批」记录，批准后异步执行、拒绝闭环
- **K8S 容器交付**：Dockerfile 构建 + 递归 apply 多 manifest
- **MCP 工具网关**：基于 JSON-RPC 2.0 的 11 个工具，危险工具审批闸门
- **AIOps 自愈诊断**：进程停止告警自动触发根因与修复动作建议
- **四语言 i18n**：`zh_cn` / `en_us` / `zh_hk` / `zh_tw`，统一 `i18n_ + md5(zh)[:10]` key
- **DSH 深蓝灰主题**：`web-vue/src/theme/dsh.ts` 作为配色唯一权威 + `--dsh-*` CSS 变量
- **云资产 / FinOps**：多云账号与实例管理、账单导入与成本分析

### Fixed

- 修复全局「薄纱」蒙层导致的暗色发灰问题
- 修复 mustache 字面量 i18n key 渲染出原始 key 的问题
- 补齐 6 处暗色漏网（`theme="light"` 侧栏、失效 CSS 变量、白底页脚）
- 登录页品牌标题折行（字距过大超出卡片宽度）
- `main.ts` 全局错误提示漏接 i18n
- 节点文件上传 multipart 丢失、表单转发缺 `Content-Type`、`FileUtil.move` 目标为目录等问题

### Changed

- 应用交付体系重构：以「应用（Application）」为第一公民，废弃旧 Pipeline API（旧端点返回 404）
- 持久层迁移到 Spring Data JPA，Flyway 精简为单基线 `V1` + 增量 `V2`/`V3`

## [0.0.1] - 2026-08-30

### Added

- 首个可用版本：在线构建、自动部署、日常运维（SSH 终端/文件、脚本库）、项目监控、
  Docker 与 K8S 资源管理、用户与权限组、Server + Agent 双进程架构
