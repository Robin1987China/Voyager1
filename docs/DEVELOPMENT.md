# Voyager1 本地开发与部署指南

## 环境要求

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | Spring Boot 3 最低要求；JDK ≥ 26 与 lombok 不兼容 |
| Maven | 3.9.x | 后端构建 |
| Node.js | ≥ 22 | 前端构建 + UI 巡检 |

## 获取代码

```bash
git clone <your-repo-url>
cd voyager1
```

## 后端构建

```bash
export JAVA_HOME=<你的 JDK17 路径>

# 编译
mvn compile

# 全量测试（external / manual 标签自动排除）
mvn test

# 打包（跳过测试）
mvn package -DskipTests

# agent jar 增量跳过：若 target 已有 jar 可能不重建，需先删除
rm -f modules/agent/target/agent-0.0.1.jar
mvn package -DskipTests
```

## 前端构建

前端构建产物会输出到 `modules/server/src/main/resources/dist`，并随 server jar 打包。

```bash
cd web-vue
npm install
npm run build
```

## 本地启动

JDK 17 需要 add-opens 参数：

```bash
export JAVA_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"

cd modules/server/target/server-0.0.1-release
./bin/Server.sh start    # Server 默认端口 2122

cd modules/agent/target/agent-0.0.1-release
./bin/Agent.sh start     # Agent 默认端口 2123
```

## 登录与账号

- 默认超级管理员账号：**`sys`**
- 首次安装时通过 Web 界面设置密码；密码规则：前端提交 `sha1(明文)`，数据库存储 `sha1(sha1(密码)+salt)`
- 忘记密码/账号锁定时，可用启动参数重置：`--rest:super_user_pwd`

## 数据库切换

默认使用 H2（免外部依赖）。切换 MySQL / MariaDB / PostgreSQL 时，激活对应 profile：

```bash
java -jar server.jar --spring.profiles.active=mysql
```

对应配置：`modules/server/src/main/resources/application-{mysql,mariadb,postgresql}.yml`。

## 一键部署流水线

```bash
bash script/deploy.sh                      # 完整流水线（构建+打包+测试+部署+验证）
bash script/deploy.sh --skip-tests         # 跳过后端测试
bash script/deploy.sh --skip-frontend      # 跳过前端构建（用现有 dist）
bash script/deploy.sh --pwd <密码>          # 指定验证登录密码
```

## UI 巡检

```bash
# 生成基线 / 对比（需服务端运行）
node script/ui-regression.mjs --pwd <密码>
node script/ui-regression.mjs --pwd <密码> --compare
```

## 环境变量

一律使用 `VOYAGER1_*` 前缀，常用项：

| 变量 | 说明 |
|---|---|
| `VOYAGER1_TYPE` / `VOYAGER1_VERSION` | 应用类型 / 版本 |
| `VOYAGER1_IS_DEBUG` | 调试模式 |
| `VOYAGER1_REMOTE_VERSION_URL` | 版本检查地址（未配置时静默降级） |
| `VOYAGER1_ENCRYPT_AES_KEY` | AES 加密密钥（生产必须覆盖默认值） |
| `VOYAGER1_LOG` | 日志路径 |

> 安全相关默认值（Server Token / AES 密钥 / JWT 密钥）见 `SECURITY.md`，生产部署前务必更换。

## 测试约定

- 新增测试必须使用 JUnit 5（jupiter）且有断言
- 依赖外部环境的测试加 `@Tag("external")`；需人工维护的加 `@Tag("manual")`（`mvn test` 会自动排除这两类）

## 更多文档

- 架构说明：`docs/ARCHITECTURE.md`
- MCP 工具：`docs/mcp-tools.md`
- 贡献指南：`CONTRIBUTING.md`
- 安全政策：`SECURITY.md`
