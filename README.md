<p align="center">
  <img src="https://img.shields.io/badge/JDK-17+-blue.svg" alt="JDK"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-4EB1BA.svg" alt="License"/>
</p>

<p align="center">
  <strong>🚀 Voyager1 — AI 驱动的轻量级一体化运维与持续交付平台（在线构建 · 自动部署 · 日常运维 · 项目监控 · MCP/AIOps）</strong>
</p>

<p align="center">
  【<strong>更是一款原生 ops 软件</strong> / <a href="./README-en.md">English</a>】
</p>

## 😭 日常开发中，您是否有以下痛点？

- <font color="red">**团队中没有专业的运维，开发还要做运维的活**</font>，需要自己手动构建、部署项目。
- 不同的项目有不同的构建、部署命令。
- 有开发、测试、生产等多环境打包的需求。
- 需要同时监控多个项目的运行状态。
- <u>需要下载 SSH 工具</u>远程连接服务器。
- *需要下载 FTP 工具*传输文件到服务器。
- 多台服务器时，在不同电脑之间账号密码同步不方便。
- 想使用一些自动化工具，但是对服务器性能太高，搭建太麻烦。
- **对自动化工具有个性化的需求，想自己修改项目**，但是市面上的工具太复杂了。

> 如果是分布式的项目，以上步骤则更加繁琐。
>
> 让 Voyager1 来帮您解决这些痛点吧！然而，这些只是 Voyager1 解决的最基础的功能。

## 🤖 AI 能力（MCP · Agent · AIOps）

Voyager1 原生把核心运维能力暴露为 **MCP（Model Context Protocol）工具**，并内置 Agent 编排与自愈能力，让 AI 助手（Cursor、Claude Code 等）能安全地操作你的环境。

- **MCP Server（13 个工具）**：`/mcp` 端点（JSON-RPC 2.0），复用 JWT 鉴权与 `@Feature` 权限，把构建、部署、流水线、SSH、监控、K8s、云资源、AIOps 自愈等能力暴露给 AI Agent 调用。
  - 只读：`version.list` / `environment.list` / `build.list` / `log.get` / `monitor.list` / `k8s.resourceList` / `cloud.instanceList` / `selfHeal.diagnose`
  - 执行：`build.trigger` / `deploy.publish` / `pipeline.trigger` / `ssh.execute`
  - 人工闸门（HITL）：`pipeline.approval`
- **Agent 意图解析**：把自然语言（如「把 v1.2.3 部署到 test」）拆解为 MCP 工具调用序列；当前为规则版（关键词匹配），LLM 实现作为可插拔扩展（后续接入 OpenAI 兼容接口）。
- **Agent 审批闸门**：高危工具（`deploy.publish`、`ssh.execute`）需人工审批后才执行；内置危险命令黑名单（`rm -rf /`、`mkfs`、`dd`、`shutdown` 等）。
- **AIOps 自愈**：告警 → 根因 → 修复动作映射（如 `process_down` / `high_cpu` / `deploy_failed`），当前为规则版，可通过 MCP 工具 `selfHeal.diagnose` 触发，后续接入 LLM 网关。

> 详见 [MCP 工具说明](docs/mcp-tools.md) 与 [架构说明](docs/ARCHITECTURE.md)。

## 🛠️ 传统运维能力

- 方便的用户管理
	1. 用户操作监控，监控指定用户指定操作以邮件形式通知
	2. 多用户管理，用户项目权限独立（上传、删除权限可控制），完善的操作日志，使用工作空间隔离权限
- 界面形式实时查看项目运行状态、控制台日志、管理项目文件
	1. 在线修改项目文本文件
- Docker 容器管理、Docker Swarm 集群管理（**Docker UI**）
- **在线 SSH 终端**，让您在没有 PuTTY、Xshell、FinalShell 等软件也能轻松管理服务器
	1. 登录 Voyager1 系统后不需要知道服务器密码
	2. 能指定 SSH 禁止执行的命令，避免执行高风险命令，并且能自动执行命令日志
	3. 设置文件目录，在线查看管理对应项目文件及配置文件
	4. SSH 命令模版在线执行脚本还能定时执行
	5. 在线修改文本文件
	6. **轻量的实现了简单的"堡垒机"功能**
- 使用项目分发一键搞定集群项目多机部署
- 在线构建不用手动更新升级项目
	1. 支持拉取 GIT、SVN 仓库
	2. **支持容器构建（docker）**
	3. 支持 SSH 方式发布
	4. 支持定时构建
	5. 支持 WebHook 形式触发构建
- 支持在线编辑 nginx 配置文件并自动 reload 等操作
	1. 管理 nginx 状态，管理 SSL 证书
- 项目状态监控异常自动报警、自动尝试重启
	1. 支持邮件 + 钉钉群 + 微信群通知，主动感知项目运行状况
- 节点脚本模版+定时执行或者触发器，拓展更多功能
- 重要路径授权配置，杜绝用户误操作系统文件
- **Kubernetes（K8s）集群管理**：连接集群、部署/扩缩容/重启应用、查看 Pod 日志与事件
- **云资源管理**：云主机账号与云监控（FINops）成本分析，多方云厂商凭证管理
- **系统备份**：数据库全量/部分备份与恢复管理

### 🔔️ 特别提醒

> 1. 在 Windows 服务器中可能有部分功能因为系统特性造成兼容性问题，建议在实际使用中充分测试。Linux 目前兼容性良好
> 2. 服务端和插件端请安装到不同目录中，切勿安装到同一目录中
> 3. 卸载 Voyager1 插件端或者服务端，先停止对应服务，然后删除对应的程序文件、日志文件夹、数据目录文件夹即可
> 4. 本地构建依赖的是系统环境，如果构建命令需要使用 maven 或者 node
	 需要在构建项目的服务器安装好对应的环境。如果已经启动服务端再安装的对应环境需要通过命令行重启服务端后环境才会生效。
> 5. 在 Ubuntu/Debian 服务器作为插件端可能会添加失败，请在当前用户的根目录创建 .bash_profile 文件
> 6. 升级 2.7.x 后不建议降级操作，会涉及到数据不兼容的情况
> 7. 由于目前 2.x.x 版本插件端和服务端主要采用 http 协议通讯，插件端和服务端网络要求互通，在使用的时候请注意。
> 8. Voyager1 3.0 版本已经开始规划更新了，尽请期待新版本的诞生吧



## 📥 安装 Voyager1

Voyager1 采用 **Server + Agent** 架构，从源码构建即可部署。当前初始版本（v0.0.1）推荐**从源码编译安装**。

### 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 17（Spring Boot 3 最低要求；JDK ≥ 26 与 lombok 不兼容） |
| Maven | 3.9.x |
| Node.js | ≥ 22（前端构建） |

### 方式一：🚀 从源码编译安装（推荐）

#### 1. 克隆代码并构建

```shell
git clone https://github.com/Robin1987China/Voyager1.git
cd Voyager1
```

#### 2. 构建前端（dist 输出到 `modules/server/src/main/resources/dist`）

```shell
cd web-vue
npm install
npm run build
cd ..
```

#### 3. 构建后端并打包

```shell
# 打包（skip 测试；若 agent jar 已存在会增量跳过，需先删除强制重建）
rm -f modules/agent/target/agent-0.0.1.jar
mvn clean package
```

打包完成后，产物位于：
- 服务端：`modules/server/target/server-0.0.1-release/`
- 插件端：`modules/agent/target/agent-0.0.1-release/`

> 也可以使用 `script/release.sh`（Linux）或 `script/release.bat`（Windows）一键打包。

#### 4. 安装并启动服务端

将 `server-0.0.1-release` 目录上传到服务器（整个目录），进入目录后：

```shell
./bin/Server.sh start     # Linux
# 或 Windows: bin\Server.bat start
```

服务端默认端口 `2122`，访问 `http://127.0.0.1:2122/`（非本机访问换成服务器 IP）。

#### 5. 安装并启动插件端

将 `agent-0.0.1-release` 目录上传到被管主机（整个目录，**与服务端不要放在同一目录**），进入目录后：

```shell
./bin/Agent.sh start      # Linux
# 或 Windows: bin\Agent.bat start
```

插件端默认端口 `2123`，由服务端调用。

> ⚠️ 服务端与插件端务必安装在不同目录；两者通过 HTTP 通信，需网络互通。

> 如无法访问管理页，检查防火墙是否放行 2122 端口：
> ```bash
> firewall-cmd --add-port=2122/tcp --permanent && firewall-cmd --reload
> ```
> 云服务器还需在安全组放行 2122 端口。

### 方式二：⌨️ 本地开发运行

适合二次开发调试。

```shell
# 1. 启动服务端（IDE 运行）
io.voyager1.Voyager1ServerApplication       # 端口 2122

# 2. 启动插件端（IDE 运行）
io.voyager1.Voyager1AgentApplication        # 端口 2123，注意控制台打印的默认账号

# 3. 启动前端开发模式
cd web-vue && npm install && npm run dev    # 默认 http://127.0.0.1:3000/
```

### 方式三：📦 Docker Compose（从源码构建镜像）

提供 `docker-compose.yml` / `docker-compose-local.yml` / `docker-compose-cluster.yml`，基于源码 `build` 构建镜像（`Dockerfile.local`），无需预置官方镜像。

```shell
cd Voyager1
# 修改 .env 中的 SERVER_TOKEN（生产务必改为随机值）
docker-compose -f docker-compose.yml up --build
```

> 容器化仅提供服务端版；插件端功能依赖宿主机环境，容器化意义不大，建议插件端直接部署在被管主机上。

## 管理 Voyager1 命令

1. Windows 系统使用 bat 脚本文件。

```bash
# 服务端管理脚本 （命令行）
./bin/Server.bat start   # 启动Voyager1服务端
./bin/Server.bat stop    # 停止Voyager1服务端
./bin/Server.bat restart # 重启Voyager1服务端
./bin/Server.bat status  # 查看Voyager1服务端运行状态
# 服务端管理脚本 （控制面板），按照面板提示输入操作
./bin/Server.bat

# 插件端管理脚本
./bin/Agent.bat start   # 启动Voyager1插件端
./bin/Agent.bat stop    # 停止Voyager1插件端
./bin/Agent.bat restart # 重启Voyager1插件端
./bin/Agent.bat status  # 查看Voyager1插件端运行状态
# 插件端管理脚本（控制面板），按照面板提示输入操作
./bin/Agent.bat

```

> Windows 系统中执行启动后需要根据日志去跟进启动的状态，如果出现乱码请检查或者修改编码格式，Windows 系统中 bat
> 编码格式推荐为 `GB2312`

2. Linux 系统中使用 sh 脚本文件。

```bash
# 服务端
./bin/Server.sh start     # 启动Voyager1服务端
./bin/Server.sh stop      # 停止Voyager1服务端
./bin/Server.sh restart   # 重启Voyager1服务端
./bin/Server.sh status    # 查看Voyager1服务端运行状态
./bin/Service.sh install  # 创建Voyager1服务端的应用服务（voyager1-server）

# 插件端
./bin/Agent.sh start     # 启动Voyager1插件端
./bin/Agent.sh stop      # 停止Voyager1插件端
./bin/Agent.sh restart   # 重启Voyager1插件端
./bin/Agent.sh status    # 查看Voyager1插件端运行状态
./bin/Service.sh install # 创建Voyager1插件端的应用服务（voyager1-agent）
```

## Linux 服务方式管理

> 这里安装服务仅供参考，实际中可以根据需求自定义配置
>
> 在使用 `./bin/Service.sh install` 成功后
>
> systemctl {status | start | stop | restart} voyager1-server
>
> systemctl {status | start | stop | restart} voyager1-agent

## ⚙️ Voyager1 的参数配置

在项目运行的根路径下的 ：

### 程序配置  `./conf/application.yml`

1. 插件端示例：
2. 服务端示例：

### 项目日志  `./conf/logback.xml`

1. 插件端示例：
2. 服务端示例：

## 📝 常见问题、操作说明


### 实践案例

> 里面有部分图片加载可能比较慢


## 构建案例仓库代码


> Node.js 编译指定目录：

```bash
yarn --cwd xxxx/ install
yarn --cwd xxxx/ build
```

> Maven 编译指定目录：

```bash
mvn -f xxxx/pom.xml clean package
```

## 🛠️ 整体架构


## 🔨贡献指南


### 贡献须知

Voyager1 作为开源项目，离不开社区的支持，欢迎任何人修改和提出建议。贡献无论大小，您的贡献会帮助背后成千上万的使用者以及开发者，您做出的贡献也会永远的保留在项目的贡献者名单中，这也是开源项目的意义所在！

为了保证项目代码的质量与规范，以及帮助您更快的了解项目的结构，请在贡献之前阅读：


### 贡献步骤

1. Fork 本仓库。

2. Fork 后会在您的账号下多了一个和本仓库一模一样的仓库，把您账号的仓库 clone 到本地。

   注意替换掉链接中的`分支名`和`用户名`。

   如果是贡献代码/文档，请基于开发分支 `feature/v0.0.1` 提交（主分支 `main` 为发布版，不接受 PR）。

   ```bash
   git clone -b 分支名 https://github.com/用户名/Voyager1.git
   ```

3. 修改代码/文档，修改后提交上来。

   ```bash
   # 把修改的文件添加到暂存区
   git add .
   # 提交到本地仓库，说明您具体做了什么修改
   git commit -m '填写您做了什么修改'
   # 推送到远程仓库，分支名请使用 feature/v0.0.1
   git push origin 分支名
   ```

4. 登录您的仓库，然后会看到一条 PR 请求，点击请求合并，等待管理员把您的代码合并进来。

### 项目分支说明

| 分支 | 说明 |
|--------|------------------------------------------------------|
| main | 主分支（发布版），受保护分支。经测试通过后合入。 |
| feature/v0.0.1 | 开发分支，接受 PR。PR 请提交到该分支。 |

> 主分支 `main` 与开发分支 `feature/v0.0.1` 必须指向同一提交保持一致；功能/修复提交需同步到两个分支。

### 贡献者

Made with [contrib.rocks](https://contrib.rocks).

## ⚠️ 生产安全须知

部署到生产环境前，请务必修改以下默认配置，避免安全风险：

| 配置项 | 位置 | 默认值 | 说明 |
|---|---|---|---|
| 数据库密码 | `conf/application.yml` → `web.user-pwd` | `voyager1` | 默认开发值，生产必须改为强密码 |
| JWT 签名密钥 | `conf/application.yml` → `web.token-jwt-key` | 空 | 必须配置（建议 ≥16 位），否则鉴权存在风险 |
| 插件端 Agent 密码 | `conf/application.yml` → `agent-pwd` | 空 | 必须配置，用于 Server↔Agent 通信鉴权 |
| 登录图形验证码 | `web.disabled-captcha` | `false`（开启） | 生产建议保持开启，不要关闭 |

> 更多安全说明见 [安全政策](SECURITY.md)。

## 📖 文档

- [架构说明](docs/ARCHITECTURE.md) — 整体架构、模块结构、技术栈、核心设计
- [开发与部署指南](docs/DEVELOPMENT.md) — 环境要求、构建、启动、数据库切换、部署
- [贡献指南](CONTRIBUTING.md) — 环境搭建、开发约定、PR 流程
- [安全政策](SECURITY.md) — 支持的版本、安全注意事项、漏洞报告
- [MCP 工具](docs/mcp-tools.md) — MCP 工具说明

## 🤝 鸣谢

- 感谢 JetBrains 提供的免费开源 License：

