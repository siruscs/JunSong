import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

const stockVue = read('junsong-ui-v3/src/views/finance/report/stock.vue')
const stockReportApi = read('junsong-ui-v3/src/api/finance/stockreport.ts')
const ledgerDrawer = read('junsong-ui-v3/src/views/finance/report/components/StockLedgerDrawer.vue')

test('stock.vue no longer shows the pause notice', () => {
  assert.doesNotMatch(stockVue, /暂未开放/, 'stock.vue must not contain the paused notice text')
})

test('stock.vue calls the stock report API', () => {
  assert.match(stockVue, /getStockReport/, 'stock.vue must call getStockReport or similar')
})

test('stock.vue shows summary flow metrics', () => {
  assert.match(stockVue, /期初/)
  assert.match(stockVue, /采购净入库/)
  assert.match(stockVue, /销售净出库/)
  assert.match(stockVue, /期末/)
})

test('stock.vue shows anomaly metrics', () => {
  assert.match(stockVue, /负库存/)
  assert.match(stockVue, /低库存/)
  assert.match(stockVue, /滞销/)
  assert.match(stockVue, /异常/)
})

test('stock.vue does NOT recalculate gifts in the frontend', () => {
  assert.doesNotMatch(stockVue, /isGift/, 'frontend must not recalculate gifts (isGift)')
  assert.doesNotMatch(stockVue, /giftQuantity/, 'frontend must not recalculate gifts (giftQuantity)')
})

test('stock.vue export button is permission-gated', () => {
  assert.match(
    stockVue,
    /finance:report:stock:export/,
    'export button must check finance:report:stock:export permission',
  )
  assert.match(stockVue, /v-hasPermi/, 'export must use v-hasPermi directive')
})

test('stock.vue fails closed on API errors (clears data, shows message)', () => {
  assert.match(stockVue, /\.catch\(/, 'must have a catch handler')
  assert.match(stockVue, /clearData|summary\.value\s*=\s*null|items\.value\s*=\s*\[\]/, 'must clear old data on error')
  assert.match(stockVue, /ElMessage\.error/, 'must show error message on failure')
})

test('stock.vue uses Composition API with script setup', () => {
  assert.match(stockVue, /<script setup lang="ts">/)
})

test('stock.vue uses authorized depts from user store', () => {
  assert.match(stockVue, /useUserStore/)
  assert.match(stockVue, /depts/)
})

test('stockreport.ts defines all 6 API functions', () => {
  assert.match(stockReportApi, /export function getStockReport\b/)
  assert.match(stockReportApi, /export function getStockReportSummary\b/)
  assert.match(stockReportApi, /export function getStockReportPage\b/)
  assert.match(stockReportApi, /export function getStockLedgerPage\b/)
  assert.match(stockReportApi, /export function exportStockReport\b/)
  assert.match(stockReportApi, /export function getStockReconciliation\b/)
})

test('stockreport.ts export uses blob response type', () => {
  assert.match(stockReportApi, /responseType:\s*'blob'/)
})

test('StockLedgerDrawer.vue exists and calls the ledger API', () => {
  assert.match(ledgerDrawer, /getStockLedgerPage/, 'drawer must call getStockLedgerPage')
  assert.match(ledgerDrawer, /el-drawer/, 'drawer must render an el-drawer')
  assert.match(ledgerDrawer, /el-pagination/, 'drawer must have its own pagination')
  assert.match(ledgerDrawer, /\.catch\(/, 'drawer must handle errors')
})

test('StockLedgerDrawer.vue renders the required ledger columns', () => {
  assert.match(ledgerDrawer, /变动时间/)
  assert.match(ledgerDrawer, /变动类型/)
  assert.match(ledgerDrawer, /变动前数量/)
  assert.match(ledgerDrawer, /变动数量/)
  assert.match(ledgerDrawer, /变动后数量/)
  assert.match(ledgerDrawer, /来源类型/)
  assert.match(ledgerDrawer, /来源单号/)
  assert.match(ledgerDrawer, /操作人/)
  assert.match(ledgerDrawer, /备注/)
})
