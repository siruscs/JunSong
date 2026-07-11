import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R15 sql creates receivable collection tables and permissions', () => {
  const sql = read('sql/finance_receivable_collection_r15.sql')
  assert.match(sql, /CREATE TABLE IF NOT EXISTS finance_receivable_collection/i)
  assert.match(sql, /CREATE TABLE IF NOT EXISTS finance_receivable_collection_log/i)
  assert.match(sql, /finance:receivableCollection:list/)
  assert.match(sql, /finance:receivableCollection:edit/)
  assert.match(sql, /finance:receivableCollection:sync/)
})

test('R15 backend exposes command center endpoints with permissions', () => {
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/ReceivableCollectionController.java')
  assert.match(controller, /@RequiresPermissions\("finance:receivableCollection:list"\)/)
  assert.match(controller, /@RequiresPermissions\("finance:receivableCollection:edit"\)/)
  assert.match(controller, /@RequiresPermissions\("finance:receivableCollection:sync"\)/)
  assert.match(controller, /\/receivable-collection\/dashboard/)
  assert.match(controller, /\/receivable-collection\/list/)
  assert.match(controller, /\/receivable-collection\/sync/)
  assert.match(controller, /\/receivable-collection\/\{collectionId\}\/follow/)
})

test('R15 backend models promise and follow-up lifecycle', () => {
  const domain = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinanceReceivableCollection.java')
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/ReceivableCollectionServiceImpl.java')
  assert.match(domain, /promisedPayDate/)
  assert.match(domain, /promisedAmount/)
  assert.match(domain, /nextFollowTime/)
  assert.match(domain, /collectionStatus/)
  assert.match(service, /PENDING/)
  assert.match(service, /PROMISED/)
  assert.match(service, /PAID/)
  assert.match(service, /getAgeBucket/)
})

test('R15 frontend adds receivable collection command center', () => {
  const api = read('junsong-ui-v3/src/api/finance/receivableCollection.ts')
  const page = read('junsong-ui-v3/src/views/finance/receivableCollection/index.vue')
  const storeDashboard = read('junsong-ui-v3/src/views/dashboard/StoreDashboard.vue')
  assert.match(api, /getReceivableCollectionDashboard/)
  assert.match(api, /syncReceivableCollections/)
  assert.match(api, /followReceivableCollection/)
  assert.match(page, /应收催收作战台/)
  assert.match(page, /承诺回款/)
  assert.match(page, /下次跟进/)
  assert.match(page, /账龄分层/)
  assert.match(storeDashboard, /今日催收/)
})
