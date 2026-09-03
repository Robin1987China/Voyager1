#!/usr/bin/env node
/**
 * Voyager1 原创性扫描脚本（Phase 0 基线 + 各阶段验收红线）
 *
 * 用途：
 *   1. 扫描历史品牌关键字（jpom/luban/pegasus/keepbx/dromara）——目标恒为 0
 *   2. 扫描「承继标识」：从上游改名而来的基础类名、存储框架类名、上游表名
 *   3. 输出承继/自研清单（markdown）
 *
 * 用法：node script/originality-scan.mjs [--json]
 */

import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..');
const JSON_OUT = process.argv.includes('--json');

// ---------- 承继标识（已知从上游改名/承继而来，重写时需逐一消灭）----------
// 基础类（原 cn.keepbx.jpom.common 改名）
const VENDORED_CLASSES = [
  'RemoteVersion', 'BaseJsonModel', 'BaseIdModel', 'IPlugin', 'PluginConfig',
  'ICron', 'IAsyncLoad', 'ICacheTask', 'ISystemTask', 'ILogRecorder'
];
// 存储框架（原 JPom 招牌式自定义 DB 层）
const STORAGE_FRAMEWORK = [
  'BaseDbService', 'BaseDbCommonService', 'DialectUtil', 'StorageTableFactory',
  'StorageServiceFactory', 'TableName', 'PageResult', 'Order', 'IStorageService',
  'DbExtConfig', 'dataBeanToEntity', 'entityToBean', 'BaseDbModel'
];
// 上游核心表名（JPom 继承，需在数据模型重构中改名/重设计）
const VENDORED_TABLES = new Set([
  'USER_INFO', 'USER_LOGIN_LOG', 'USER_PERMISSION_GROUP', 'USEROPERATELOGV1',
  'USER_BIND_WORKSPACE', 'WORKSPACE', 'SYSTEM_PARAMETERS',
  'MACHINE_NODE_INFO', 'MACHINE_NODE_STAT_LOG', 'MACHINE_SSH_INFO', 'MACHINE_DOCKER_INFO',
  'CLUSTER_INFO', 'NODE_INFO', 'PROJECT_INFO', 'REPOSITORY',
  'BUILD_INFO', 'BUILDHISTORYLOG',
  'OUT_GIVING', 'OUTGIVINGLOG', 'LOG_READ',
  'MONITOR_INFO', 'MONITOR_USER_OPT', 'MONITORNOTIFYLOG',
  'SSH_INFO', 'SSHTERMINALEXECUTELOG', 'COMMAND_INFO', 'COMMAND_EXEC_LOG',
  'SCRIPT_INFO', 'SERVER_SCRIPT_INFO', 'SCRIPT_EXECUTE_LOG', 'SERVER_SCRIPT_EXECUTE_LOG',
  'SCRIPT_LIBRARY',
  'DOCKER_INFO', 'DOCKER_SWARM_INFO',
  'CERTIFICATE_INFO', 'FILE_STORAGE', 'STATIC_FILE_STORAGE',
  'FILE_RELEASE_TASK_LOG', 'FILE_RELEASE_TASK_TEMPLATE'
]);
// 品牌关键字（目标恒为 0）
const BRAND_KEYWORDS = ['jpom', 'keepbx', 'dromara', 'luban', 'pegasus'];

// ---------- 工具 ----------
function walk(dir, exts, out = []) {
  if (!existsSync(dir)) return out;
  for (const name of readdirSync(dir)) {
    const p = path.join(dir, name);
    let st;
    try { st = statSync(p); } catch { continue; }
    if (st.isDirectory()) {
      const base = path.basename(p);
      if (['node_modules', 'target', 'dist', '.git', 'logs'].includes(base)) continue;
      walk(p, exts, out);
    } else if (exts.includes(path.extname(p))) {
      out.push(p);
    }
  }
  return out;
}

function readAll(dir, exts) {
  const files = walk(dir, exts);
  const hits = [];
  for (const f of files) {
    const rel = path.relative(REPO_ROOT, f);
    const content = readFileSync(f, 'utf8');
    hits.push({ file: rel, content });
  }
  return hits;
}

// 从 sql-view CSV 提取表名
function extractTables() {
  const csvDir = path.join(REPO_ROOT, 'modules/server/src/main/resources/sql-view');
  const tables = new Set();
  if (!existsSync(csvDir)) return tables;
  for (const f of readdirSync(csvDir)) {
    if (!f.endsWith('.csv')) continue;
    const lines = readFileSync(path.join(csvDir, f), 'utf8').split('\n');
    for (const line of lines.slice(1)) {
      const parts = line.split(',');
      const first = (parts[0] || '').trim();
      // 跳过 alter/index 的指令行
      if (!first || first === 'alterType' || first === 'tableName' || first.startsWith('ADD') || first.startsWith('DROP')) continue;
      if (/^[A-Z][A-Z0-9_]*$/.test(first)) tables.add(first);
    }
  }
  return tables;
}

// ---------- 扫描 ----------
const result = {
  brands: [],
  vendoredClasses: [],
  storageFramework: [],
  tables: { derived: [], original: [] }
};

// 读取根目录下的直接文件（不递归，避免扫到 openspec/、扫描产物自身）
function readRootFiles(exts) {
  const out = [];
  for (const name of readdirSync(REPO_ROOT)) {
    const p = path.join(REPO_ROOT, name);
    let st;
    try { st = statSync(p); } catch { continue; }
    if (st.isFile() && exts.includes(path.extname(p))) {
      out.push({ file: name, content: readFileSync(p, 'utf8') });
    }
  }
  return out;
}

// 1) 品牌关键字 + 承继标识（源码 + 配置 + 脚本 + docs + 根目录直接文件）
const scanFiles = [
  ...readAll(path.join(REPO_ROOT, 'modules'), ['.java', '.xml']),
  ...readAll(path.join(REPO_ROOT, 'web-vue/src'), ['.ts', '.vue']),
  ...readAll(path.join(REPO_ROOT, 'script'), ['.mjs', '.sh']),
  ...readAll(path.join(REPO_ROOT, 'docs'), ['.md']),
  ...readRootFiles(['.md', '.yml', '.yaml', '.json', '.xml'])
];
for (const { file, content } of scanFiles) {
  const lower = content.toLowerCase();
  // 扫描脚本自身包含「禁用词清单」，排除自身避免误报
  const isScanScript = file === 'script/originality-scan.mjs';
  // 新 core 命名空间（io.voyager1.core.*）是重写目标，不计入「承继」统计（但仍做品牌关键字扫描）
  const isNewCore = file.includes('/io/voyager1/core/');
  // 迁移脚本（db/migration 及一次性 migrate-*.mjs）引用旧名以「清除旧名」，属过渡产物，不计入品牌与承继统计
  const isMigration = file.includes('/db/migration/') || /\/migrate-[^/]+\.mjs$/.test(file);
  // 测试（src/test）可能引用旧名以验证迁移，不计入品牌残留（但承继类引用仍纳入统计）
  const isTest = file.includes('/src/test/');
  for (const kw of BRAND_KEYWORDS) {
    if (isScanScript || isMigration || isTest) continue;
    if (lower.includes(kw)) result.brands.push({ file, kw });
  }
  if (!isScanScript && !isNewCore && !isMigration) {
    for (const cls of VENDORED_CLASSES) {
      if (new RegExp(`\\b${cls}\\b`).test(content)) result.vendoredClasses.push({ file, cls });
    }
    for (const cls of STORAGE_FRAMEWORK) {
      if (new RegExp(`\\b${cls}\\b`).test(content)) result.storageFramework.push({ file, cls });
    }
  }
}

// 2) 表名分类
const tables = extractTables();
for (const t of [...tables].sort()) {
  (VENDORED_TABLES.has(t) ? result.tables.derived : result.tables.original).push(t);
}

// ---------- 输出 ----------
if (JSON_OUT) {
  console.log(JSON.stringify({
    brandHits: result.brands.length,
    vendoredClassFiles: result.vendoredClasses.length,
    storageFrameworkFiles: result.storageFramework.length,
    derivedTables: result.tables.derived.length,
    originalTables: result.tables.original.length
  }, null, 2));
  process.exit(0);
}

const uniq = (arr) => [...new Set(arr)];
console.log('# Voyager1 原创性扫描报告\n');
console.log(`- 品牌关键字命中: **${result.brands.length}** 处（目标 0）`);
console.log(`- 承继基础类命中: **${result.vendoredClasses.length}** 处（重写目标）`);
console.log(`- 承继存储框架命中: **${result.storageFramework.length}** 处（重写目标）`);
console.log(`- 承继表: **${result.tables.derived.length}** 张 / 自研表: **${result.tables.original.length}** 张\n`);

if (result.brands.length) {
  console.log('## 品牌关键字残留（必须为 0）\n');
  for (const b of uniq(result.brands.map((x) => `${x.file} :: ${x.kw}`))) console.log(`- ${b}`);
}

console.log('## 承继基础类（涉及文件数）\n');
const vc = {};
for (const { cls } of result.vendoredClasses) vc[cls] = (vc[cls] || 0) + 1;
for (const [k, v] of Object.entries(vc).sort((a, b) => b[1] - a[1])) console.log(`- ${k}: ${v} 处`);

console.log('\n## 承继存储框架（涉及文件数）\n');
const sf = {};
for (const { cls } of result.storageFramework) sf[cls] = (sf[cls] || 0) + 1;
for (const [k, v] of Object.entries(sf).sort((a, b) => b[1] - a[1])) console.log(`- ${k}: ${v} 处`);

console.log('\n## 承继表（需重构改名）\n');
console.log(result.tables.derived.join(', '));

console.log('\n## 自研表（保留）\n');
console.log(result.tables.original.join(', '));
