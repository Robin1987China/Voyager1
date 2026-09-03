#!/usr/bin/env node
/**
 * Phase 2 批量表重命名迁移执行器
 *
 * 依据 script/table-rename-map.json 的 old→new 映射，完成三步联动：
 *   1. 更新 sql-view/table.all.v1.0.csv 首列表名（InitDb 在全新库直接建新名）；
 *   2. 更新各 Model 的 @TableName(value = "旧名") → 新名；
 *   3. 生成 db/migration/V3__rename_core_tables.java（条件重命名，保留存量数据）。
 *
 * 用法：node script/rename-tables.mjs [--dry-run]
 */
import { readFileSync, writeFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const DRY = process.argv.includes('--dry-run');

const MAP = JSON.parse(readFileSync(path.join(__dirname, 'table-rename-map.json'), 'utf8')).tables;
const entries = Object.entries(MAP).sort((a, b) => a[0].localeCompare(b[0]));
if (entries.length === 0) throw new Error('映射为空');

const CSV = path.join(ROOT, 'modules/server/src/main/resources/sql-view/table.all.v1.0.csv');
const MODEL_ROOT = path.join(ROOT, 'modules/server/src/main/java/io/voyager1');

// ---------- 工具 ----------
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

// 1) 找每个旧表名对应的 model 文件（含 @TableName(value = "旧名"）
function findModel(oldName) {
  for (const f of walk(MODEL_ROOT)) {
    const content = readFileSync(f, 'utf8');
    if (content.includes(`@TableName(value = "${oldName}"`)) return f;
  }
  return null;
}

// 2) 更新 CSV
let csv = readFileSync(CSV, 'utf8');
const csvChanges = [];
for (const [old, next] of entries) {
  const re = new RegExp(`^${old},`, 'gm');
  const before = csv;
  csv = csv.replace(re, `${next},`);
  if (csv !== before) csvChanges.push(`${old} → ${next}`);
}
if (!DRY) writeFileSync(CSV, csv);

// 3) 更新 model @TableName
const modelChanges = [];
const missing = [];
for (const [old, next] of entries) {
  const f = findModel(old);
  if (!f) { missing.push(old); continue; }
  let content = readFileSync(f, 'utf8');
  const re = new RegExp(`@TableName\\(value = "${old}"`, 'g');
  if (!re.test(content)) { missing.push(`${old} (未匹配注解)`); continue; }
  content = content.replace(new RegExp(`value = "${old}"`, 'g'), `value = "${next}"`);
  if (!DRY) writeFileSync(f, content);
  modelChanges.push(`${old} → ${next}  [${path.relative(ROOT, f)}]`);
}

// 4) 生成 V3 迁移
const v3Pairs = entries
  .map(([old, next]) => `        renameIfOldExists(conn, "${old}", "${next}");`)
  .join('\n');
const v3 = `package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * V3：承继表批量重命名为原创新名（SYS_/INFRA_/CI_/OPS_/STORAGE_ 前缀）
 * <p>
 * 由 script/rename-tables.mjs 依据 table-rename-map.json 自动生成。
 * 条件重命名：旧表存在 → 删除 InitDb 刚建的空白新表（如有）→ 旧表重命名（保留数据）；
 * 旧表不存在（全新库已按新名建表）→ 跳过。
 */
public class V3__rename_core_tables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
${v3Pairs}
    }

    private void renameIfOldExists(Connection conn, String oldName, String newName) throws Exception {
        if (!tableExists(conn, oldName)) {
            return; // 全新库：InitDb 已按新名建表
        }
        try (Statement st = conn.createStatement()) {
            if (tableExists(conn, newName)) {
                st.execute("DROP TABLE " + newName);
            }
            st.execute("ALTER TABLE " + oldName + " RENAME TO " + newName);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
`;
const v3Path = path.join(ROOT, 'modules/server/src/main/java/db/migration/V3__rename_core_tables.java');
if (!DRY) writeFileSync(v3Path, v3);

// ---------- 报告 ----------
console.log(`模式: ${DRY ? 'DRY-RUN（未写盘）' : '已写盘'}`);
console.log(`表总数: ${entries.length}`);
console.log(`\nCSV 变更: ${csvChanges.length}`);
console.log(`Model 变更: ${modelChanges.length}`);
for (const m of modelChanges) console.log(`  - ${m}`);
if (missing.length) {
  console.log(`\n⚠️ 未找到/未匹配的旧表名: ${missing.join(', ')}`);
} else {
  console.log(`\n✅ 全部 ${entries.length} 张表已联动更新`);
}
console.log(`\n生成迁移: ${path.relative(ROOT, v3Path)}`);
