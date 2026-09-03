# Contributing to Voyager1

感谢你对 Voyager1 的关注！本项目采用 Server（服务端）+ Agent（节点端）的轻量级运维平台架构。欢迎通过提交 issue、PR、文档改进等方式参与贡献。

## 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 17（Spring Boot 3 最低要求；JDK ≥ 26 与 lombok 不兼容） |
| Maven | 3.9.x |
| Node.js | ≥ 22（前端构建 + UI 巡检） |

## 快速开始

```bash
# 后端编译（JAVA_HOME 指向 JDK17）
mvn compile

# 全量测试（external / manual 标签自动排除）
mvn test

# 打包（跳过测试；agent jar 存在时会增量跳过，需先删除）
mvn package -DskipTests
rm -f modules/agent/target/agent-0.0.1.jar

# 前端构建（dist 输出到 modules/server/src/main/resources/dist）
cd web-vue && npm run build
```

## 项目结构

```
modules/
├── server              # 服务端主程序（Web 控制台）
├── agent               # 节点端主程序
├── common              # 共享基础类
├── agent-transport     # Server ↔ Agent 传输层（SPI）
├── storage-module      # 数据库方言实现（h2/mysql/mariadb/postgresql）
└── sub-plugin          # 功能插件（git/svn/docker/ssh/email/webhook/encrypt）
```

## 开发约定

- 新功能 / 行为变更请走 **OpenSpec 规格驱动闭环**：`/opsx-propose` → `/opsx-apply` → `/opsx-archive`，主规格位于 `openspec/specs/`。
- 根包名 `io.voyager1`（兼容入口 `io.voyager1`）；环境变量一律 `VOYAGER1_*` 前缀。
- 新增测试必须使用 **JUnit 5（jupiter）** 且带断言；依赖外部环境的测试加 `@Tag("external")`，需人工维护的加 `@Tag("manual")`。
- 前端统一使用 **Naive UI**（`naive-ui ^2.45.3` + `unplugin-vue-components` 自动导入），已完全移除 ant-design-vue，禁止逆向引入 Ant 组件；`package-lock.json` 已纳入版本控制，请勿改回 `^` 或升级后不更新 lock。

## 提交与 PR 流程

1. 从 `main` 拉取最新代码，创建功能分支。
2. 保持提交信息清晰，说明「为什么改」而非只写「改了什么」。
3. 确保 `mvn compile` 与 `mvn test` 通过后再提交。
4. 提交 PR，描述变更点与影响范围；涉及行为变更时附上对应的 OpenSpec change。

## 报告问题

- Bug 或功能建议：提 GitHub issue，附上复现步骤、版本号、相关日志。
- 安全漏洞：请勿公开 issue，改用 **Security → Report a vulnerability**（见 `SECURITY.md`）。
