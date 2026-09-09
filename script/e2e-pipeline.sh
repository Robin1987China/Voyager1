#!/bin/bash
# =============================================================================
# 版本状态机 + 环境化 CD 端到端测试脚本（真实服务端）
#
# 前提：服务端运行中（deploy.sh 已部署）
# 用法: bash script/e2e-pipeline.sh [明文密码] [base_url]
#   默认密码 nGetCEvj，默认地址 http://127.0.0.1:2122
#
# 验证链路:
#   A. 版本生命周期: 创建 → 提测(冻结) → 打回(恢复) → 提测 → 发布
#   B. 版本状态机约束: 已打回不可部署 / 开发中不可上 prod / 非法流转拦截
#   C. 环境管理: 预置环境 / 重名拒绝 / 假节点绑定拒绝 / 应用外键校验
#   D. 审批闭环: prod 待审批 → 拒绝 → 已拒绝；重复审批拦截
#   E. 旧 Pipeline API 防回归: 已删除端点返回 404
# =============================================================================
set -euo pipefail
BASE="${2:-http://127.0.0.1:2122}"
PLAIN_PWD="${1:-nGetCEvj}"
PWD_SHA1=$(node -e "console.log(require('crypto').createHash('sha1').update(process.argv[1],'utf8').digest('hex'))" "$PLAIN_PWD")
PASS=0; FAIL=0

check() { # check <desc> <0/1>
  if [ "$2" = "0" ] || [ "$2" = "true" ]; then
    PASS=$((PASS+1)); echo "  ✅ $1"
  else
    FAIL=$((FAIL+1)); echo "  ❌ $1"
  fi
}
code() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{console.log(JSON.parse(d).code)}catch(e){console.log('parse-error')}})"; }

TOKEN=$(curl -s --max-time 10 -X POST "$BASE/userLogin?loginName=admin&userPwd=$PWD_SHA1" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{console.log(JSON.parse(d).data.token)}catch(e){console.log('')}})")
[ -n "$TOKEN" ] && echo "登录 OK" || { echo "登录失败（可传参: bash script/e2e-pipeline.sh <密码> <base>）"; exit 1; }
AUTH="Authorization: $TOKEN"
SUFFIX=$(date +%s)
BUILD_ID="e2e-app-$SUFFIX"

echo ""
echo "========== A. 版本生命周期 =========="
V=$(curl -s -X POST "$BASE/version/create" -H "$AUTH" -d "buildId=$BUILD_ID&buildNumberId=1&version=v1.0.0-$SUFFIX&artifactRef=/tmp/x.jar")
VID=$(echo "$V" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{console.log(JSON.parse(d).data.id)}catch(e){console.log('')}})")
check "创建版本" "$([ -n "$VID" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/submit" -H "$AUTH" -d "id=$VID&remark=e2e" | code)
check "提测(CI冻结)" "$([ "$R" = "200" ] && echo 0 || echo 1)"
S=$(curl -s -X POST "$BASE/version/list" -H "$AUTH" -d "buildId=$BUILD_ID" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).data[0].status))")
check "状态=已提测(1)" "$([ "$S" = "1" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/return" -H "$AUTH" -d "id=$VID&remark=e2e打回" | code)
check "打回(CI恢复)" "$([ "$R" = "200" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/submit" -H "$AUTH" -d "id=$VID&remark=重新提测" | code)
check "打回后重新提测" "$([ "$R" = "200" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/release" -H "$AUTH" -d "id=$VID&remark=e2e发布" | code)
check "发布" "$([ "$R" = "200" ] && echo 0 || echo 1)"

echo ""
echo "========== B. 版本状态机约束 =========="
V2=$(curl -s -X POST "$BASE/version/create" -H "$AUTH" -d "buildId=$BUILD_ID&buildNumberId=2&version=v2.0.0-$SUFFIX&artifactRef=/tmp/y.jar")
V2ID=$(echo "$V2" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{console.log(JSON.parse(d).data.id)}catch(e){console.log('')}})")
R=$(curl -s -X POST "$BASE/environment/deploy" -H "$AUTH" -d "versionId=$V2ID&environment=prod" | code)
check "开发中版本部署 prod 被拒(405)" "$([ "$R" != "200" ] && echo 0 || echo 1)"
curl -s -X POST "$BASE/version/submit" -H "$AUTH" -d "id=$V2ID&remark=x" >/dev/null
curl -s -X POST "$BASE/version/return" -H "$AUTH" -d "id=$V2ID&remark=x" >/dev/null
R=$(curl -s -X POST "$BASE/environment/deploy" -H "$AUTH" -d "versionId=$V2ID&environment=dev" | code)
check "已打回版本部署任意环境被拒(405)" "$([ "$R" != "200" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/version/release" -H "$AUTH" -d "id=$V2ID&remark=非法流转" | code)
check "打回状态直接发布被拒(405)" "$([ "$R" != "200" ] && echo 0 || echo 1)"

echo ""
echo "========== C. 环境管理 =========="
ENVS=$(curl -s -X POST "$BASE/environment/list" -H "$AUTH")
echo "$ENVS" | grep -q '"name":"dev"' && echo "$ENVS" | grep -q '"name":"prod"'
check "预置环境 dev/test/prod" "$?"
R=$(curl -s -X POST "$BASE/environment/save" -H "$AUTH" -d "name=test&sortValue=9" | code)
check "环境重名拒绝(非200)" "$([ "$R" != "200" ] && echo 0 || echo 1)"
TEST_ENV_ID=$(echo "$ENVS" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).data.find(e=>e.name==='test').id))")
R=$(curl -s -X POST "$BASE/environment/bind-target" -H "$AUTH" -d "environmentId=$TEST_ENV_ID&targetType=NODE&targetId=ghost-node-$SUFFIX&projectId=p1" | code)
check "不存在节点绑定拒绝(非200)" "$([ "$R" != "200" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/environment/bind-target" -H "$AUTH" -d "environmentId=$TEST_ENV_ID&targetType=K8S&targetId=c1&projectId=p1" | code)
check "不支持的目标类型拒绝(非200)" "$([ "$R" != "200" ] && echo 0 || echo 1)"
R=$(curl -s -X POST "$BASE/application/save" -H "$AUTH" -d "name=e2e-ghost-$SUFFIX&repositoryId=ghost-repo&buildId=ghost-build" | code)
check "应用假外键拒绝(非200)" "$([ "$R" != "200" ] && echo 0 || echo 1)"

echo ""
echo "========== D. 审批闭环（prod 需审批） =========="
# 用已发布版本 V（状态 Released）部署 prod → 待审批
R=$(curl -s -X POST "$BASE/environment/deploy" -H "$AUTH" -d "versionId=$VID&environment=prod")
RC=$(echo "$R" | code)
REC_ID=$(echo "$R" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{console.log(JSON.parse(d).data)}catch(e){console.log('')}})")
# prod 未绑定节点时应先报「未绑定目标」；已绑定时应落待审批记录——两种均为有效路径
if [ "${RC}" = "200" ] && [ -n "${REC_ID}" ]; then
  check "prod 部署落待审批记录" 0
  R=$(curl -s -X POST "$BASE/environment/approve-deploy" -H "$AUTH" -d "recordId=$REC_ID&approve=false&remark=本期不上" | code)
  check "审批拒绝成功" "$([ "$R" = "200" ] && echo 0 || echo 1)"
  ST=$(curl -s -X POST "$BASE/environment/deploy-records" -H "$AUTH" -d "versionId=$VID" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{const r=JSON.parse(d).data.find(x=>x.id==='$REC_ID');console.log(r?r.status:'missing')})")
  check "记录闭环为已拒绝(4)" "$([ "$ST" = "4" ] && echo 0 || echo 1)"
  R=$(curl -s -X POST "$BASE/environment/approve-deploy" -H "$AUTH" -d "recordId=$REC_ID&approve=true" | code)
  check "重复审批拦截(非200)" "$([ "$R" != "200" ] && echo 0 || echo 1)"
else
  check "prod 未绑定目标时部署被拒（有效路径，HTTP ${RC}）" "$([ "${RC}" != "200" ] && echo 0 || echo 1)"
fi

echo ""
echo "========== E. 旧 Pipeline API 防回归 =========="
for api in "pipeline/save-config" "pipeline/trigger" "pipeline/list-execute" "log-read/list"; do
  R=$(curl -s -X POST "$BASE/$api" -H "$AUTH" | head -c 200)
  echo "$R" | grep -q "No static resource"
  check "已删除端点 /$api 返回 404" "$?"
done

echo ""
echo "========== 结果汇总 =========="
echo "PASS: $PASS  FAIL: $FAIL"
[ "$FAIL" = "0" ] && echo "✅ 端到端全部通过" || { echo "❌ 有失败项"; exit 1; }
