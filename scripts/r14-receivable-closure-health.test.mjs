import { readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

const overview = readFileSync('junsong-ui-v3/src/views/finance/overview/index.vue', 'utf8')
const storeDashboard = readFileSync('junsong-ui-v3/src/views/dashboard/StoreDashboard.vue', 'utf8')
const reviewApi = readFileSync('junsong-ui-v3/src/api/finance/reviewTask.ts', 'utf8')
const reviewPage = readFileSync('junsong-ui-v3/src/views/finance/reviewTask/index.vue', 'utf8')
const saleMapper = readFileSync('junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinSaleRecordMapper.xml', 'utf8')
const saleService = readFileSync('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinSaleRecordServiceImpl.java', 'utf8')

test('R14 finance overview renders receivable metrics from operation dashboard', () => {
  assert.match(overview, /本期实收/)
  assert.match(overview, /历史欠款回收/)
  assert.match(overview, /本期新增应收/)
  assert.match(overview, /期末应收余额/)
  assert.match(overview, /逾期应收/)
  assert.match(overview, /currentPeriodPaymentAmount/)
  assert.match(overview, /historicalReceivableCollectedAmount/)
  assert.match(overview, /currentPeriodNewReceivableAmount/)
  assert.match(overview, /endingReceivableAmount/)
  assert.match(overview, /overdueReceivableCount/)
})

test('R14 store dashboard keeps receivable follow-up and adds receivable pressure metrics', () => {
  assert.match(storeDashboard, /应收待跟进/)
  assert.match(storeDashboard, /待跟进应收/)
  assert.match(storeDashboard, /逾期应收/)
})

test('R14 review task frontend can generate receivable collection tasks', () => {
  assert.match(reviewApi, /generateReceivableCollectionTasks/)
  assert.match(reviewApi, /\/finance\/review-task\/generate\/receivable-collection/)
  assert.match(reviewPage, /生成催收任务/)
  assert.match(reviewPage, /minAgeDays/)
  assert.match(reviewPage, /minUnpaidAmount/)
})

test('R14 payment concurrency guard uses row lock before overpayment validation', () => {
  assert.match(saleMapper, /selectFinSaleRecordBySaleIdForUpdate/)
  assert.match(saleMapper, /FOR UPDATE/i)
  assert.match(saleService, /selectFinSaleRecordBySaleIdForUpdate/)
  assert.match(saleService, /sumPaymentAmountBySaleId/)
  assert.match(saleService, /缴款金额不能大于剩余应收金额/)
})
