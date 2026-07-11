import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('stock ledger sale out reads allow-negative sys_config key', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinStockLedgerServiceImpl.java')
  assert.match(src, /finance\.stock\.allowNegativeSaleOut/)
  assert.match(src, /CacheConstants\.SYS_CONFIG_KEY\s*\+\s*ALLOW_NEGATIVE_SALE_OUT_KEY/)
  assert.match(src, /after\s*<\s*0\s*&&\s*!\s*isAllowNegativeSaleOut\(\)/)
})

test('allow-negative sale out config sql defaults to false', () => {
  const sql = read('sql/finance_stock_allow_negative_sale_out_config.sql')
  assert.match(sql, /finance\.stock\.allowNegativeSaleOut/)
  assert.match(sql, /'false'/)
  assert.match(sql, /WHERE NOT EXISTS/i)
})
