import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const form = fs.readFileSync(new URL('../src/pages/form/index.vue', import.meta.url), 'utf8')
const detail = fs.readFileSync(new URL('../src/pages/detail/index.vue', import.meta.url), 'utf8')

test('form shows submitting state and explains unknown write results', () => {
  assert.match(form, /submitting \? '保存中' :/)
  assert.match(form, /isUnknownWriteOutcome\(e\)/)
  assert.match(form, /保存结果待确认/)
})

test('sale payment is locked while submitting and ambiguous results refresh detail', () => {
  assert.match(detail, /paymentSubmitting:/)
  assert.match(detail, /:disabled="paymentSubmitting"/)
  assert.match(detail, /if \(this\.paymentSubmitting\) return/)
  assert.match(detail, /isUnknownWriteOutcome\(e\)[\s\S]*?this\.closePayment\(\)[\s\S]*?await this\.loadDetail\(\)/)
})

test('return-sale payment compares against the absolute remaining amount', () => {
  const block = detail.match(/async submitPayment\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(block, /const remainingCents = Math\.abs\(Math\.round\(this\.remainingAmount \* 100\)\)/)
})
