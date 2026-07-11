import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('expense page does not expose edit action for verified expenses', () => {
  const src = read('junsong-ui-v3/src/views/finance/expense/index.vue')
  assert.match(src, /canEditExpense\s*\(/, '费用修改按钮必须经过状态判断')
  assert.match(src, /scope\.row\.status\s*!==\s*['"]1['"]/, '已核销费用不能显示修改入口')
  assert.match(src, /已核销费用不能修改/, '直接调用修改处理时也必须提示并阻断')
})

test('finance service keeps a server-side guard against editing verified expenses', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseServiceImpl.java')
  assert.match(src, /VerifyStatus\.VERIFIED\.equals\(existing\.getStatus\(\)\)/)
  assert.match(src, /已核销的费用记录不可修改/)
})

test('expense page uses independent verification permissions and batch request contract', () => {
  const view = read('junsong-ui-v3/src/views/finance/expense/index.vue')
  const api = read('junsong-ui-v3/src/api/finance/expense.ts')
  assert.match(view, /finance:expense:verify/)
  assert.match(view, /finance:expense:unverify/)
  const batchButton = view.split('\n').find(line => line.includes('@click="handleBatchVerify"')) || ''
  assert.doesNotMatch(batchButton, /finance:expense:edit/)
  assert.match(api, /interface ExpenseVerifyRequest/)
  assert.match(api, /requestId: string/)
  assert.doesNotMatch(view, /\bverifyExpense\b/)
})

test('expense page supports capability-gated reversal with a required reason', () => {
  const view = read('junsong-ui-v3/src/views/finance/expense/index.vue')
  const api = read('junsong-ui-v3/src/api/finance/expense.ts')
  assert.match(api, /url: `\/finance\/expense\/unverify\/\$\{batchId\}`/)
  assert.match(api, /url: `\/finance\/expense\/\$\{expenseId\}\/capability`/)
  assert.match(view, /反核销原因/)
  assert.match(view, /canUnverify/)
  assert.match(view, /operationDisabledReason/)
  assert.match(view, /\.trim\(\)/)
  assert.match(view, /unverifyForm:\s*\{[^}]*requestId:/s)
  assert.match(view, /handleUnverify\(row\)[^]*requestId:\s*this\.createRequestId\(\)/)
  const submitUnverify = view.match(/submitUnverify\(\)\s*\{([^]*?)\n\s*\}\n\s*\}/)?.[1] || ''
  assert.match(submitUnverify, /requestId:\s*this\.unverifyForm\.requestId/)
  assert.doesNotMatch(submitUnverify, /createRequestId/)
  assert.match(view, /v-if="unverifyCapability\.canUnverify"[^>]*@click="submitUnverify"/)
  assert.match(view, /handleUnverify\(row\)[^]*this\.unverifyOpen = true[^]*if \(!capability\.canUnverify\)/)
  assert.match(view, /核销批次/)
})

test('expense verification computes differences and uses single-aware feedback', () => {
  const view = read('junsong-ui-v3/src/views/finance/expense/index.vue')
  assert.match(view, /this\.selectedAdvances\.length === 0/)
  assert.doesNotMatch(view, /!this\.selectedAdvances\.length === 0/)
  assert.match(view, /selectedExpenses\.length === 1/)
  assert.match(view, /费用核销成功/)
  assert.match(view, /批量核销成功/)
})
