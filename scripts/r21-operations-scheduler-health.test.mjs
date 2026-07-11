import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.existsSync(path) ? fs.readFileSync(path, 'utf8') : ''

test('R21 operation scheduler deliverables exist', () => {
  assert.ok(fs.existsSync('sql/r21_operations_scheduler.sql'), 'SQL file missing')
  assert.ok(fs.existsSync('scripts/r21-operations-scheduler-health.test.mjs'), 'health test missing')
})

test('R21 scheduler has required job codes and observable statuses', () => {
  const corpus = [
    read('sql/r21_operations_scheduler.sql'),
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysOperationSchedulerController.java'),
    read('junsong-modules/junsong-system/src/main/resources/mapper/system/SysOperationScheduleLogMapper.xml')
  ].join('\n')

  for (const jobCode of [
    'R21_CASHFLOW_FORECAST_SNAPSHOT',
    'R21_MEMBER_GROWTH_EFFECT_BACKFILL',
    'R21_STOCK_DAILY_SNAPSHOT',
    'R21_OPERATION_MEMO_DRAFT'
  ]) {
    assert.match(corpus, new RegExp(jobCode), `missing jobCode: ${jobCode}`)
  }

  for (const status of ['SUCCESS', 'FAILED', 'SKIPPED', 'PARTIAL']) {
    assert.match(corpus, new RegExp(status), `missing status: ${status}`)
  }
})

test('R21 has manual trigger and recent results APIs', () => {
  const controller = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysOperationSchedulerController.java')

  assert.match(controller, /POST|PostMapping.*trigger/i, 'manual trigger API missing')
  assert.match(controller, /GET|GetMapping.*(dashboard|recent)/i, 'dashboard/recent API missing')
  assert.match(controller, /system:operation-scheduler:view/, 'view permission missing')
  assert.match(controller, /system:operation-scheduler:trigger/, 'trigger permission missing')
})

test('R21 has scheduled trigger capability via @Scheduled', () => {
  const dispatcher = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/scheduler/R21ScheduledDispatcher.java')
  const executor = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/scheduler/R21SchedulerExecutor.java')

  assert.ok(dispatcher.length > 0, 'R21ScheduledDispatcher.java missing')
  assert.ok(executor.length > 0, 'R21SchedulerExecutor.java missing')
  assert.match(dispatcher, /@Scheduled/, 'R21ScheduledDispatcher must use @Scheduled annotation')

  const scheduledCount = (dispatcher.match(/@Scheduled/g) || []).length
  assert.ok(scheduledCount >= 4, `expected at least 4 @Scheduled methods, found ${scheduledCount}`)

  for (const jobCode of [
    'R21_CASHFLOW_FORECAST_SNAPSHOT',
    'R21_MEMBER_GROWTH_EFFECT_BACKFILL',
    'R21_STOCK_DAILY_SNAPSHOT',
    'R21_OPERATION_MEMO_DRAFT'
  ]) {
    assert.match(dispatcher, new RegExp(jobCode), `dispatcher missing jobCode: ${jobCode}`)
  }

  assert.match(executor, /SCHEDULED/, 'executor must support SCHEDULED trigger type')
  assert.match(executor, /MANUAL/, 'executor must support MANUAL trigger type')
})

test('R21 does not contain forbidden R22-R25 code', () => {
  const newFiles = [
    read('sql/r21_operations_scheduler.sql'),
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysOperationSchedulerController.java'),
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysOperationScheduleLogServiceImpl.java'),
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/task/CashflowForecastSnapshotTask.java'),
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/task/StockDailySnapshotTask.java'),
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/task/OperationMemoDraftTask.java'),
    read('junsong-modules/junsong-member/src/main/java/com/junsong/member/task/MemberGrowthEffectBackfillTask.java'),
  ].join('\n')

  const forbidden = [/sms/i, /wechat/i, /webhook\s*subscription/i, /open\s*platform/i, /what-if/i, /load\s*test/i]
  for (const pattern of forbidden) {
    assert.doesNotMatch(newFiles, pattern, `forbidden pattern found: ${pattern}`)
  }
})

test('R21 is registered in admin-health', () => {
  const adminHealth = read('scripts/admin-health.mjs')
  assert.match(adminHealth, /R21 operations scheduler health/, 'R21 not registered in admin-health')
})
