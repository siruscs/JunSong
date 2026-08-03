import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const page = fs.readFileSync('src/pages/stock-ledger/index.vue', 'utf8')

test('stock ledger exposes paged date and type filters', () => {
  assert.match(page, /@scrolltolower="loadMore"/)
  assert.match(page, /pageNum: 1/)
  assert.match(page, /pageSize: 30/)
  assert.match(page, /startDate/)
  assert.match(page, /endDate/)
  assert.match(page, /selectedChangeType/)
  assert.match(page, /changeType: this\.selectedChangeType \|\| undefined/)
})

test('department changes reset stock ledger pagination', () => {
  assert.match(page, /this\.currentDeptId = s\.currentDeptId[\s\S]{0,180}this\.reload\(\)/)
})
