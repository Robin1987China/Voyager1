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
cd modules/server/target/server-0.0.2-release && ./bin/Server.sh start
cd modules/agent/target/agent-0.0.2-release && ./bin/Agent.sh start
```

## 关键约定（防回归）

1. **包名/品牌**：根包 `io.voyager1`；禁止引入历史遗留品牌依赖；全仓关键字零残留
2. **环境变量**：一律 `VOYAGER1_*` 前缀（VOYAGER1_TYPE/VOYAGER1_VERSION/VOYAGER1_IS_DEBUG/VOYAGER1_REMOTE_VERSION_CACHE_FILE/VOYAGER1_REMOTE_VERSION_AUTH/JOIN_VOYAGER1_BETA_RELEASE/VOYAGER1_AGENT_APPLICATION/VOYAGER1_SERVER_APPLICATION/VOYAGER1_DATE_PATH）
3. **前端已从 Ant Design Vue 迁移到 Naive UI**：ant-design-vue 依赖已完全移除（src 无 `a-*` 组件、package.json 无 antd），统一使用 Naive（`naive-ui ^2.45.3` + `unplugin-vue-components` 自动导入）。**禁止逆向引入 Ant 组件或 antd 依赖**；新增 Naive 组件后需确认自动导入/类型（`components.d.ts`）已更新
4. **CustomTable 组件**：`<slot name="title" v-bind="slotProps || {}">` 必须保留 `|| {}` 兜底（antd Card 的 title 插槽无参调用，slotProps 为 null，直接 v-bind 会抛 `null.key` 导致功能区消失）。当前改用 `<template #header>`（Naive 无 `#title` slot，见「Naive UI 迁移防回归」）
5. **启动脚本**：JDK8 专属参数（UseFastAccessorMethods 等）必须放在 `java_8` 分支内（历史兼容）；JDK17 运行需 add-opens
6. **日志路径**：`LogbackConfig` 回退路径需剥离 `jar!` 段；启动脚本需 export VOYAGER1_LOG
7. **系统任务**：启动时系统任务执行依赖 bean 就绪（SystemEvent 用 SmartInitializingSingleton）
8. **版本检查**：`remote-version-url` 未配置时静默降级（返回 null，不打 WARN）
9. **release 目录是打包产物**：修改源码后必须重新 `mvn package` 才生效（含前端 dist、bin 脚本权限 755）

## Naive UI 迁移防回归（Ant → Naive 语义差异，改 UI 必看）

迁移遗留了大量 Ant 与 Naive 的 API 语义差异，稍不注意就会让「列表页数据/操作列/工具栏/搜索框/弹窗」静默失效。以下是已踩过的坑，**禁止回退**：

| 坑 | Ant 写法（错） | Naive 正确写法 |
|---|---|---|
| `n-card` 无 `#title` slot | `<template #title>` | `<template #header>`（`title` 是 prop，不是 slot） |
| `n-grid` 只渲染 `n-grid-item` | `<n-grid justify="end"><n-form/></n-grid>` | 用 `div` + flex 布局包裹非 grid-item 内容 |
| `row-key` 必须是函数 | `row-key="id"`（字符串） | `:row-key="(row) => row.id"`（否则报 `getKey is not a function`，整表空白） |
| 数据源 prop 名 | `:options="list"` / 组件内部 `dataSource` 与页面 `:data` 不一致 | 原生 `n-data-table` 用 `:data="list"`；CustomTable 已兼容 `data` 别名回退 `dataSource` |
| 列字段名 | 迁移把列定义 `dataIndex`→`key`，但 slot 仍按 `column.dataIndex` 判断 | CustomTable 的 `toNaiveColumn` 需合成 `dataIndex`（`col.key ?? col.dataIndex`），否则操作列命中不了 |
| 表格体插槽双命名 | 页面用 `#bodyCell` 或 `#tableBodyCell` 不统一 | CustomTable 用 `slots.tableBodyCell \|\| slots.bodyCell` 双名兼容 |
| `n-tabs` 取值 | `v-model:active-key` / `#rightExtra` | `v-model:value` + `n-tab-pane :name`；页签 `:name` 必补，否则切换/关闭误删末位 |
| `n-form` 无 `@finish` | `@finish="submit"` | `@submit.prevent` + 手动 `this.$refs.xxx.validate()`；重置校验用 `restoreValidation()`（非 `resetFields()`） |
| `n-modal` 无 `@ok`/`@cancel` 事件 | `@ok/@cancel` | CustomModal 已封装：`emits['ok','cancel']` + `preset="card"` + footer；`#title` 改 `#header`；`:footer="null/false"` 隐藏死按钮 |
| `n-drawer` 插槽 | `#title/#footer/#extra` 直接挂 | 插槽由 `n-drawer-content` 承接；`@update:show` 需转发并补发 `close`/`update:open` |
| 下拉/单选事件 | `@change` / `props.onClick`（下拉菜单） | `n-select`/`n-radio-group` 用 `@update:value`；`n-dropdown` 用 `@select` |
| 穿梭框 | 依赖 `n-transfer` 的 `#children` 插槽 | 自研 `compositionTransfer` 自包含双面板+穿梭按钮（保留 `onChange(targetKeys,direction)` 契约） |
| 图标注册 | 只 import 不注册（普通 `<script>`） | 必须写进 `components` 注册（如 `ReloadOutlined` 等），否则渲染空白 |
| 自动刷新倒计时 | `n-statistic` 显示时间戳假倒计时 | `n-countdown` + `:key` remount 重置（`countdownKey++`） |
| 日志弹窗显隐 prop | 只认 `visible` | `logView` 需兼容 `show` 别名 + watch 同步 `visibleModel` |
| 表格横向滚动 | `scroll-x: 'max-content'`（Ant） | `n-data-table` 的 `scroll-x` 需数字像素；CustomTable 已把非数字值归一化为不设 scroll-x（否则表格宽度被撑成 100 万 px，非 fixed 列全被推出视口） |

## 常见坑备忘

- **登录失败排查**：先确认密码格式（前端 sha1），再确认账号锁定（多次失败锁 30 分钟，用 `--rest:super_user_pwd` 重置解锁）
- **H2 独占锁**：应用运行时不能直接连接 db 文件（只读也不行）
- **agent jar 不更新**：`mvn package` 时若 target 已有 jar 可能跳过重建，删掉再打
- **UI 巡检假阳性**：页面切换时请求 abort 产生的 `AxiosError: Network Error` 是 WARN 非 FAIL；全屏终端页（full-terminal/ssh-tabs）无参数渲染空白属正常
- **测试**：新增测试必须用 JUnit5（jupiter）、必须有断言；外部依赖测试加 `@Tag("external")`，人工维护类加 `@Tag("manual")`
