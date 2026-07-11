import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const detailPath = new URL('../junsong-miniprogram/src/pages/detail/index.vue', import.meta.url)
const detail = await readFile(detailPath, 'utf8')

test('sale detail exposes payment as a footer action instead of an inline receipt module', () => {
  assert.doesNotMatch(detail, /确认收款/)
  assert.doesNotMatch(detail, /收款信息/)
  assert.match(detail, /v-if="canPayment"/)
  assert.match(detail, />缴款<\/button>/)
  assert.match(detail, /hasActionPermission\(this\.moduleKey, 'payment'\)/)
})

test('payment panel matches the PC payment fields and endpoint contract', () => {
  assert.match(detail, /缴款日期/)
  assert.match(detail, /缴款金额/)
  assert.match(detail, /付款方式/)
  assert.match(detail, /剩余应收/)
  assert.match(detail, /paymentDate/)
  assert.match(detail, /paymentAmount/)
  assert.match(detail, /url: '\/finance\/sale\/payment\/'/)
})

test('payment is hidden for paid-off sales and rejects overpayment', () => {
  assert.match(detail, /String\(this\.record\?\.status\) !== '2'/)
  assert.match(detail, /缴款金额不能超过剩余应收/)
  assert.match(detail, /paymentCents > remainingCents/)
  assert.match(detail, /Math\.round\(Number\(this\.record\.saleAmount \|\| 0\) \* 100\)/)
})
