import assert from 'node:assert/strict'
import fs from 'node:fs'

const routes = fs.readFileSync('junsong-ui-v3/src/router/constantRoutes.ts', 'utf8')
const overview = fs.readFileSync('junsong-ui-v3/src/views/finance/overview/index.vue', 'utf8')
const stockReport = fs.readFileSync('junsong-ui-v3/src/views/finance/report/stock.vue', 'utf8')
const stockInitApi = fs.readFileSync('junsong-ui-v3/src/api/finance/stockInit.ts', 'utf8')
const miniModules = fs.readFileSync('junsong-miniprogram/src/config/modules.js', 'utf8')
const miniStockPage = fs.readFileSync('junsong-miniprogram/src/pages/stock/index.vue', 'utf8')
const miniPages = fs.readFileSync('junsong-miniprogram/src/pages.json', 'utf8')
const miniDetail = fs.readFileSync('junsong-miniprogram/src/pages/detail/index.vue', 'utf8')
const mpPerm = fs.readFileSync('junsong-ui-v3/src/views/member/mpPerm/index.vue', 'utf8')
const stockSql = fs.readFileSync('sql/finance_stock_init.sql', 'utf8')
const stockInitService = fs.readFileSync('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinStockInitServiceImpl.java', 'utf8')
const stockInitPage = fs.readFileSync('junsong-ui-v3/src/views/finance/stockInit/index.vue', 'utf8')
const stockInitDetail = fs.readFileSync('junsong-ui-v3/src/views/finance/stockInit/detail.vue', 'utf8')
const workbench = fs.readFileSync('junsong-miniprogram/src/pages/workbench/index.vue', 'utf8')

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
assert.match(overview, /<h2 class="page-title">财务管理概览<\/h2>/)
assert.match(routes, /path: ['"]\/finance\/stockInit['"]/)
assert.doesNotMatch(routes, /path: ['"]\/finance\/stockInit\/index['"]/)
assert.match(stockReport, /@click="openCostAdjust\(scope\.row\)"/)
assert.match(stockReport, /createCostAdjustment\(/)
assert.match(stockInitApi, /url: ['"]\/finance\/stockInit['"]/)

assert.match(miniModules, /stockCost:\s*\{[\s\S]*?title: '库存与成本'/)
assert.match(miniModules, /customPage: '\/pages\/stock\/index'/)
assert.match(miniStockPage, /requireModulePermission\('stockCost'\)/)
assert.match(miniStockPage, /getStockValueReport/)
assert.match(miniPages, /"path": "pages\/stock\/index"/)
assert.doesNotMatch(miniStockPage, /成本调整|盘点过账|过账/)
assert.match(miniModules, /paymentEdit: 'finance:sale:payment'/)
assert.match(miniDetail, /openPaymentEdit\(payment\)/)
assert.match(miniDetail, /method: this\.editingPaymentId \? 'PUT' : 'POST'/)
assert.match(mpPerm, /key: "stockCost", name: "库存与成本"/)
assert.match(stockSql, /icon = 'fa fa-archive'/)
assert.match(stockReport, /prop="adjustmentAmount" label="成本调整"/)
assert.match(mpPerm, /key: "stockAdjustment", name: "库存调整"/)
assert.match(miniModules, /stockAdjustment:\s*\{/)
assert.match(stockInitApi, /adjustmentType/)
assert.match(stockInitApi, /adjustmentDate/)
assert.match(stockInitPage, /库存调整/)
assert.match(stockInitPage, /调整类型/)
assert.match(stockInitPage, /调整日历/)
assert.match(stockInitDetail, /label="调整数量"/)
assert.match(stockInitPage, /el-drawer v-model="detailOpen" title="库存调整明细"/)
assert.match(stockInitPage, /getStockInitDetail\(/)
assert.doesNotMatch(stockInitPage, /router\.push\(`\/finance\/stockInit\/detail/)
assert.match(stockInitPage, /删除调整单/)
assert.match(stockInitApi, /deleteStockInit/)
assert.match(stockInitService, /已过账或已删除的调整单不可删除/)
assert.match(stockInitService, /STATUS_POSTED\.equals\(header\.getStatus\(\)\)/)
assert.match(workbench, /\/member\/mp\/modules/)
assert.match(stockInitService, /setChangeType\([^)]*getAdjustmentType/)
assert.doesNotMatch(stockInitService, /ledger\.setChangeType\(STOCK_INIT\)/)

console.log('finance stock/miniprogram repair baseline checks passed')
