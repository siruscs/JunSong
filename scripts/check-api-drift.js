#!/usr/bin/env node

/**
 * 漂移检查脚本 — 比较 catalog.ts 与 sdk/openapi.json 的路径一致性
 *
 * 用法：从仓库根目录执行
 *   node scripts/check-api-drift.js
 *
 * 退出码：
 *   0 — 路径完全一致
 *   1 — 存在不一致
 *   2 — 文件读取或解析错误
 */

const fs = require('fs')
const path = require('path')

const REPO_ROOT = path.resolve(__dirname, '..')
const CATALOG_PATH = path.join(REPO_ROOT, 'junsong-ui-v3/src/views/open/data/catalog.ts')
const OPENAPI_PATH = path.join(REPO_ROOT, 'sdk/openapi.json')

// ── 解析 catalog.ts ──────────────────────────────────────────

function parseCatalog() {
  const src = fs.readFileSync(CATALOG_PATH, 'utf-8')
  const entries = []

  // 匹配每个 openApiCatalog 条目中的 method + path 对
  // 策略：找到 openApiCatalog 数组内容，然后逐块解析 method 和 path
  const catalogMatch = src.match(/export const openApiCatalog[\s\S]*?=\s*\[([\s\S]*?)\n\]/)
  if (!catalogMatch) {
    console.error('错误：无法找到 openApiCatalog 数组定义')
    process.exit(2)
  }

  const catalogBody = catalogMatch[1]
  // 按对象分隔：每个 { ... } 是一个条目
  const blocks = catalogBody.split(/\n\s*\{/)
  for (const block of blocks) {
    const methodMatch = block.match(/method:\s*'([A-Z]+)'/)
    const pathMatch = block.match(/path:\s*'([^']+)'/)
    if (methodMatch && pathMatch) {
      entries.push({
        method: methodMatch[1].toUpperCase(),
        path: pathMatch[1],
      })
    }
  }

  return entries
}

// ── 解析 openapi.json ────────────────────────────────────────

function parseOpenApi() {
  const raw = fs.readFileSync(OPENAPI_PATH, 'utf-8')
  const spec = JSON.parse(raw)
  const entries = []

  if (!spec.paths) {
    console.error('错误：openapi.json 中没有 paths 字段')
    process.exit(2)
  }

  for (const [urlPath, methods] of Object.entries(spec.paths)) {
    for (const method of Object.keys(methods)) {
      const upper = method.toUpperCase()
      if (['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].includes(upper)) {
        entries.push({ method: upper, path: urlPath })
      }
    }
  }

  return entries
}

// ── 规范化路径（去掉尾部斜杠，统一参数占位符） ────────────────────

function normalize(p) {
  return p.replace(/\/$/, '') || '/'
}

// ── 生成唯一键 ──────────────────────────────────────────────

function key(entry) {
  return `${entry.method} ${normalize(entry.path)}`
}

// ── 主流程 ──────────────────────────────────────────────────

function main() {
  console.log('=== 开放平台 API 漂移检查 ===\n')

  let catalogEntries, openApiEntries
  try {
    catalogEntries = parseCatalog()
    console.log(`catalog.ts：找到 ${catalogEntries.length} 个端点`)
  } catch (err) {
    console.error(`读取 catalog.ts 失败：${err.message}`)
    process.exit(2)
  }

  try {
    openApiEntries = parseOpenApi()
    console.log(`openapi.json：找到 ${openApiEntries.length} 个端点`)
  } catch (err) {
    console.error(`读取 openapi.json 失败：${err.message}`)
    process.exit(2)
  }

  const catalogKeys = new Set(catalogEntries.map(key))
  const openApiKeys = new Set(openApiEntries.map(key))

  const onlyInCatalog = [...catalogKeys].filter(k => !openApiKeys.has(k)).sort()
  const onlyInOpenApi = [...openApiKeys].filter(k => !catalogKeys.has(k)).sort()

  let hasError = false

  if (onlyInCatalog.length > 0) {
    hasError = true
    console.log('\n❌ 仅在 catalog.ts 中存在（openapi.json 缺失）：')
    for (const k of onlyInCatalog) {
      console.log(`   - ${k}`)
    }
  }

  if (onlyInOpenApi.length > 0) {
    hasError = true
    console.log('\n❌ 仅在 openapi.json 中存在（catalog.ts 缺失）：')
    for (const k of onlyInOpenApi) {
      console.log(`   - ${k}`)
    }
  }

  if (hasError) {
    console.log('\n⚠️  路径不一致，请同步 catalog.ts 和 openapi.json')
    process.exit(1)
  } else {
    console.log('\n✅ catalog.ts 与 openapi.json 路径完全一致')
    process.exit(0)
  }
}

main()
