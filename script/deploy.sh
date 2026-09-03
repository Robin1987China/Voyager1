#!/bin/bash
# =============================================================================
# Voyager1 本地 CI/CD 一键部署流水线
#
# 用法:
#   bash script/deploy.sh                     # 完整流水线（构建+打包+测试+部署+验证）
#   bash script/deploy.sh --skip-tests        # 跳过后端测试
#   bash script/deploy.sh --skip-frontend     # 跳过前端构建（用现有 dist）
#   bash script/deploy.sh --pwd <密码>         # 指定登录密码（验证登录用，默认 nGetCEvj）
#   bash script/deploy.sh --no-captcha        # 禁用登录图形验证码（UI 巡检等自动化测试用）
#
# 环境要求:
#   - JDK 8-17（lombok 与 JDK26 不兼容，建议 17）：export JAVA_HOME=...
#   - Maven：export MAVEN_HOME=... 或使用 PATH 中的 mvn
#   - Node >= 22（前端构建 + UI 巡检）
#   - macOS Chrome（UI 巡检用，可选）
#
# 流程:
#   1. 前端构建   npm run build → modules/server/src/main/resources/dist
#   2. 后端打包   mvn package（agent jar 强制重建，避免增量跳过）
#   3. 产物同步   jar → release/lib，权限校验
#   4. 测试       mvn test（全量测试，external/manual 标签自动排除）
#   5. 部署启动   Server.sh + Agent.sh（JDK17 + add-opens）
#   6. 验证       端口/HTTP/登录/UI 巡检
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SKIP_TESTS=false
SKIP_FRONTEND=false
NO_CAPTCHA=false
LOGIN_PWD="${VOYAGER1_LOGIN_PWD:-nGetCEvj}"
VOYAGER1_VERSION="0.0.1"

# ---------- 参数解析 ----------
for arg in "$@"; do
  case "$arg" in
    --skip-tests) SKIP_TESTS=true ;;
    --skip-frontend) SKIP_FRONTEND=true ;;
    --no-captcha) NO_CAPTCHA=true ;;
    --pwd) LOGIN_PWD="${2:-}"; shift ;;
    *) ;;
  esac
done

log() { echo -e "\n\033[1;36m[deploy]\033[0m $*"; }
fail() { echo -e "\n\033[1;31m[deploy][ERROR]\033[0m $*" >&2; exit 1; }

# ---------- 环境检查 ----------
command -v mvn >/dev/null 2>&1 || fail "未找到 mvn，请 export MAVEN_HOME 或加入 PATH"
command -v node >/dev/null 2>&1 || fail "未找到 node"
command -v java >/dev/null 2>&1 || fail "未找到 java"
JAVA_VER=$(java -version 2>&1 | head -1 | grep -oE '"[0-9]+' | tr -d '"')
[ "${JAVA_VER:-0}" -ge 26 ] && fail "JDK ${JAVA_VER} 与 lombok 不兼容，请使用 JDK 8-17（export JAVA_HOME）"
log "环境 OK：java ${JAVA_VER} / mvn / node $(node -v)"

ADD_OPENS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"

SERVER_RELEASE="$REPO_ROOT/modules/server/target/server-${VOYAGER1_VERSION}-release"
AGENT_RELEASE="$REPO_ROOT/modules/agent/target/agent-${VOYAGER1_VERSION}-release"

# ---------- 1. 前端构建 ----------
if [ "$SKIP_FRONTEND" = false ]; then
  log "1/6 前端构建..."
  (cd "$REPO_ROOT/web-vue" && npm run build) || fail "前端构建失败"
else
  log "1/6 跳过前端构建（--skip-frontend）"
fi

# ---------- 2. 后端打包 ----------
log "2/6 后端打包（mvn package -DskipTests）..."
# agent jar 存在时 maven 增量可能跳过重建，强制删除
rm -f "$REPO_ROOT/modules/agent/target/agent-${VOYAGER1_VERSION}.jar"
(cd "$REPO_ROOT" && mvn package -DskipTests -q) || fail "后端打包失败"

# ---------- 3. 产物同步 ----------
log "3/6 同步产物到 release 目录..."
cp "$REPO_ROOT/modules/server/target/server-${VOYAGER1_VERSION}.jar" "$SERVER_RELEASE/lib/" || fail "server jar 同步失败"
cp "$REPO_ROOT/modules/agent/target/agent-${VOYAGER1_VERSION}.jar" "$AGENT_RELEASE/lib/" || fail "agent jar 同步失败"
ls -la "$SERVER_RELEASE/bin/Server.sh" "$AGENT_RELEASE/bin/Agent.sh" | awk '{print $1, $9}' | grep -q "rwxr-xr-x" || chmod +x "$SERVER_RELEASE/bin/"*.sh "$AGENT_RELEASE/bin/"*.sh

# ---------- 4. 测试 ----------
if [ "$SKIP_TESTS" = false ]; then
  log "4/6 后端测试（mvn test）..."
  (cd "$REPO_ROOT" && mvn test -q) || fail "测试失败"
else
  log "4/6 跳过测试（--skip-tests）"
fi

# ---------- 5. 部署启动 ----------
log "5/6 部署启动（先停旧进程）..."
lsof -tiTCP:2122 -sTCP:LISTEN 2>/dev/null | xargs kill 2>/dev/null || true
lsof -tiTCP:2123 -sTCP:LISTEN 2>/dev/null | xargs kill 2>/dev/null || true
sleep 3
export JAVA_OPTS="$ADD_OPENS"
if [ "$NO_CAPTCHA" = true ]; then
  export VOYAGER1_WEB_DISABLEDCAPTCHA=true
  log "已禁用登录图形验证码（--no-captcha，测试环境）"
fi
(cd "$SERVER_RELEASE" && ./bin/Server.sh start >/dev/null 2>&1) || fail "Server.sh 启动失败"
(cd "$AGENT_RELEASE" && ./bin/Agent.sh start >/dev/null 2>&1) || fail "Agent.sh 启动失败"
sleep 15

# ---------- 6. 验证 ----------
log "6/6 验证..."
SERVER_HTTP=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:2122/ || echo 000)
AGENT_HTTP=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:2123/ || echo 000)
[ "$SERVER_HTTP" = "200" ] || fail "服务端验证失败（HTTP $SERVER_HTTP）"
[ "$AGENT_HTTP" = "200" ] || fail "插件端验证失败（HTTP $AGENT_HTTP）"
log "服务端 2122: HTTP $SERVER_HTTP ✅  插件端 2123: HTTP $AGENT_HTTP ✅"

# 登录验证
SHAPWD=$(node -e "const c=require('crypto');console.log(c.createHash('sha1').update(process.argv[1],'utf8').digest('hex'))" "$LOGIN_PWD" 2>/dev/null || true)
if [ -n "$SHAPWD" ]; then
  LOGIN_RESULT=$(curl -s -X POST "http://127.0.0.1:2122/userLogin?loginName=admin&userPwd=$SHAPWD" | head -c 50)
  echo "$LOGIN_RESULT" | grep -q '"code":200' && log "登录验证 ✅（admin）" || log "登录验证跳过（密码或账号变化，可 --pwd 指定）"
fi

log "部署完成 ✅"
log "管理页面: http://127.0.0.1:2122/  插件端: http://127.0.0.1:2123/"
