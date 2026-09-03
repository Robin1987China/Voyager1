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

Voyager1 supports various installation methods to meet different user needs. Just choose one method to install.

### Method 1: 🚀(Recommended) One-click Installation (Linux)

#### One-click Server Installation

> **Note: The installation directory is the current directory where the command is executed!**
>
> ⚠️ Special Reminder: When using the one-click installation, ensure the command is executed in different directories. The Server and Agent cannot be installed in the same directory!
>
> To change the data and log storage paths of the server,
> modify the `voyager1.path` configuration property in the file

```shell
# Default one-click installation
# Default one-click installation and automatic startup service configuration

# Install server and jdk environment
yum install -y wget && \
bash install.sh Server jdk

# Install server and jdk, maven environment
yum install -y wget && \
bash install.sh Server jdk+mvn

# ubuntu
apt-get install -y wget && \
bash install.sh Server jdk
```

After a successful startup, the server port is `2122`. You can access the management page via `http://127.0.0.1:2122/`
(if not accessing from the local machine, replace 127.0.0.1 with the IP address of the installed server).

> If you cannot access the management system, run the command `systemctl status firewalld` to check if the firewall is enabled.
> If you see `Active: active (running)` in green in the status bar, you need to allow port `2122`.
>
>```bash
># Allow port 2122 for the management system
>firewall-cmd --add-port=2122/tcp --permanent
># Reload the firewall to take effect
>firewall-cmd --reload
>```
>
>If you have allowed the port in the operating system but still cannot access it, and you are using a cloud server, check the security group rules in the cloud server's control panel to see if port 2122 is allowed.
>
>⚠️ Note: There are multiple firewalls in Linux systems: Firewall, Iptables, SELinux, etc. When checking firewall configurations, make sure to check all of them.

#### One-Click Agent Installation

> If the server where the server side is installed also needs to be managed, you need to install the agent on the server side as well (both the server and agent can be installed on the same server but in different directories).
>
> ⚠️ Special reminder: Do not execute the one-click installation command in the same directory for both the Server and Agent!
>
> If you need to modify the agent data and log storage paths, update the `voyager1.path` configuration property in the file

```shell
# Default one-click installation
# Default one-click installation and auto-configure startup service

# Install agent and JDK environment
yum install -y wget && \
bash install.sh Agent jdk

# ubuntu
apt-get install -y wget && \
bash install.sh Agent jdk
```

After a successful startup, the agent port is `2123`, which is used by the server.

### Method 2: 📦 Container Installation


#### One-Command Installation

```shell
```

#### Using Mount to Store Data (may have compatibility issues in some environments)

1. Alibaba Cloud Repository

```shell
mkdir -p /home/voyager1-server/logs
mkdir -p /home/voyager1-server/data
mkdir -p /home/voyager1-server/conf
docker run -d -p 2122:2122 \
	--name voyager1-server \
	-v /home/voyager1-server/logs:/usr/local/voyager1-server/logs \
	-v /home/voyager1-server/data:/usr/local/voyager1-server/data \
	-v /home/voyager1-server/conf:/usr/local/voyager1-server/conf \
```

2. Docker Hub Repository

```shell
mkdir -p /home/voyager1-server/logs
mkdir -p /home/voyager1-server/data
mkdir -p /home/voyager1-server/conf
docker run -d -p 2122:2122 \
	--name voyager1-server \
	-v /home/voyager1-server/logs:/usr/local/voyager1-server/logs \
	-v /home/voyager1-server/data:/usr/local/voyager1-server/data \
	-v /home/voyager1-server/conf:/usr/local/voyager1-server/conf \
```

#### Using Docker Volumes to Store Data

1. Alibaba Cloud Repository

```shell
docker volume create voyager1-server-data
docker volume create voyager1-server-logs
docker volume create voyager1-server-conf
docker run -d -p 2122:2122 \
	--name voyager1-server \
	-v voyager1-server-data:/usr/local/voyager1-server/data \
	-v voyager1-server-logs:/usr/local/voyager1-server/logs \
	-v voyager1-server-conf:/usr/local/voyager1-server/conf \
```

2. Docker Hub Repository

```shell
docker volume create voyager1-server-data
docker volume create voyager1-server-logs
docker volume create voyager1-server-conf
docker run -d -p 2122:2122 \
	--name voyager1-server \
	-v voyager1-server-data:/usr/local/voyager1-server/data \
	-v voyager1-server-logs:/usr/local/voyager1-server/logs \
	-v voyager1-server-conf:/usr/local/voyager1-server/conf \
```

> Container installation only provides the server version. Due to isolation between the container and the host environment, many functionalities of the agent cannot be used properly. Therefore, containerization of the agent is not very meaningful.
>
> For more information on installing Docker, configuring images, auto-start, and locating the installation directory, refer to the documentation
>
> In lower versions of Docker, you may encounter the error `ls: cannot access'/usr/local/voyager1-server/lib/': Operation not permitted`
> In this case, add the `--privileged` parameter

### Method 3: 💾 Download and Install

2. Extract the files
3. Install the agent:
	1. The `agent-x.x.x-release` directory contains all the installation files for the agent
	2. Upload the entire directory to the corresponding server
	3. Start the agent. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the agent is `2123`
4. Install the server:
	1. The `server-x.x.x-release` directory contains all the installation files for the server
	2. Upload the entire directory to the corresponding server
	3. Start the server. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the server is `2122`. Access the management page at `http://127.0.0.1:2122/` (if not accessed locally, replace `127.0.0.1` with your server's IP address)

### Method 4: ⌨️ Compile and Install

2. Switch to the `web-vue` directory and run `npm install` (you need to have the Vue environment set up in advance; refer to the README.md in the web-vue directory for details)
3. Run `npm run build` to package the Vue project
4. Switch to the project root directory and run: `mvn clean package`
5. Install the agent:
	1. Check the agent installation package in `modules/agent/target/agent-x.x.x-release`
	2. Upload the entire directory to the server
	3. Start the agent. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the agent is `2123`
6. Install the server:
	1. Check the server installation package in `modules/server/target/server-x.x.x-release`
	2. Upload the entire directory to the server
	3. Start the server. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the server is `2122`. Access the management page at `http://127.0.0.1:2122/` (if not accessed locally, replace `127.0.0.1` with your server's IP address)

> You can also use `script/release.bat` or `script/release.sh` for quick packaging.

### Method 5: 📦 One-Click Start with Docker-Compose

- No environment installation required; automatically compiles and builds

> Note: Remember to modify the token value in the `.env` file

```shell
yum install -y git
cd Voyager1
docker-compose -f docker-compose.yml up
# docker-compose -f docker-compose.yml up --build
# docker-compose -f docker-compose.yml build --no-cache
# docker-compose -f docker-compose-local.yml up
# docker-compose -f docker-compose-local.yml build --build-arg TEMP_VERSION=.0
# docker-compose -f docker-compose-cluster.yml up --build
```

### Method 6: 💻 Compile and Run

   dev branch)
2. Run the agent:
	1. Run `io.voyager1.Voyager1AgentApplication`
	2. Note the default username and password information printed in the console.
	3. The agent's default running port: `2123`
3. Run the server:
	1. Run `io.voyager1.Voyager1ServerApplication`
	2. The server's default running port: `2122`
4. Build the Vue page, switch to the `web-vue` directory (make sure you have node and npm environments set up locally).
5. Install the Vue project dependencies by executing `npm install` in the console.
6. Start development mode by executing `npm run dev` in the console.
7. Access the frontend page using the address output in the console: `http://127.0.0.1:3000/` (if not accessing from the local machine, replace `127.0.0.1` with your server's IP address).

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

   Use `dev` for code contributions and `docs` for documentation contributions.

   ```bash
   git clone -b branch-name https://github.com/username/Voyager1.git
   ```

3. Modify the code/documentation and commit your changes.

   ```bash
   # Add your changes to the staging area
   git add .
   # Commit your changes with a descriptive message
   git commit -m 'Describe your changes'
   # Push to your remote repository, replacing branch-name with dev or docs
   git push origin branch-name
   ```

4. Create a Pull Request (PR).

   Go to your repository on GitHub, create a PR request, and wait for the administrators to merge your changes.

### Branch Explanation

| Branch     | Description                                                   |
|--------|------------------------------------------------------|
| master | Main branch, protected. Does not accept PRs. Merges from the beta branch after testing.       |
| beta   | 	Beta version branch, protected. Does not accept PRs. Merges from the dev branch after testing. |
| dev    | Development branch, accepts PRs. Please submit PRs to the dev branch.                           |
| docs   | Documentation branch, accepts PRs. Used for project documentation, feature introductions, and FAQ summaries.                         |

> Primarily use the dev and docs branches for PR submissions. Other branches are for archiving and can be ignored by contributors.

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

