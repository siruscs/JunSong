import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const page = fs.readFileSync(new URL('../junsong-ui-v3/src/views/finance/product/index.vue', import.meta.url), 'utf8')
const api = fs.readFileSync(new URL('../junsong-ui-v3/src/api/finance/product.ts', import.meta.url), 'utf8')
const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinProductMapper.xml', import.meta.url), 'utf8')
const service = fs.readFileSync(new URL('../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinProductServiceImpl.java', import.meta.url), 'utf8')

test('product edit preserves the selected product id and sale price fields', () => {
  assert.match(page, /productId:\s*data\.productId \|\| productId/)
  assert.match(page, /salePrice:\s*data\.salePrice \?\? data\.sale_price/)
  assert.match(page, /if \(response\.code !== 200\) return/)
  assert.match(mapper, /sale_price = #\{product\.salePrice\}/)
})

test('product code uniqueness is scoped to the current department', () => {
  assert.match(mapper, /p\.dept_id = #\{deptId\}/)
  assert.match(mapper, /p\.product_id != #\{productId\}/)
  assert.match(service, /checkProductCodeUnique\(finProduct\.getProductCode\(\),\s*\n?\s*finProduct\.getDeptId\(\),\s*\n?\s*finProduct\.getProductId\(\)\)/)
})

test('product edits do not reuse a stale failed idempotency key', () => {
  assert.match(api, /idempotencyNewKey:\s*true/)
})

test('all manual PUT updates generate a fresh idempotency key by default', () => {
  const request = fs.readFileSync(new URL('../junsong-ui-v3/src/api/request.ts', import.meta.url), 'utf8')
  assert.match(request, /forceNewKey = .*method === 'put'/)
})
