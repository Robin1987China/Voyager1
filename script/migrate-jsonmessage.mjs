#!/usr/bin/env node
/**
 * 一次性迁移：JsonMessage / IJsonMessage / BaseJsonMessage → io.voyager1.core.api.ApiResult
 *
 * 用法：node script/migrate-jsonmessage.mjs [--dry-run]
 * 说明：先替换 import，再替换裸类名；对使用 ApiResult 却无 import 的文件自动补 import。
 */
import { readFileSync, writeFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const DRY = process.argv.includes('--dry-run');

const MODULES = ['modules/common', 'modules/server', 'modules/agent', 'modules/agent-transport', 'modules/sub-plugin', 'modules/storage-module'];

function walk(dir, out = []) {
  if (!existsSync(dir)) return out;
  for (const name of readdirSync(dir)) {
    const p = path.join(dir, name);
    let st;
    try { st = statSync(p); } catch { continue; }
    if (st.isDirectory()) walk(p, out);
    else if (name.endsWith('.java')) out.push(p);
  }
  return out;
}

const EXCLUDE = [
  '/io/voyager1/core/api/ApiResult.java',
  'ApiResultGoldenTest.java',
];

const files = MODULES.flatMap((m) => walk(path.join(ROOT, m)));
let changed = 0;
const changedFiles = [];

for (const f of files) {
  if (EXCLUDE.some((e) => f.includes(e))) continue;
  const original = readFileSync(f, 'utf8');
  let content = original;

  // 1) import 全限定名
  content = content.replace(/import io\.voyager1\.model\.JsonMessage;/g, 'import io.voyager1.core.api.ApiResult;');
  content = content.replace(/import io\.voyager1\.IJsonMessage;/g, 'import io.voyager1.core.api.ApiResult;');
  content = content.replace(/import io\.voyager1\.BaseJsonMessage;/g, 'import io.voyager1.core.api.ApiResult;');

  // 2) 裸类名（单词边界）
  content = content.replace(/\bJsonMessage\b/g, 'ApiResult');
  content = content.replace(/\bIJsonMessage\b/g, 'ApiResult');
  content = content.replace(/\bBaseJsonMessage\b/g, 'ApiResult');

  // 3) 若用到 ApiResult 但无 import，则补 import
  if (/\bApiResult\b/.test(content) && !content.includes('import io.voyager1.core.api.ApiResult;')) {
    const pkgMatch = content.match(/^(package [^;]+;\s*\n)/);
    const insert = 'import io.voyager1.core.api.ApiResult;\n';
    if (pkgMatch) {
      content = content.replace(pkgMatch[1], pkgMatch[1] + '\n' + insert);
    } else {
      content = insert + '\n' + content;
    }
  }

  if (content !== original) {
    changed++;
    changedFiles.push(path.relative(ROOT, f));
    if (!DRY) writeFileSync(f, content);
  }
}

console.log(`模式: ${DRY ? 'DRY-RUN' : '已写盘'}`);
console.log(`变更文件数: ${changed}`);
for (const f of changedFiles) console.log('  ' + f);
