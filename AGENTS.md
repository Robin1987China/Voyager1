# AGENTS.md

Voyager1 项目开发指南（供 AI agent 与开发者使用）

## 项目概览

Voyager1 是自研的轻量级运维平台（在线构建、自动部署、日常运维、项目监控），采用 **Server + Agent** 架构：
- **Server**（端口 2122）：Web 控制台，管理构建/监控/SSH/Docker/权限
- **Agent**（端口 2123）：部署在被管主机上的插件端，执行构建/脚本/进程管理

项目代码统一使用 Voyager1 自有命名空间，**代码层禁止出现任何历史遗留品牌关键字残留**。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.5.16 + Maven 多模块 |
| 前端 | Vue 3 + Vite + TypeScript（`web-vue/`），i18n 四语言 |
| 存储 | H2 默认（`storage-module` SPI 可切换 MySQL/MariaDB/PG） |
| 通信 | HTTP + WebSocket（终端/日志流）+ JSch SSH + JGit/SvnKit |

## 模块结构（`modules/`）

- `server`：服务端主程序（controller/service/socket/权限/oauth2/monitor）
- `agent`：插件端主程序
- `common`：共享基础类与跨端契约（含 `io.voyager1.core.api.ApiResult` 统一响应体等）
- `agent-transport`：Server↔Agent 传输层（SPI）
- `storage-module`：数据库方言实现（h2/mysql/mariadb/postgresql）
- `sub-plugin`：功能插件（git-clone/svn-clone/docker-cli/ssh-jsch/email/webhook/encrypt）

## 业务能力（自研 Pipeline + 多云/K8s）

| 能力 | 说明 | 关键代码 |
|---|---|---|
| 版本状态机 | 构建产物版本、发布状态流转（含提测冻结/打回） | `service/version/VersionService.java` |
| 可视化 Pipeline 编辑器 | 流水线图形编辑、节点编排 | `service/pipeline/PipelineConfigService.java`、`web-vue/src/pages/pipeline/pipeline-list.vue` |
| 环境晋升 | dev→test→prod 泳道视图、自动 CD、审批邮件、失败自动打回 | `web-vue/src/pages/pipeline/swimlane.vue`、`service/environment/EnvironmentService.java`、`service/DeploymentService.java` |
| 触发方式 | 手动 / cron 定时 / WebHook 触发 Pipeline；构建列表与 Pipeline 双向关联 | `service/pipeline/PipelineConfigService.java`（CronUtils key `pipeline:<id>`）、`POST /pipeline/trigger-webhook` |
| 云资产 | 云账号（aliyun/tencent/aws）+ 云实例管理，实例一键导入为 SSH 机器 | `service/cloud/{CloudService,CloudInstanceService}.java`、`controller/cloud/CloudController.java`、`web-vue/src/pages/cloud/cloud-list.vue`（表 CLOUD_ACCOUNT/CLOUD_INSTANCE） |
| K8s 集群 | kubeconfig 接入集群、结构化资源列表/详情/删除/扩缩容/重启/日志/事件、manifest 部署（fabric8 SDK） | `service/k8s/K8sService.java`、`controller/k8s/K8sController.java`、`web-vue/src/pages/k8s/k8s-list.vue`（表 K8S_CLUSTER） |

## 开发环境

- **JDK 17**：必须（Spring Boot 3 最低要求；JDK ≥ 26 与 lombok 不兼容）
- **Maven 3.9.x**；**Node ≥ 22**（前端构建 + UI 巡检）
- 登录密码规则：前端提交 `sha1(明文)`，数据库存 `sha1(sha1(pwd)+salt)`；重置密码用启动参数 `--rest:super_user_pwd`（自动解锁+正确格式）

## 常用命令

```bash
# 后端编译（JAVA_HOME 指向 JDK17）
mvn compile

# 全量测试（默认执行；external/manual 标签自动排除）
mvn test

# 打包（-DskipTests 跳过测试；agent jar 存在时增量会跳过重建，需先 rm）
mvn package -DskipTests
rm -f modules/agent/target/agent-0.0.1.jar   # agent 强制重建

# 前端构建（dist 输出到 modules/server/src/main/resources/dist，打包进 server jar）
cd web-vue && npm run build

# 一键部署流水线（构建+打包+测试+启动+验证）
bash script/deploy.sh [--skip-tests] [--skip-frontend] [--pwd <密码>] [--no-captcha]

# UI 全页面巡检（需服务端运行；--baseline 生成基线 / --compare 对比 / 缺省 scan 模式）
# 巡检含登录页冒烟检查；自动化登录要求服务端禁用图形验证码，故需先 --no-captcha 部署
# 可选 --base <url> 覆盖地址（默认 http://127.0.0.1:2122）、--user <用户名>（默认 admin）
node script/ui-regression.mjs --pwd <密码> [--baseline | --compare] [--base <url>] [--user <用户名>]

# 本地启动（JDK17 需 add-opens）
export JAVA_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
cd modules/server/target/server-0.0.1-release && ./bin/Server.sh start
cd modules/agent/target/agent-0.0.1-release && ./bin/Agent.sh start
```

## 关键约定（防回归）

1. **包名/品牌**：根包 `io.voyager1`；禁止引入历史遗留品牌依赖；全仓关键字零残留
2. **环境变量**：一律 `VOYAGER1_*` 前缀（VOYAGER1_TYPE/VOYAGER1_VERSION/VOYAGER1_IS_DEBUG/VOYAGER1_REMOTE_VERSION_CACHE_FILE/VOYAGER1_REMOTE_VERSION_AUTH/JOIN_VOYAGER1_BETA_RELEASE/VOYAGER1_AGENT_APPLICATION/VOYAGER1_SERVER_APPLICATION/VOYAGER1_DATE_PATH）
3. **前端 UI 框架**：统一使用 Naive UI（`naive-ui ^2.45.3` + `unplugin-vue-components` 自动导入）。**禁止引入其它 UI 组件库**；新增 Naive 组件后需确认自动导入/类型（`components.d.ts`）已更新
4. **CustomTable 组件**：`<slot name="title" v-bind="slotProps || {}">` 必须保留 `|| {}` 兜底（title 插槽可能无参调用，slotProps 为 null，直接 v-bind 会抛 `null.key` 导致功能区消失）。当前改用 `<template #header>`（`title` 是 prop，不是 slot）
5. **启动脚本**：JDK8 专属参数（UseFastAccessorMethods 等）必须放在 `java_8` 分支内（历史兼容）；JDK17 运行需 add-opens
6. **日志路径**：`LogbackConfig` 回退路径需剥离 `jar!` 段；启动脚本需 export VOYAGER1_LOG
7. **系统任务**：启动时系统任务执行依赖 bean 就绪（SystemEvent 用 SmartInitializingSingleton）
8. **版本检查**：`remote-version-url` 未配置时静默降级（返回 null，不打 WARN）
9. **release 目录是打包产物**：修改源码后必须重新 `mvn package` 才生效（含前端 dist、bin 脚本权限 755）

## 常见坑备忘

- **登录失败排查**：先确认密码格式（前端 sha1），再确认账号锁定（多次失败锁 30 分钟，用 `--rest:super_user_pwd` 重置解锁）
- **H2 独占锁**：应用运行时不能直接连接 db 文件（只读也不行）
- **agent jar 不更新**：`mvn package` 时若 target 已有 jar 可能跳过重建，删掉再打
- **UI 巡检假阳性**：页面切换时请求 abort 产生的 `AxiosError: Network Error` 是 WARN 非 FAIL；全屏终端页（full-terminal/ssh-tabs）无参数渲染空白属正常
- **测试**：新增测试必须用 JUnit5（jupiter）、必须有断言；外部依赖测试加 `@Tag("external")`，人工维护类加 `@Tag("manual")`
