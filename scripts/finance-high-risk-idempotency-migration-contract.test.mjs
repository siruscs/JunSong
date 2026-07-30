import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const sql = fs.readFileSync('sql/finance_high_risk_idempotency_constraints.sql', 'utf8')

test('finance high risk idempotency migration skips optional finance_stocktake table when absent', () => {
  assert.match(sql, /@finance_stocktake_table_exists/)
  assert.match(sql, /finance_stocktake table missing, skip reverse_idempotency_key column/)
  assert.match(sql, /finance_stocktake table missing, skip uk_reverse_idempotency_key/)
  assert.match(sql, /finance_stocktake table missing, skip duplicate reverse idempotency check/)
})
