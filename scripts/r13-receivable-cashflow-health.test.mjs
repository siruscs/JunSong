import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R13 sale payment can bind current period without editing sale period', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinSaleRecordServiceImpl.java')
  assert.match(src, /getCurrentPeriodId\(sale\.getDeptId\(\)\)/)
  // addPayment must NOT check sale.getPeriodId() for editability
  assert.doesNotMatch(src, /addPayment[\s\S]{0,900}assertPeriodEditable\(sale\.getPeriodId\(\)\)/)
})

test('R13 receivable list endpoint exists', () => {
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinSaleRecordController.java')
  assert.match(controller, /\/receivable\/list/)
  assert.match(controller, /finance:sale:list/)
  const mapper = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinSaleRecordMapper.xml')
  assert.match(mapper, /selectReceivableList/)
  assert.match(mapper, /sale_amount\s*&gt;\s*COALESCE|coalesce\(r\.paid_amount,\s*0\)\s*&lt;\s*r\.sale_amount/i)
})

test('R13 frontend shows receivable tab and current-period payment hint', () => {
  const src = read('junsong-ui-v3/src/views/finance/sale/index.vue')
  assert.match(src, /历史欠款/)
  assert.match(src, /计入当前.*核算周期|核算周期.*不计入/)
})

test('R13 operation dashboard has receivable indicators', () => {
  const vo = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/FinanceOperationDashboardVO.java')
  assert.match(vo, /currentPeriodPaymentAmount/)
  assert.match(vo, /historicalReceivableCollectedAmount/)
  assert.match(vo, /endingReceivableAmount/)
})

test('R13 collection review task generation exists', () => {
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceReviewTaskServiceImpl.java')
  assert.match(service, /generateReceivableCollectionTasks/)
  assert.match(service, /RECEIVABLE_COLLECTION/)
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReviewTaskController.java')
  assert.match(controller, /receivable-collection/)
})

test('R13 overpayment protection in addPayment', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinSaleRecordServiceImpl.java')
  assert.match(src, /缴款金额不能大于剩余应收金额/)
})

test('R13 store dashboard has receivable follow-up panel', () => {
  const src = read('junsong-ui-v3/src/views/dashboard/StoreDashboard.vue')
  assert.match(src, /应收待跟进/)
})
