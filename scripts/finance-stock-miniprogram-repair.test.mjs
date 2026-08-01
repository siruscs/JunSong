import assert from 'node:assert/strict'
import fs from 'node:fs'

const routes = fs.readFileSync('junsong-ui-v3/src/router/constantRoutes.ts', 'utf8')
const overview = fs.readFileSync('junsong-ui-v3/src/views/finance/overview/index.vue', 'utf8')

for (const route of [
  '/finance/report/sale',
  '/finance/report/profit',
  '/finance/report/expense',
  '/finance/report/profitShare',
  '/finance/report/stock',
  '/finance/report/store'
]) {
  assert.match(routes, new RegExp(`path: ['"]${route.replaceAll('/', '\\/')}['"]`), `missing route ${route}`)
}

assert.match(overview, /class="page-head"[\s\S]*?刷新/)
assert.doesNotMatch(overview, /<h2 class="page-title">财务管理概览<\/h2>/)

console.log('finance stock/miniprogram repair baseline checks passed')
