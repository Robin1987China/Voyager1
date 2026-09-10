# AGENTS.md

Voyager1 项目开发指南（供 AI agent 与开发者使用）

## 项目概览

Voyager1 是自研的轻量级运维平台（在线构建、自动部署、日常运维、项目监控），采用 **Server + Agent** 架构：

- **Server**（端口 2122）：Web 控制台，管理应用交付 / 构建 / 环境 / 监控 / SSH / Docker / 权限
- **Agent**（端口 2123）：部署在被管主机上的插件端，执行构建 / 脚本 / 进程管理

项目根包为 `io.voyager1`。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.5.16 + Maven 多模块 |
| 前端 | Vue 3.5 + Vite 5 + TypeScript + Naive UI（`web-vue/`），i18n 四语言 |
| 存储 | H2 默认（Spring profile 可切 MySQL / MariaDB / PostgreSQL），Flyway 迁移 |
| 通信 | HTTP + WebSocket（终端 / 日志流）+ JSch SSH + JGit / SvnKit |

## 模块结构（`modules/`）

- `server`：服务端主程序（controller / service / socket / 权限 / oauth2 / monitor）
- `agent`：插件端主程序
- `common`：共享基础类与跨端契约（含 `io.voyager1.core.api.ApiResult` 统一响应体）
- `agent-transport`：Server↔Agent 传输层（SPI）
- `sub-plugin`：功能插件（docker-cli / email / encrypt / git-clone / ssh-jsch / svn-clone / webhook）

## 业务能力

| 能力 | 说明 | 关键代码 |
|---|---|---|
| 版本状态机 | 构建产物版本、发布状态流转（Developing→Submitted→Released，提测冻结 CI / 打回解冻） | `service/version/VersionService.java`、`pages/pipeline/version-list.vue` |
| 应用管理 | 应用绑定代码仓库 + 构建配置（外键校验），详情聚合环境泳道 / 构建历史 / 部署记录 | `service/application/ApplicationService.java`、`pages/application/` |
| 环境化 CD | dev/test/prod 环境定义（策略 CI_CD/CD_ONLY、审批闸门）+ 目标绑定（NODE / K8S / SSH，工作区权限校验）；部署挂版本状态机；异步执行，每次部署独立发布记录 | `service/environment/{EnvironmentService,DeploymentService}.java`、`pages/environment/` |
| 部署审批 | 需审批环境落「待审批」记录，`POST /environment/approve-deploy` 批准后异步执行 / 拒绝闭环 | `DeploymentService.approve`、`pages/environment/deploy-records.vue` |
| 触发方式 | 手动 / cron 定时 / WebHook 触发构建；提测后自动 CD 到 test（`VOYAGER1_ENV_AUTO_CD` 可关）、发布后自动部署 prod | `service/version/VersionService.java`、`build/BuildExecuteService.java` |
| 云资产 | 云账号（aliyun / tencent / aws）+ 云实例管理，实例一键导入为 SSH 机器 | `service/cloud/`、`pages/cloud/cloud-list.vue` |
| K8s 集群 | kubeconfig 接入集群、资源列表 / 详情 / 删除 / 扩缩容 / 重启 / 日志 / 事件、manifest 部署（fabric8 SDK） | `service/k8s/K8sService.java`、`pages/k8s/k8s-list.vue` |
| FinOps | 账单导入 / 同步、成本分析、标签归集、预算检查、闲置资源 | `service/finops/`、`pages/finops/finops-list.vue` |

## 开发环境

- **JDK 17**：必须（Spring Boot 3 最低要求；JDK ≥ 26 与 lombok 不兼容）
- **Maven 3.9.x**、**Node ≥ 22**（前端构建 + UI 巡检）
- 登录密码规则：前端提交 `sha1(明文)`，数据库存 `sha1(sha1(pwd)+salt)`；重置密码用启动参数 `--rest:super_user_pwd`（自动解锁 + 正确格式）

## 常用命令

```bash
# 后端编译（JAVA_HOME 指向 JDK17）
mvn compile

# 全量测试（默认执行；external/manual 标签自动排除）
mvn test

# 打包（-DskipTests 跳过测试；agent jar 存在时增量会跳过重建，需先 rm）
mvn package -DskipTests
rm -f modules/agent/target/agent-0.0.2.jar   # agent 强制重建

# 前端构建（dist 输出到 modules/server/src/main/resources/dist，打包进 server jar）
cd web-vue && npm run build

# 一键部署流水线（构建 + 打包 + 测试 + 启动 + 验证）
bash script/deploy.sh [--skip-tests] [--skip-frontend] [--pwd <密码>] [--base <主机>] [--no-captcha]

# UI 全页面巡检（需服务端运行；--baseline 生成基线 / --compare 对比 / 缺省 scan）
# 含登录页冒烟；自动化登录要求服务端禁用图形验证码，故需先 --no-captcha 部署
node script/ui-regression.mjs --pwd <密码> [--baseline | --compare] [--base <url>] [--user <用户名>]

# 版本状态机 + 环境化 CD 端到端验证（需服务端运行）
bash script/e2e-pipeline.sh [明文密码] [base_url]

# 本地启动（JDK17 需 add-opens）
export JAVA_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
cd modules/server/target/server-0.0.2-release && ./bin/Server.sh start
cd modules/agent/target/agent-0.0.2-release && ./bin/Agent.sh start
```

## 关键约定（防回归）

1. **环境变量**：一律 `VOYAGER1_*` 前缀
2. **前端 UI 框架**：统一 Naive UI（`naive-ui ^2.45.3` + `unplugin-vue-components` 自动导入），禁止引入其它 UI 组件库；新增 Naive 组件后需确认 `components.d.ts` 已更新
3. **前端主题**：颜色统一走 `web-vue/src/theme/dsh.ts`（调色板唯一权威 + `--dsh-*` CSS 变量），禁止硬编码色值
4. **前端 i18n**：文案统一走 `$t('i18n_*')`，四语言文件 `src/i18n/locales/{zh_cn,en_us,zh_hk,zh_tw}.json` 必须同步
5. **CustomTable**：`<slot name="title" v-bind="slotProps || {}">` 必须保留 `|| {}` 兜底（title 插槽可能无参调用，slotProps 为 null）
6. **启动脚本**：JDK8 专属参数必须放在 `java_8` 分支内；JDK17 运行需 add-opens
7. **日志路径**：`LogbackConfig` 回退路径需剥离 `jar!` 段；启动脚本需 export VOYAGER1_LOG
8. **版本检查**：`remote-version-url` 未配置时静默降级（返回 null，不打 WARN）
9. **release 目录含运行时数据，禁止 `mvn clean`**：`target/server-0.0.2-release/` 里除了打包产物，还有 `db/`（H2 数据库）、`conf/`（含 `disabled-captcha` 等配置）、`logs/`。`mvn clean` 会把它们**连库一起删掉且不可恢复**。
   前端改动后安全重建方式（只清 dist 缓存，不碰运行数据）：
   ```bash
   cd web-vue && npm run build
   cd .. && rm -rf modules/server/target/classes/dist && mvn -pl modules/server -am package -DskipTests
   ```

## 常见坑备忘

- **登录失败排查**：先确认密码格式（前端 sha1），再确认账号锁定（多次失败锁 30 分钟，用 `--rest:super_user_pwd` 重置解锁）
- **H2 独占锁**：应用运行时不能直接连接 db 文件（只读也不行）
- **数据库就在 release 目录里**：跑过服务的 `target/server-0.0.2-release/db/Server.mv.db` 即真实数据库；`mvn clean` 会连它一起删除（不可恢复）。只想重建前端产物时：`npm run build` + `rm -rf target/classes/dist` + `mvn -pl modules/server -am package -DskipTests`
- **agent jar 不更新**：`mvn package` 时若 target 已有 jar 可能跳过重建，删掉再打
- **UI 巡检假阳性**：页面切换时请求 abort 产生的 `AxiosError: Network Error` 是 WARN 非 FAIL；全屏终端页（full-terminal / ssh-tabs）无参数渲染空白属正常
- **测试**：新增测试必须用 JUnit5（jupiter）、必须有断言；外部依赖测试加 `@Tag("external")`，人工维护类加 `@Tag("manual")`
- **macOS 网络**：lo0 默认只有 127.0.0.1，访问 127.0.0.2 会挂起；脚本一律用 127.0.0.1（可 `--base` 覆盖），curl 必加 `--max-time`
- **macOS bash 3.2**：中文字符串内插变量必须写 `${VAR}`（否则 set -u 下误报 `unbound variable`）；`tail --pid` 为 GNU 专属，脚本需做 BSD 回退
- **改 Flyway 迁移后**：已落地的 dev/测试库会因 checksum 变化启动校验失败，删除对应 H2 文件重建（测试库在 `$TMPDIR/voyager1-test-data`）
