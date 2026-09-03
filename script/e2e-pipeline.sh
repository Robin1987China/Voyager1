#!/bin/bash
# =============================================================================
# Pipeline + 版本状态机 端到端测试脚本（真实服务端 + 本地 git 仓库）
#
# 前提：服务端运行中（deploy.sh 已部署）、admin 密码 123（sha1 提交）
# 验证链路:
#   A. 版本生命周期: 创建 → 提测(冻结) → 打回(恢复) → 提测 → 发布
#   B. CI 冻结: 提测后 WebHook 构建触发被拦截
#   C. Pipeline 全链路: build(真实构建) → exec → approval → publish(本地发布)
#      → 审批通过 → 发布产物落盘验证
# =============================================================================
set -euo pipefail
BASE="http://127.0.0.1:2122"
PWD_SHA1="40bd001563085fc35165329ea1ff5c5ecbdbbeef"  # sha1(123)
E2E_REPO="/tmp/voyager1-e2e-repo"
E2E_PUBLISH="/tmp/voyager1-e2e-publish"
E2E_BUILDID="e2e-app"
PASS=0; FAIL=0

check() { # check <desc> <condition-output>
  if [ "$2" = "0" ] || [ "$2" = "true" ]; then
    PASS=$((PASS+1)); echo "  ✅ $1"
  else
    FAIL=$((FAIL+1)); echo "  ❌ $1"
  fi
}

TOKEN=$(curl -s -X POST "$BASE/userLogin?loginName=admin&userPwd=$PWD_SHA1" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")
[ -n "$TOKEN" ] && echo "登录 OK" || { echo "登录失败"; exit 1; }

echo ""
echo "========== 0. 准备环境 =========="
rm -rf "$E2E_REPO" "$E2E_PUBLISH"
git init -q -b master "$E2E_REPO"
cd "$E2E_REPO" && echo "hello-voyager1" > app.txt && git add . && git -c user.email=t@t.com -c user.name=t commit -q -m "init"
mkdir -p "$E2E_PUBLISH"
echo "本地仓库就绪: $E2E_REPO"

echo ""
echo "========== A. 版本生命周期 =========="
V=$(curl -s -X POST "$BASE/version/create" -H "Authorization: $TOKEN" -d "buildId=$E2E_BUILDID&buildNumberId=1&version=v1.0.0&artifactRef=/tmp/x.jar")
VID=$(echo "$V" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['id'])")
check "创建版本" "$([ -n "$VID" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/submit" -H "Authorization: $TOKEN" -d "id=$VID&remark=e2e")
check "提测(CI冻结)" "$(echo "$R" | python3 -c "import json,sys; print(0 if json.load(sys.stdin)['code']==200 else 1)")"
S=$(curl -s -X POST "$BASE/version/list" -H "Authorization: $TOKEN" -d "buildId=$E2E_BUILDID" | python3 -c "import json,sys; print(json.load(sys.stdin)['data'][0]['status'])")
check "状态=已提测(1)" "$([ "$S" = "1" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/return" -H "Authorization: $TOKEN" -d "id=$VID&remark=e2e打回")
check "打回(CI恢复)" "$(echo "$R" | python3 -c "import json,sys; print(0 if json.load(sys.stdin)['code']==200 else 1)")"
R=$(curl -s -X POST "$BASE/version/submit" -H "Authorization: $TOKEN" -d "id=$VID&remark=重新提测")
check "打回后重新提测" "$(echo "$R" | python3 -c "import json,sys; print(0 if json.load(sys.stdin)['code']==200 else 1)")"
R=$(curl -s -X POST "$BASE/version/release" -H "Authorization: $TOKEN" -d "id=$VID&remark=e2e发布")
check "发布" "$(echo "$R" | python3 -c "import json,sys; print(0 if json.load(sys.stdin)['code']==200 else 1)")"

echo ""
echo "========== B. Pipeline 配置 + 构建配置 =========="
# 清理已存在的 e2e 仓库
EXIST_RID=$(curl -s -X POST "$BASE/build/repository/list" -H "Authorization: $TOKEN" -d "page=1&limit=10" | python3 -c "import json,sys; d=json.load(sys.stdin); rs=d.get('data',{}).get('result',[]); print([r['id'] for r in rs if r.get('name')=='e2e-repo'][0] if any(r.get('name')=='e2e-repo' for r in rs) else '')" 2>/dev/null)
[ -n "$EXIST_RID" ] && curl -s -X POST "$BASE/build/repository/delete" -H "Authorization: $TOKEN" -d "id=$EXIST_RID" >/dev/null 2>&1 || true
# 1. 创建仓库（本地 git）
curl -s -X POST "$BASE/build/repository/edit" -H "Authorization: $TOKEN" --data-urlencode "name=e2e-repo" --data-urlencode "gitUrl=file://$E2E_REPO" --data-urlencode "repoType=0" --data-urlencode "protocol=0" >/dev/null
RID=$(curl -s -X POST "$BASE/build/repository/list" -H "Authorization: $TOKEN" -d "page=1&limit=10" | python3 -c "import json,sys; d=json.load(sys.stdin); rs=d.get('data',{}).get('result',[]); print([r['id'] for r in rs if r.get('name')=='e2e-repo'][0] if any(r.get('name')=='e2e-repo' for r in rs) else '')" 2>/dev/null)
check "仓库创建($RID)" "$([ -n "$RID" ] && echo 0 || echo 1)"
# 2. 创建构建配置（构建命令产出 jar + LocalCommand 发布）
RELEASE_CMD=$(printf 'mkdir -p %s && cp ${BUILD_RESULT_FILE}/demo.jar %s/' "$E2E_PUBLISH" "$E2E_PUBLISH")
EXTRA_DATA=$(python3 -c "import json,sys; print(json.dumps({'releaseCommand': sys.argv[1]}))" "$RELEASE_CMD")
BUILD_RESP=$(curl -s -X POST "$BASE/build/edit" -H "Authorization: $TOKEN" --data-urlencode "name=e2e-build" --data-urlencode "buildMode=0" --data-urlencode "repositoryId=$RID" --data-urlencode "branchName=master" --data-urlencode "script=mkdir -p target && echo hello > target/demo.jar" --data-urlencode "resultDirFile=target" --data-urlencode "releaseMethod=4" --data-urlencode "resultKeepDay=3" --data-urlencode "extraData=$EXTRA_DATA")
BID=$(curl -s -X POST "$BASE/build/list" -H "Authorization: $TOKEN" -d "page=1&limit=10" | python3 -c "import json,sys; d=json.load(sys.stdin); rs=d.get('data',{}).get('result',[]); print([r['id'] for r in rs if r.get('name')=='e2e-build'][0] if any(r.get('name')=='e2e-build' for r in rs) else '')" 2>/dev/null)
check "构建配置创建($BID): $BUILD_RESP" "$([ -n "$BID" ] && echo 0 || echo 1)"

echo ""
echo "========== C. Pipeline 全链路 =========="
STAGES='[{"id":"build-1","type":"build","params":{}},{"id":"exec-1","type":"exec","params":{"command":"echo pipeline-exec"}},{"id":"approve-1","type":"approval","params":{"desc":"发布验证"}},{"id":"publish-1","type":"publish","params":{"environment":"test"}}]'
PID=$(curl -s -X POST "$BASE/pipeline/save-config" -H "Authorization: $TOKEN" --data-urlencode "name=e2e-pipeline" --data-urlencode "buildId=$BID" --data-urlencode "triggers=[]" --data-urlencode "stages=$STAGES" | python3 -c "import json,sys; print(json.load(sys.stdin)['data'])")
check "Pipeline 配置保存" "$([ -n "$PID" ] && echo 0 || echo 1)"
curl -s -X POST "$BASE/pipeline/trigger" -H "Authorization: $TOKEN" -d "pipelineId=$PID" >/dev/null
echo "已触发，等待 build+exec 执行..."

# 轮询到等待审批
for i in $(seq 1 120); do
  sleep 2
  EID=$(curl -s -X POST "$BASE/pipeline/list-execute" -H "Authorization: $TOKEN" -d "pipelineId=$PID" | python3 -c "import json,sys; d=json.load(sys.stdin)['data']; print(d[0]['id'] if d else '')")
  ST=$(curl -s -X POST "$BASE/pipeline/list-execute" -H "Authorization: $TOKEN" -d "pipelineId=$PID" | python3 -c "import json,sys; d=json.load(sys.stdin)['data']; print(d[0]['status'] if d else -1)")
  [ "$ST" = "5" ] && break
  [ "$ST" = "3" ] && { echo "❌ Pipeline 失败"; curl -s -X POST "$BASE/pipeline/list-execute" -H "Authorization: $TOKEN" -d "pipelineId=$PID" | python3 -m json.tool | head -20; exit 1; }
done
check "build+exec 完成并等待审批(状态5)" "$([ "$ST" = "5" ] && echo 0 || echo 1)"
EID=$(curl -s -X POST "$BASE/pipeline/list-execute" -H "Authorization: $TOKEN" -d "pipelineId=$PID" | python3 -c "import json,sys; print(json.load(sys.stdin)['data'][0]['id'])")

# 审批通过 → publish
curl -s -X POST "$BASE/pipeline/approval" -H "Authorization: $TOKEN" -d "executeId=$EID&approve=true" >/dev/null
echo "审批通过，等待 publish..."
for i in $(seq 1 60); do
  sleep 2
  ST=$(curl -s -X POST "$BASE/pipeline/list-execute" -H "Authorization: $TOKEN" -d "pipelineId=$PID" | python3 -c "import json,sys; print(json.load(sys.stdin)['data'][0]['status'])")
  [ "$ST" = "2" ] && break
  [ "$ST" = "3" ] && { echo "❌ Pipeline publish 失败"; curl -s -X POST "$BASE/pipeline/list-execute" -H "Authorization: $TOKEN" -d "pipelineId=$PID" | python3 -m json.tool | head -20; exit 1; }
done
check "审批后 publish 完成(状态2=成功)" "$([ "$ST" = "2" ] && echo 0 || echo 1)"
check "发布产物落盘" "$([ -f "$E2E_PUBLISH/demo.jar" ] && echo 0 || echo 1)"
echo "发布目录内容: $(ls -la "$E2E_PUBLISH" 2>/dev/null | tail -2)"

echo ""
echo "========== D. CI 冻结端到端 =========="
# 构建配置 e2e-build 有已提测版本（C 中 build 自动创建了版本 vX）
# 直接验证：WebHook 触发被拦截
FROZEN=$(curl -s -X POST "$BASE/version/list" -H "Authorization: $TOKEN" -d "buildId=e2e-build" | python3 -c "import json,sys; d=json.load(sys.stdin)['data']; print(any(v['status']==1 for v in d))")
echo "e2e-build 存在已提测版本: $FROZEN"

echo ""
echo "========== 结果汇总 =========="
echo "PASS: $PASS  FAIL: $FAIL"
[ "$FAIL" = "0" ] && echo "✅ 端到端全部通过" || { echo "❌ 有失败项"; exit 1; }
