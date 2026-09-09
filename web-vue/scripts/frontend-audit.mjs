// Voyager1 前端静态审计脚本
// 用法：cd web-vue && npm run audit
// 检查：1) 模板调用未定义方法（运行时错误） 2) 裸 $t()（i18n） 3) 调用已删除后端接口 4) 菜单↔路由不一致
import { parse } from '@vue/compiler-dom'
import { readFileSync, readdirSync, statSync, existsSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const SRC = join(__dirname, '..', 'src')
const MENUS = join(__dirname, '..', '..', 'modules', 'server', 'src', 'main', 'resources', 'menus')

const issues = []
const add = (cat, path, line, msg) => issues.push({ cat, path, line, msg })

function walkFiles(dir, ext, out = []) {
  if (!existsSync(dir)) return out
  for (const f of readdirSync(dir)) {
    const p = join(dir, f)
    const s = statSync(p)
    if (s.isDirectory()) walkFiles(p, ext, out)
    else if (p.endsWith(ext)) out.push(p)
  }
  return out
}

// ---------- 1. 模板调用未定义方法 ----------
function parseDefinedMethods(script) {
  const names = new Set()
  for (const m of script.matchAll(/\bmethods\s*:\s*\{/g)) {
    const i = m.index + m[0].length - 1
    let depth = 0, j = i
    while (j < script.length) {
      if (script[j] === '{') depth++
      else if (script[j] === '}') { depth--; if (depth === 0) break }
      j++
    }
    const block = script.slice(i, j + 1)
    for (const mm of block.matchAll(/^\s*([A-Za-z_$][\w$]*)\s*\(/gm)) names.add(mm[1])
  }
  for (const mm of script.matchAll(/\b(?:const|let|var|function)\s+([A-Za-z_$][\w$]*)\s*[=(]/g)) names.add(mm[1])
  return names
}

function checkUndefinedMethods() {
  for (const fp of walkFiles(SRC, '.vue')) {
    const txt = readFileSync(fp, 'utf-8')
    const scripts = [...txt.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/g)].map((m) => m[1])
    const tmpls = [...txt.matchAll(/<template[^>]*>([\s\S]*?)<\/template>/g)].map((m) => m[1])
    if (!scripts.length) continue
    const defined = parseDefinedMethods(scripts[scripts.length - 1])
    const template = tmpls.join('\n')
    const calls = new Set()
    for (const m of template.matchAll(/@[\w:.-]+="\s*([A-Za-z_$][\w$]*)\s*\(/g)) calls.add(m[1])
    for (const m of template.matchAll(/@[\w:.-]+="\s*([A-Za-z_$][\w$]*)\s*"/g)) calls.add(m[1])
    const ignore = new Set(['click', 'change', 'input', 'blur', 'select', 'focus', 'keydown', 'keyup', 'check', 'event', 'reset'])
    for (const c of [...calls].sort()) {
      if (!ignore.has(c) && !defined.has(c)) add('未定义方法', fp, '', c)
    }
  }
}

// ---------- 2. 裸 $t()（用 Vue 编译器精确定位文本节点） ----------
function checkRawDollarT() {
  for (const fp of walkFiles(SRC, '.vue')) {
    const txt = readFileSync(fp, 'utf-8')
    const t = txt.indexOf('<template>')
    if (t < 0) continue
    const ends = ['<script', '<style'].map((k) => { const i = txt.indexOf(k, t + 9); return i < 0 ? txt.length : i })
    const end = Math.min(...ends)
    const tpl = txt.slice(t, end)
    const base = txt.slice(0, t).split('\n').length - 1
    let ast
    try { ast = parse(tpl, { comments: false, whitespace: 'preserve' }) } catch { continue }
    const walk = (n) => {
      if (n.type === 2 && typeof n.content === 'string' && n.content.includes('$t(')) {
        add('裸$t()', fp, (n.loc?.start?.line || 0) + base, n.content.trim().slice(0, 60))
      }
      if (Array.isArray(n.children)) n.children.forEach(walk)
    }
    ast.children.forEach(walk)
  }
}

// ---------- 3. 调用已删除后端接口 ----------
const DELETED_EP = ['/pipeline/', '/log-read/', '/file-manager/release-task', '/certificate/deploy']
function checkDeletedApi() {
  for (const fp of walkFiles(join(SRC, 'api'), '.ts')) {
    const lines = readFileSync(fp, 'utf-8').split('\n')
    lines.forEach((ln, i) => {
      for (const ep of DELETED_EP) if (ln.includes(ep)) add('已删后端引用', fp, i + 1, ln.trim().slice(0, 80))
    })
  }
}

// ---------- 4. 菜单 id ↔ 路由 ↔ router 一致性 ----------
function checkMenuRoute() {
  const routeMenu = {}
  const rmTxt = readFileSync(join(SRC, 'router', 'route-menu.ts'), 'utf-8')
  for (const m of rmTxt.matchAll(/^\s*([A-Za-z0-9_\-]+)\s*:\s*['"]([^'"]+)['"]/gm)) routeMenu[m[1]] = m[2]
  const routerTxt = readFileSync(join(SRC, 'router', 'index.ts'), 'utf-8')
  const routerPaths = new Set([...routerTxt.matchAll(/path:\s*['"]([^'"]+)['"]/g)].map((m) => m[1]))
  const menuIds = new Set()
  const walkMenu = (items) => items.forEach((it) => { menuIds.add(it.id); if (it.childs) walkMenu(it.childs) })
  for (const loc of readdirSync(MENUS)) {
    for (const fn of readdirSync(join(MENUS, loc))) {
      try { walkMenu(JSON.parse(readFileSync(join(MENUS, loc, fn), 'utf-8'))) } catch {}
    }
  }
  const groups = new Set(['overview', 'delivery', 'host', 'cloud', 'artifact', 'monitor', 'tools', 'assets-manager', 'user', 'setting', 'about', 'system-overview'])
  for (const mid of [...menuIds].sort()) {
    if (groups.has(mid)) continue
    if (!routeMenu[mid]) add('菜单无路由映射', 'menus', mid, '')
  }
  for (const [mid, path] of Object.entries(routeMenu)) {
    if (!routerPaths.has(path)) add('路由不存在', 'route-menu.ts', mid, path)
  }
}

checkUndefinedMethods()
checkRawDollarT()
checkDeletedApi()
checkMenuRoute()

// ---------- 输出 ----------
const byCat = {}
for (const i of issues) byCat[i.cat] = (byCat[i.cat] || 0) + 1
console.log(`=== 前端审计：共 ${issues.length} 条问题 ===`)
for (const [cat, n] of Object.entries(byCat)) console.log(`  ${cat}: ${n}`)
console.log()
for (const i of issues) {
  const loc = i.path + (i.line ? `:${i.line}` : '')
  console.log(`[${i.cat}] ${loc}`)
  if (i.msg) console.log(`      ${i.msg}`)
}
if (!issues.length) console.log('✅ 无问题')
process.exit(issues.length ? 1 : 0)
