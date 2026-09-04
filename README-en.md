<p align="center">
  <img src="https://img.shields.io/badge/JDK-17+-blue.svg" alt="JDK"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-4EB1BA.svg" alt="License"/>
</p>

<p align="center">
  <strong>🚀 Voyager1 — AI-Powered Lightweight DevOps & Continuous Delivery Platform (Online Build · Auto Deploy · Daily Ops · Project Monitoring · MCP/AIOps)</strong>
</p>

<p align="center">
  【<strong>It is also a native ops software</strong> / <a href="./README.md">中文</a>】
</p>

## 😭 Do you experience these pain points in your daily development?

- <font color="red">**No dedicated operations team, so developers have to handle operations tasks**</font>, including manual project building and deployment.
- Different projects require different build and deployment commands.
- Need to package for various environments like development, testing, and production.
- Need to monitor the status of multiple projects simultaneously.
- <u>Need to download SSH tools</u> to remotely connect to servers.
- *Need to download FTP tools* to transfer files to servers.
- Syncing account passwords across multiple servers and computers is inconvenient.
- Want to use automation tools, but they are high-demanding on server performance and too complicated to set up.
- **Have specific needs for automation tools and want to modify the project**, but existing tools are too complex.

> For distributed projects, these steps are even more cumbersome.
>
> Let Voyager1 help you solve these pain points! And these are just the basic features that Voyager1 offers.

## 🤖 AI Capabilities (MCP · Agent · AIOps)

Voyager1 natively exposes its core operations capabilities as **MCP (Model Context Protocol) tools**, and ships with built-in Agent orchestration and self-healing, enabling AI assistants (Cursor, Claude Code, etc.) to safely operate your environment.

- **MCP Server (12 tools)**: `/mcp` endpoint (JSON-RPC 2.0), reusing JWT auth and `@Feature` permission, exposing build, deploy, pipeline, SSH, monitor, K8s, and cloud capabilities to AI agents.
  - Read-only: `version.list` / `environment.list` / `build.list` / `log.get` / `monitor.list` / `k8s.resourceList` / `cloud.instanceList`
  - Execute: `build.trigger` / `deploy.publish` / `pipeline.trigger` / `ssh.execute`
  - Human-in-the-loop (HITL): `pipeline.approval`
- **Agent Intent Parsing**: turns natural language (e.g. "deploy v1.2.3 to test") into MCP tool-call sequences. Currently rule-based (keyword matching), with LLM as a pluggable extension (OpenAI-compatible endpoints to follow).
- **Agent Approval Gate**: high-risk tools (`deploy.publish`, `ssh.execute`) require human approval before execution; built-in dangerous-command blacklist (`rm -rf /`, `mkfs`, `dd`, `shutdown`, etc.).
- **AIOps Self-Healing**: alert → root cause → remediation mapping (e.g. `process_down` / `high_cpu` / `deploy_failed`), currently rule-based, with an LLM gateway to follow.

> See [MCP Tools](docs/mcp-tools.md) and [Architecture](docs/ARCHITECTURE.md) for details.

## 🛠️ Traditional Operations Capabilities

- Convenient User Management
  1. User activity monitoring, with email notifications for specific user actions
  2. Multi-user management with independent project permissions (control over upload and delete rights), comprehensive operation logs, and workspace-based permission isolation
  3. Accounts can enable **MFA (Multi-Factor Authentication)** for security
- Real-time interface to view project status, console logs, and manage project files
	1. Edit project text files online
- Docker container management and Docker Swarm cluster management（**Docker UI**）
- **Online SSH Terminal**, allowing easy server management without using PuTTY, Xshell, FinalShell, etc.
	1. No need to know server passwords after logging into the Voyager1 system
	2. Specify forbidden SSH commands to prevent high-risk operations and automatically log command execution
	3. Set file directories to manage project files and configuration files online
	4. Execute SSH command templates and schedule scripts online
	5. Edit text files online
	6. **Lightweight implementation of basic"bastion host"functionality**
- One-click cluster project deployment using project distribution
- Online build process eliminates the need for manual project updates and upgrades
  1. Supports pulling from GIT and SVN repositories
  2. **Supports container builds (docker)**
	3. Supports SSH-based deployment
  4. Supports scheduled builds
	5. Supports WebHook-triggered builds
- Supports online editing of nginx configuration files and automatic reload operations
	1. Manage nginx status and SSL certificates
- Automatic alerts and restart attempts for abnormal project status
	1. Supports notifications via email, DingTalk groups, and WeChat groups, actively monitoring project status
- Node script templates with scheduling or triggers for expanded functionality
- Configurable authorization for important paths to prevent user errors with system files
- **Kubernetes (K8s) cluster management**: connect clusters, deploy/scale/restart applications, view Pod logs and events
- **Cloud resource management**: cloud host accounts and cloud monitoring (FINops) cost analysis, multi-cloud provider credential management
- **System backup**: full/partial database backup and restore management

### 🔔️ Special Reminders

> 1. On Windows servers, some features may have compatibility issues due to system characteristics. It is recommended to thoroughly test in practical use. Linux currently has good compatibility.
> 2. Install the server and plugin components in different directories; do not install them in the same directory.
> 3. To uninstall Voyager1 plugin or server components, first stop the corresponding service, then delete the related program files, log folders, and data directories.
> 4. Local builds depend on the system environment. If build commands require Maven or Node.js,
     ensure the corresponding environment is installed on the build server. If the environment is installed after the server is started, restart the server via the command line for the environment to take effect.
> 5. On Ubuntu/Debian servers, the plugin component may fail to add. Please create a .bash_profile file in the root directory of the current user.
> 6. After upgrading to version 2.7.x, downgrading is not recommended due to potential data incompatibility issues.
> 7. Currently, version 2.x.x uses HTTP for communication between the plugin and server components, so ensure network connectivity between them during use.
> 8. Voyager1 version 3.0 is already being planned. Stay tuned for the new release!



## 📥 Installing Voyager1

Voyager1 uses a **Server + Agent** architecture and can be built from source. For the initial release (v0.0.2), building from source is recommended.

### Environment Requirements

| Tool | Version |
|---|---|
| JDK | 17 (Spring Boot 3 minimum; JDK ≥ 26 is incompatible with lombok) |
| Maven | 3.9.x |
| Node.js | ≥ 22 (frontend build) |

### Method 1: 🚀 Build from Source (Recommended)

#### 1. Clone and Build

```shell
git clone https://github.com/Robin1987China/Voyager1.git
cd Voyager1
```

#### 2. Build the Frontend (dist output to `modules/server/src/main/resources/dist`)

```shell
cd web-vue
npm install
npm run build
cd ..
```

#### 3. Build the Backend and Package

```shell
# Package (skip tests; if the agent jar already exists it may be skipped, delete it first)
rm -f modules/agent/target/agent-0.0.2.jar
mvn clean package
```

After packaging, the artifacts are located at:
- Server: `modules/server/target/server-0.0.2-release/`
- Agent: `modules/agent/target/agent-0.0.2-release/`

> You can also use `script/release.sh` (Linux) or `script/release.bat` (Windows) for one-command packaging.

#### 4. Install and Start the Server

Upload the `server-0.0.2-release` directory to the server (the entire directory), then inside it:

```shell
./bin/Server.sh start     # Linux
# or Windows: bin\Server.bat start
```

The server default port is `2122`, accessible at `http://127.0.0.1:2122/` (replace `127.0.0.1` with the server IP if not local).

#### 5. Install and Start the Agent

Upload the `agent-0.0.2-release` directory to the managed host (the entire directory, **in a different directory from the server**), then inside it:

```shell
./bin/Agent.sh start      # Linux
# or Windows: bin\Agent.bat start
```

The agent default port is `2123`, used by the server.

> ⚠️ The server and agent must be installed in different directories; they communicate over HTTP and must have network connectivity.

> If you cannot access the management page, check whether the firewall allows port 2122:
> ```bash
> firewall-cmd --add-port=2122/tcp --permanent && firewall-cmd --reload
> ```
> For cloud servers, also allow port 2122 in the security group.

### Method 2: ⌨️ Local Development

For secondary development and debugging.

```shell
# 1. Start the server (run in IDE)
io.voyager1.Voyager1ServerApplication       # port 2122

# 2. Start the agent (run in IDE)
io.voyager1.Voyager1AgentApplication        # port 2123, note the default account printed

# 3. Start the frontend dev server
cd web-vue && npm install && npm run dev    # default http://127.0.0.1:3000/
```

### Method 3: 📦 Docker Compose (Build from Source)

Provides `docker-compose.yml` / `docker-compose-local.yml` / `docker-compose-cluster.yml`, building images from source (`Dockerfile.local`) — no pre-built official image required.

```shell
cd Voyager1
# Modify SERVER_TOKEN in .env (use a random value in production)
docker-compose -f docker-compose.yml up --build
```

> Containerization only provides the server version; agent functionality depends on the host environment, so containerizing the agent is not meaningful. Deploy the agent directly on managed hosts instead.

## Managing Voyager1 Commands

1. Using BAT Script Files on Windows

```bash
# Server management scripts (command line)
./bin/Server.bat start   # Start the Voyager1 server
./bin/Server.bat stop    # Stop the Voyager1 server
./bin/Server.bat restart # Restart the Voyager1 server
./bin/Server.bat status  # Check the Voyager1 server status
# Server management script (control panel), follow the panel prompts for operations
./bin/Server.bat

# Agent management scripts
./bin/Agent.bat start   # Start the Voyager1 agent
./bin/Agent.bat stop    # Stop the Voyager1 agent
./bin/Agent.bat restart # Restart the Voyager1 agent
./bin/Agent.bat status  # Check the Voyager1 agent status
# Agent management script (control panel), follow the panel prompts for operations
./bin/Agent.bat

```

> After executing the startup script on Windows, follow the logs to check the startup status. If you encounter garbled text, check or modify the encoding format. It is recommended to use
> `GB2312` for BAT script encoding on Windows.

2. Using SH Script Files on Linux

```bash
# Server management scripts
./bin/Server.sh start     # Start the Voyager1 server
./bin/Server.sh stop      # Stop the Voyager1 server
./bin/Server.sh restart   # Restart the Voyager1 server
./bin/Server.sh status    # Check the Voyager1 server status
./bin/Service.sh install  # Create a service for the Voyager1 server (voyager1-server)

# Agent management scripts
./bin/Agent.sh start     # Start the Voyager1 agent
./bin/Agent.sh stop      # Stop the Voyager1 agent
./bin/Agent.sh restart   # Restart the Voyager1 agent
./bin/Agent.sh status    # Check the Voyager1 agent status
./bin/Service.sh install # Create a service for the Voyager1 agent (voyager1-agent)
```

## Linux Service Management

> The following service installation instructions are for reference only; customize configurations as needed.
>
> After successfully using `./bin/Service.sh install`:
>
> systemctl {status | start | stop | restart} voyager1-server
>
> systemctl {status | start | stop | restart} voyager1-agent

## ⚙️ Voyager1 Configuration Parameters

Located in the project's root path:

### Application Configuration `./conf/application.yml`

1. Agent example:
2. Server example:

### Project Logs `./conf/logback.xml`

1. Agent example:
2. Server example:

## 📝 Frequently Asked Questions and User Guide


### Practical Examples

> Some images may load slowly.


## Example Code Repositories


> Node.js compile specific directory:

```bash
yarn --cwd xxxx/ install
yarn --cwd xxxx/ build
```

> Maven compile specific directory:

```bash
mvn -f xxxx/pom.xml clean package
```

## 🛠️ Overall Architecture



## 🔨Contribution Guide


### Contribution Guidelines

As an open-source project, Voyager1 relies on community support and welcomes contributions from everyone. Whether big or small, your contributions will help thousands of users and developers. Your contributions will also be permanently recorded in the list of contributors, which is the essence of open-source projects!

To ensure code quality and standards, and to help you quickly understand the project structure, please read the following before contributing:


### Contribution Steps

1. Fork this repository.

2. Clone your forked repository to your local machine.

   Replace `branch-name` and `username` with the appropriate values.

   For code/documentation contributions, base your work on the `feature/v0.0.2` development branch (the `main` branch is the release version and does not accept PRs).

   ```bash
   git clone -b branch-name https://github.com/username/Voyager1.git
   ```

3. Modify the code/documentation and commit your changes.

   ```bash
   # Add your changes to the staging area
   git add .
   # Commit your changes with a descriptive message
   git commit -m 'Describe your changes'
   # Push to your remote repository, using branch feature/v0.0.2
   git push origin branch-name
   ```

4. Create a Pull Request (PR).

   Go to your repository on GitHub, create a PR request, and wait for the administrators to merge your changes.

### Branch Explanation

| Branch | Description |
|--------|------------------------------------------------------|
| main | Main branch (release), protected. Merged after testing. |
| feature/v0.0.2 | Development branch, accepts PRs. Please submit PRs to this branch. |

> The `main` and `feature/v0.0.2` branches must point to the same commit to stay consistent; feature/fix commits are synced to both branches.

## ⚠️ Production Security Notes

Before deploying to production, be sure to change the following default settings to avoid security risks:

| Item | Location | Default | Notes |
|---|---|---|---|
| Database password | `conf/application.yml` → `web.user-pwd` | `voyager1` | Development default; must change to a strong password in production |
| JWT signing key | `conf/application.yml` → `web.token-jwt-key` | empty | Must be configured (≥16 chars recommended), otherwise auth is vulnerable |
| Agent password | `conf/application.yml` → `agent-pwd` | empty | Must be configured; used for Server↔Agent communication auth |
| Login captcha | `web.disabled-captcha` | `false` (enabled) | Keep it enabled in production; do not disable |

> See [Security Policy](SECURITY.md) for more details.

## 🤝 Acknowledgements

- Special thanks to JetBrains for providing a free open-source license:

