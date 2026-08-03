import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const page = fs.readFileSync('src/pages/stock-adjustment/index.vue', 'utf8')
const api = fs.readFileSync('src/api/stockInit.js', 'utf8')

test('stock adjustment supports multiple product rows', () => {
  assert.match(page, /v-for="\(item, index\) in form\.items"/)
  assert.match(page, /addItem\(\)/)
  assert.match(page, /removeItem\(index\)/)
  assert.match(page, /\.map\(row => createEmptyItem/)
})

test('stock adjustment validates every product row', () => {
  assert.match(page, /for \(const item of this\.form\.items\)/)
  assert.match(page, /最多三位小数/)
  assert.match(page, /最多两位小数/)
})

test('stock adjustment workflow exposes deterministic action keys', () => {
  assert.match(api, /buildStockInitIdempotencyKey/)
  assert.match(api, /idempotencyKey: buildStockInitIdempotencyKey\(batchId, 'validate'/)
  assert.match(api, /buildStockInitIdempotencyKey\(batchId, 'post'/)
})
