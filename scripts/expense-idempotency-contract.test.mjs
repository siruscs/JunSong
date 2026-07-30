import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const source = fs.readFileSync(
  new URL('../junsong-ui-v3/src/api/finance/expense.ts', import.meta.url),
  'utf8',
)

test('expense creation explicitly carries an idempotency key', () => {
  assert.match(source, /generateIdempotencyKey/)
  assert.match(source, /idempotencyKey\s*:/)
})
