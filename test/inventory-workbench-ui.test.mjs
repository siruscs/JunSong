import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const stock = fs.readFileSync('src/pages/stock/index.vue', 'utf8')
const ledger = fs.readFileSync('src/pages/stock-ledger/index.vue', 'utf8')
const adjustment = fs.readFileSync('src/pages/stock-adjustment/index.vue', 'utf8')

test('库存三个页面使用统一工作台结构和状态语义', () => {
  for (const page of [stock, ledger, adjustment]) {
    assert.match(page, /inventory-workbench/)
    assert.match(page, /workspace-header/)
    assert.match(page, /workspace-eyebrow/)
    assert.match(page, /workspace-title/)
  }
  assert.match(stock, /StateView/)
  assert.match(stock, /inventory-summary/)
  assert.match(ledger, /ledger-filters/)
  assert.match(ledger, /ledger-card/)
  assert.match(adjustment, /adjustment-card/)
  assert.match(adjustment, /adjustment-sheet/)
})

test('库存页面保留关键业务入口和安全区布局', () => {
  assert.match(stock, /openLedger\(item\)/)
  assert.match(ledger, /loadMore/)
  assert.match(adjustment, /canCreateAdjustment/)
  assert.match(adjustment, /safe-area-inset-bottom/)
})
