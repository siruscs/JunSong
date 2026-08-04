import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const stock = fs.readFileSync('src/pages/stock/index.vue', 'utf8')
const ledger = fs.readFileSync('src/pages/stock-ledger/index.vue', 'utf8')
const adjustment = fs.readFileSync('src/pages/stock-adjustment/index.vue', 'utf8')
const list = fs.readFileSync('src/pages/list/index.vue', 'utf8')

test('库存三个页面使用销售记录同款通用业务页结构', () => {
  for (const page of [stock, ledger, adjustment]) {
    assert.match(page, /class="hero"/)
    assert.match(page, /class="work-scope"/)
    assert.match(page, /section-card/)
    assert.match(page, /section-header/)
  }
  assert.match(stock, /StateView/)
  assert.match(stock, /summary-bar/)
  assert.doesNotMatch(stock, /库存明细/)
  assert.match(ledger, /filters-card/)
  assert.match(ledger, /库存流水：\{\{ productName/)
  assert.doesNotMatch(ledger, /<view class="product-context"/)
  assert.match(ledger, /filter-inline/)
  assert.match(ledger, /ledger-card/)
  assert.match(adjustment, /adjustment-card/)
  assert.match(adjustment, /bottom-action/)
  assert.match(adjustment, /sheet/)
})

test('库存页面保留关键业务入口和安全区布局', () => {
  assert.match(stock, /openLedger\(item\)/)
  assert.match(ledger, /loadMore/)
  assert.match(adjustment, /canCreateAdjustment/)
  assert.match(adjustment, /safe-area-inset-bottom/)
})

test('accounting period list is restricted to the selected department', () => {
  assert.match(list, /query\.deptId = this\.currentDeptId/)
  assert.match(list, /this\.moduleKey === 'accountingPeriod'/)
  assert.match(list, /String\(item\.deptId\) === String\(requestDeptId\)/)
})
