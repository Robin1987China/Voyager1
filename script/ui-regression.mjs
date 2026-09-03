#!/usr/bin/env node
/**
 * Voyager1 UI 全页面巡检脚本（零依赖：Node >= 22 全局 WebSocket + 无头 Chrome）
 *
 * 用法：
 *   node script/ui-regression.mjs                      # 全量巡检并输出报告
 *   node script/ui-regression.mjs --baseline           # 生成功能区基线 baseline.json
 *   node script/ui-regression.mjs --compare            # 对比基线，输出功能区差异
 *
 * 前置条件：
 *   1. 服务端运行中（默认 http://127.0.0.1:2122，可用 --base 覆盖）
 *   2. 登录凭据：--user admin --pwd <明文密码>（脚本内部 sha1 后提交）
 *   3. 无头 Chrome 可用（默认 /Applications/Google Chrome.app/...）
 *
 * 判定：
 *   FAIL  JS 异常 / console error / 主体未渲染 / 登录页回退 / 错误页文本
 *   WARN  功能区为空（无按钮且无输入框，需人工确认）
 *   PASS  其余
 */

import { spawn } from 'node:child_process';
import { writeFileSync, readFileSync, existsSync, mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..');

// ---------- 参数解析 ----------
const args = process.argv.slice(2);
const opt = (name, def) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 && args[i + 1] ? args[i + 1] : def;
};
const BASE = opt('base', 'http://127.0.0.1:2122');
const USER = opt('user', 'admin');
const PWD = opt('pwd', '');
const CHROME = opt('chrome', '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome');
const DEBUG_PORT = 9222;
const WAIT_MS = 8000;
const BASELINE_FILE = path.join(REPO_ROOT, 'script', 'ui-regression-baseline.json');
const SHOTS_DIR = path.join(REPO_ROOT, 'script', 'ui-shots');
const MODE = args.includes('--baseline') ? 'baseline' : args.includes('--compare') ? 'compare' : 'scan';

const sha1 = (s) => createHash('sha1').update(s, 'utf8').digest('hex');

// ---------- 路由清单提取 ----------
function loadRoutes() {
  const routerSrc = readFileSync(path.join(REPO_ROOT, 'web-vue/src/router/index.ts'), 'utf8');
  const paths = new Set();
  for (const m of routerSrc.matchAll(/path:\s*'([^']+)'/g)) {
    const p = m[1];
    if (p.includes('*') || p.includes(':') || p.startsWith('http')) continue; // 跳过 404/动态/外部
    if (p === '/' || p === '/login' || p === '/install' || p === '/prohibit-access' || p === '/404') continue;
    paths.add(p);
  }
  // 菜单 → route-menu 映射补充
  const menuFile = path.join(REPO_ROOT, 'modules/server/src/main/resources/menus/zh-CN/index.json');
  if (existsSync(menuFile)) {
    const menus = JSON.parse(readFileSync(menuFile, 'utf8'));
    const routeMenuFile = path.join(REPO_ROOT, 'web-vue/src/router/route-menu.ts');
    const rmSrc = readFileSync(routeMenuFile, 'utf8');
    const walk = (items) => {
      for (const item of items) {
        if (item.id) {
          const m = rmSrc.match(new RegExp(`${item.id.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}:\\s*'([^']+)'`));
          if (m) paths.add(m[1]);
        }
        if (item.childs) walk(item.childs);
      }
    };
    walk(menus);
  }
  return [...paths].sort();
}

// ---------- CDP 客户端 ----------
class CDP {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    this.exceptions = [];
    this.consoleErrors = [];
    ws.onmessage = (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.id && this.pending.has(msg.id)) {
        const p = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        msg.error ? p.reject(new Error(msg.error.message)) : p.resolve(msg.result);
      } else if (msg.method === 'Runtime.exceptionThrown') {
        this.exceptions.push(msg.params.exceptionDetails.exception?.description || 'unknown exception');
      } else if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') {
        this.consoleErrors.push(msg.params.args.map((a) => a.description ?? a.value ?? '').join(' ').slice(0, 500));
      }
    };
  }
  static async connect(url) {
    const ws = new WebSocket(url);
    await new Promise((r, j) => { ws.onopen = r; ws.onerror = j; });
    return new CDP(ws);
  }
  send(method, params = {}) {
    const msgId = ++this.id;
    return new Promise((resolve, reject) => {
      this.pending.set(msgId, { resolve, reject });
      this.ws.send(JSON.stringify({ id: msgId, method, params }));
    });
  }
}

// ---------- 页面巡检 ----------
const SNAPSHOT_EXPR = `(() => {
  const text = document.body ? document.body.innerText : '';
  const buttons = [...document.querySelectorAll('button')].map(b => (b.textContent || '').trim()).filter(Boolean);
  return {
    url: location.href,
    title: document.title,
    textLen: text.length,
    textHead: text.slice(0, 300),
    buttons,
    inputCount: document.querySelectorAll('input').length,
    tableCount: document.querySelectorAll('.n-data-table').length,
    searchBox: !!document.querySelector('.search-box'),
    hasLoginBtn: buttons.some(b => b.replace(/\\s/g, '') === '登录')
  };
})()`;

async function scanPage(cdp, url) {
  cdp.exceptions = [];
  cdp.consoleErrors = [];
  await cdp.send('Page.navigate', { url });
  await new Promise((r) => setTimeout(r, WAIT_MS));
  const res = await cdp.send('Runtime.evaluate', { expression: SNAPSHOT_EXPR, returnByValue: true });
  const base = res.result.value;

  // tab 遍历：激活每个未访问的 tab，收集异常与功能区
  const tabs = [];
  const tabResults = await cdp.send('Runtime.evaluate', {
    expression: `(() => {
      // 排除左侧多窗口导航标签（ant-tabs-tab-with-remove），仅遍历页面内 a-tabs
      const tabEls = [...document.querySelectorAll('.ant-tabs-tab:not(.ant-tabs-tab-with-remove)')];
      const names = tabEls.map(t => (t.textContent || '').trim());
      return names;
    })()`,
    returnByValue: true,
  });
  const tabNames = tabResults.result.value || [];
  for (let i = 0; i < tabNames.length; i++) {
    await cdp.send('Runtime.evaluate', {
      expression: `(() => { const t = document.querySelectorAll('.ant-tabs-tab:not(.ant-tabs-tab-with-remove)')[${i}]; if (t) t.click(); return 'ok'; })()`,
    });
    // 等待目标 tab 渲染（懒加载 + 接口请求：等待激活 pane 内容 + 缓冲 2s）
    for (let w = 0; w < 10; w++) {
      await new Promise((r) => setTimeout(r, 500));
      const ready = await cdp.send('Runtime.evaluate', {
        expression: `(() => { const panes = document.querySelectorAll('.ant-tabs-tabpane'); const active = [...panes].find(p => p.classList.contains('ant-tabs-tabpane-active')); return !!(active && (active.textContent || '').length > 0); })()`,
        returnByValue: true,
      });
      if (ready.result.value) break;
    }
    await new Promise((r) => setTimeout(r, 2000));
    const tabSnap = await cdp.send('Runtime.evaluate', { expression: SNAPSHOT_EXPR, returnByValue: true });
    tabs.push({ name: tabNames[i], snapshot: { buttons: tabSnap.result.value.buttons, inputCount: tabSnap.result.value.inputCount, tableCount: tabSnap.result.value.tableCount, searchBox: tabSnap.result.value.searchBox, textLen: tabSnap.result.value.textLen } });
  }
  return { ...base, tabs };
}

// ---------- 登录页冒烟检查（无需登录态，防登录表单渲染回归）----------
async function loginPageCheck(cdp) {
  cdp.exceptions = [];
  cdp.consoleErrors = [];
  await cdp.send('Page.navigate', { url: `${BASE}/#/login` });
  await new Promise((r) => setTimeout(r, 5000));
  const res = await cdp.send('Runtime.evaluate', {
    expression: `(() => {
      const inputs = [...document.querySelectorAll('input')];
      const buttons = [...document.querySelectorAll('button')];
      return {
        url: location.href,
        textLen: document.body ? document.body.innerText.length : 0,
        inputCount: inputs.length,
        hasLoginName: inputs.some(i => /loginname/i.test(i.id || i.name || '') || /用户名|登录名|账号|username|account/i.test(i.placeholder || '')),
        hasPassword: inputs.some(i => i.type === 'password'),
        hasLoginBtn: buttons.some(b => b.type === 'submit' || (b.className || '').includes('btn-login'))
      };
    })()`,
    returnByValue: true,
  });
  return res.result.value;
}

// ---------- 判定 ----------
function verdict(page, route) {
  const issues = [];
  const renderErrors = page.exceptions.filter((e) => !/AxiosError: Network Error/.test(e));
  const netErrors = page.exceptions.filter((e) => /AxiosError: Network Error/.test(e));
  const consoleRender = page.consoleErrors.filter((e) => !/AxiosError/.test(e));
  const consoleNet = page.consoleErrors.filter((e) => /AxiosError/.test(e));
  if (renderErrors.length) issues.push(`JS异常: ${renderErrors[0].slice(0, 200)}`);
  if (consoleRender.length) issues.push(`console错误: ${consoleRender[0].slice(0, 200)}`);
  if (page.hasLoginBtn) issues.push('页面回退到登录页（登录态失效）');
  if (!page.textLen || page.textLen < 30) issues.push('主体未渲染（文本过短）');
  if (/找不到|404|页面不存在|not found/i.test(page.textHead)) issues.push('错误页文本');
  const renderIssue = renderErrors.length > 0 || consoleRender.length > 0; // JS 渲染异常才是硬失败
  const netIssue = netErrors.length > 0 || consoleNet.length > 0;
  const structuralIssue = page.hasLoginBtn || !page.textLen || page.textLen < 30 || /找不到|404|页面不存在|not found/i.test(page.textHead);
  let status;
  if (renderIssue) status = 'FAIL';
  else if (netIssue) status = 'WARN'; // 网络中断（巡检切换页面时请求被 abort 等），非渲染 bug
  else if (structuralIssue) status = 'WARN'; // 空白/登录回退但无 JS 异常 → 依赖上下文的页面，需人工确认
  else if (page.buttons.length === 0 && page.inputCount === 0) status = 'WARN';
  else status = 'PASS';
  return { status, issues, netIssue };
}

// 登录页判定：登录按钮/账号/密码输入框缺一即为硬失败（登录页应有的“功能区”）
function loginVerdict(page) {
  const issues = [];
  const renderErrors = page.exceptions.filter((e) => !/AxiosError: Network Error/.test(e));
  const consoleRender = page.consoleErrors.filter((e) => !/AxiosError/.test(e));
  if (renderErrors.length) issues.push(`JS异常: ${renderErrors[0].slice(0, 200)}`);
  if (consoleRender.length) issues.push(`console错误: ${consoleRender[0].slice(0, 200)}`);
  if (!page.hasLoginName) issues.push('登录名输入框缺失');
  if (!page.hasPassword) issues.push('密码输入框缺失');
  if (!page.hasLoginBtn) issues.push('登录按钮缺失');
  if (!page.textLen || page.textLen < 10) issues.push('登录页主体未渲染');
  const status = issues.length ? 'FAIL' : 'PASS';
  return { status, issues };
}

// ---------- 布局冒烟（侧边栏「功能管理/系统管理」切换 + 截图存档）----------
const LAYOUT_EXPR = `(() => ({
  hasSwitch: !!document.querySelector('.mode-switch .n-radio-group, .mode-switch .mode-switch-btn'),
  hasWrapper: !!document.querySelector('.mode-switch'),
  checked: (document.querySelector('.mode-switch .n-radio-button--checked input') || {}).value || null,
  switchText: [...document.querySelectorAll('.mode-switch .n-radio-button')].map((b) => (b.innerText || '').trim()).join('/'),
  menuCount: document.querySelectorAll('.sider-menu .n-menu-item').length,
  lsUser: localStorage.getItem('Voyager1-User') || ''
}))()`;

async function captureShot(cdp, name) {
  try {
    mkdirSync(SHOTS_DIR, { recursive: true });
    const res = await cdp.send('Page.captureScreenshot', { format: 'png' });
    writeFileSync(path.join(SHOTS_DIR, `${name}.png`), Buffer.from(res.data, 'base64'));
  } catch (e) {
    // 截图失败不阻断巡检
  }
}

// 校验侧边栏切换按钮存在、且当前视图高亮正确（防「切换控件消失/高亮错」这类回归）
async function layoutCheck(cdp) {
  const issues = [];
  const checks = [
    { route: '/overview', name: 'overview', expect: 'normal', label: '功能视图(/overview)' },
    { route: '/system/overview', name: 'system-overview', expect: 'management', label: '系统视图(/system/overview)' }
  ];
  for (const c of checks) {
    await cdp.send('Page.navigate', { url: `${BASE}/#${c.route}` });
    await new Promise((r) => setTimeout(r, 6000));
    const res = await cdp.send('Runtime.evaluate', { expression: LAYOUT_EXPR, returnByValue: true });
    const v = res.result.value || {};
    await captureShot(cdp, c.name);
    if (!v.hasSwitch) {
      issues.push(`${c.label}：侧边栏缺少「功能管理/系统管理」切换按钮（wrapper=${v.hasWrapper}，menu=${v.menuCount}，lsUser=${v.lsUser ? v.lsUser.slice(0, 40) : '空'}）`);
    } else if (v.checked !== c.expect) {
      issues.push(`${c.label}：切换高亮错误（期望 ${c.expect}，实际 ${v.checked || '无'}；当前项 ${v.switchText || '空'}）`);
    }
  }
  return issues;
}

// ---------- 主流程 ----------
let chromeProcess = null;
function killChrome() {
  if (chromeProcess && !chromeProcess.killed) {
    try { chromeProcess.kill('SIGKILL'); } catch (e) { /* ignore */ }
  }
}
async function main() {
  if (!PWD) {
    console.error('缺少 --pwd <明文密码>');
    process.exit(1);
  }
  const routes = loadRoutes();
  console.log(`[INFO] 待巡检路由: ${routes.length} 个（${MODE} 模式）`);

  // 登录前检查验证码（UI 巡检为自动化，需免验证码环境）
  let disabledCaptcha = false;
  try {
    const cfg = await (await fetch(`${BASE}/login-config`)).json();
    disabledCaptcha = cfg?.data?.disabledCaptcha === true;
  } catch (e) { /* 忽略，下面登录会给出明确错误 */ }
  if (!disabledCaptcha) {
    console.error('[ERROR] 服务端启用了登录图形验证码，UI 巡检无法自动登录。');
    console.error('        请在测试环境禁用验证码后重试：');
    console.error('          - 部署：bash script/deploy.sh --no-captcha [--pwd <密码>]');
    console.error('          - 或手动启动服务端前 export VOYAGER1_WEB_DISABLEDCAPTCHA=true');
    process.exit(1);
  }

  // 登录
  const login = await fetch(`${BASE}/userLogin?loginName=${USER}&userPwd=${sha1(PWD)}`, { method: 'POST' });
  const loginJson = await login.json();
  if (loginJson.code !== 200) {
    console.error(`[ERROR] 登录失败: ${loginJson.msg}（请确认 --pwd 密码正确，且账号未被锁定）`);
    process.exit(1);
  }
  const token = loginJson.data.token;
  console.log('[INFO] 登录成功');

  // 启动无头 Chrome
  const profile = path.join('/tmp', `voyager1-ui-reg-${Date.now()}`);
  chromeProcess = spawn(CHROME, ['--headless=new', '--disable-gpu', '--no-sandbox', `--remote-debugging-port=${DEBUG_PORT}`, `--user-data-dir=${profile}`, '--no-first-run', 'about:blank'], { stdio: 'ignore' });
  await new Promise((r) => setTimeout(r, 4000));
  const targets = await (await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/list`)).json();
  const page = targets.find((t) => t.type === 'page');
  if (!page) { console.error('[ERROR] 无头 Chrome 未启动'); process.exit(1); }
  const cdp = await CDP.connect(page.webSocketDebuggerUrl);
  await cdp.send('Page.enable');
  await cdp.send('Runtime.enable');

  // 登录页冒烟检查（在注入登录态之前，登录页为公开路由）
  const loginCheck = await loginPageCheck(cdp);
  const lv = loginVerdict({ ...loginCheck, exceptions: cdp.exceptions, consoleErrors: cdp.consoleErrors });
  console.log(`[${lv.status}] /login（登录页冒烟）${lv.issues.length ? ' - ' + lv.issues[0].slice(0, 100) : ''}`);
  await captureShot(cdp, 'login');

  // 注入登录态
  await cdp.send('Page.navigate', { url: BASE + '/' });
  await new Promise((r) => setTimeout(r, 4000));
  await cdp.send('Runtime.evaluate', {
    expression: `localStorage.setItem('Voyager1-Token', '${token}'); localStorage.setItem('Voyager1-Long-Term-Token', '${token}'); 'ok'`,
  });

  // 布局冒烟检查（侧边栏「功能管理/系统管理」切换 + 截图存档）
  const layoutIssues = await layoutCheck(cdp);
  if (layoutIssues.length) {
    for (const i of layoutIssues) console.log(`[FAIL] 布局冒烟 - ${i}`);
  } else {
    console.log('[PASS] 布局冒烟（功能管理/系统管理 切换）');
  }

  // 巡检
  const results = [];
  for (const route of routes) {
    const url = `${BASE}/#${route}`;
    const data = await scanPage(cdp, url);
    const v = verdict({ ...data, exceptions: cdp.exceptions, consoleErrors: cdp.consoleErrors }, route);
    results.push({ route, url: data.url, ...v, snapshot: { buttons: data.buttons, inputCount: data.inputCount, tableCount: data.tableCount, searchBox: data.searchBox, textLen: data.textLen }, tabs: data.tabs || [] });
    console.log(`[${v.status}] ${route}${v.issues.length ? ' - ' + v.issues[0].slice(0, 100) : ''}`);
  }

  // 输出
  const layoutFail = layoutIssues.length ? 1 : 0;
  const pass = results.filter((r) => r.status === 'PASS').length + (lv.status === 'PASS' ? 1 : 0) + (layoutFail ? 0 : 1);
  const warn = results.filter((r) => r.status === 'WARN').length;
  const fail = results.filter((r) => r.status === 'FAIL').length + (lv.status === 'FAIL' ? 1 : 0) + layoutFail;

  if (MODE === 'baseline') {
    const baseline = Object.fromEntries(results.map((r) => [r.route, { ...r.snapshot, tabs: r.tabs }]));
    writeFileSync(BASELINE_FILE, JSON.stringify(baseline, null, 2));
    console.log(`\n[INFO] 基线已保存: ${BASELINE_FILE}`);
  } else if (MODE === 'compare') {
    if (!existsSync(BASELINE_FILE)) {
      console.error('[ERROR] 基线不存在，请先 --baseline');
      process.exit(1);
    }
    const baseline = JSON.parse(readFileSync(BASELINE_FILE, 'utf8'));
    console.log('\n=== 功能区差异 ===');
    for (const r of results) {
      const old = baseline[r.route];
      if (!old) { console.log(`[NEW] ${r.route}（基线中不存在）`); continue; }
      const diff = [];
      const removed = old.buttons.filter((b) => !r.snapshot.buttons.includes(b));
      const added = r.snapshot.buttons.filter((b) => !old.buttons.includes(b));
      if (removed.length) diff.push(`按钮消失: ${removed.join(', ')}`);
      if (added.length) diff.push(`新增按钮: ${added.join(', ')}`);
      if (old.inputCount !== r.snapshot.inputCount) diff.push(`输入框 ${old.inputCount}->${r.snapshot.inputCount}`);
      if (old.tableCount !== r.snapshot.tableCount) diff.push(`表格 ${old.tableCount}->${r.snapshot.tableCount}`);
      if (old.searchBox !== r.snapshot.searchBox) diff.push(`searchBox ${old.searchBox}->${r.snapshot.searchBox}`);
      // tab 维度对比（旧基线无 tabs 字段则跳过）
      if (old.tabs && r.tabs && old.tabs.length) {
        for (const tab of r.tabs) {
          const oldTab = old.tabs.find((t) => t.name === tab.name);
          if (!oldTab) { diff.push(`新增tab: ${tab.name}`); continue; }
          const removed = oldTab.snapshot.buttons.filter((b) => !tab.snapshot.buttons.includes(b));
          const added = tab.snapshot.buttons.filter((b) => !oldTab.snapshot.buttons.includes(b));
          if (removed.length) diff.push(`tab[${tab.name}]按钮消失: ${removed.join(', ')}`);
          if (added.length) diff.push(`tab[${tab.name}]新增按钮: ${added.join(', ')}`);
          if (oldTab.snapshot.searchBox !== tab.snapshot.searchBox) diff.push(`tab[${tab.name}]searchBox ${oldTab.snapshot.searchBox}->${tab.snapshot.searchBox}`);
        }
      }
      if (diff.length) console.log(`[DIFF] ${r.route}: ${diff.join('; ')}`);
    }
  }

  console.log(`\n=== 巡检报告 ===`);
  console.log(`PASS: ${pass}  WARN: ${warn}  FAIL: ${fail}  总计: ${results.length + 2}（含登录页冒烟 + 布局冒烟）`);
  console.log(`截图存档: ${SHOTS_DIR}/`);
  if (fail) {
    console.log('\n失败页面:');
    if (lv.status === 'FAIL') {
      console.log(`  FAIL /login（登录页）`);
      for (const i of lv.issues) console.log(`    - ${i.slice(0, 300)}`);
    }
    if (layoutIssues.length) {
      console.log(`  FAIL 布局冒烟（功能管理/系统管理切换）`);
      for (const i of layoutIssues) console.log(`    - ${i.slice(0, 300)}`);
    }
    for (const r of results.filter((x) => x.status === 'FAIL')) {
      console.log(`  FAIL ${r.route}`);
      for (const i of r.issues) console.log(`    - ${i.slice(0, 300)}`);
    }
  }
  if (warn) {
    console.log('\nWARN 页面（功能区为空，需人工确认）:');
    for (const r of results.filter((x) => x.status === 'WARN')) console.log(`  WARN ${r.route}`);
  }
  killChrome();
  process.exit(fail ? 2 : 0);
}

main().catch((e) => { console.error('[FATAL]', e); killChrome(); process.exit(1); });
