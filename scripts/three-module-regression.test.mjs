import test from 'node:test'
import assert from 'node:assert/strict'

import { createThreeModuleRegressionSteps } from './three-module-regression.mjs'

test('three module regression includes finance tests', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '财务模块单测' &&
      step.command === 'mvn' &&
      step.args.includes('junsong-modules/junsong-finance') &&
      step.args.some(arg =>
        arg.includes('ExpenseOcrServiceImplTest') &&
        arg.includes('FinanceReportServiceImplTest')
      ) &&
      step.args.includes('test')
    ),
  )
})

test('three module regression includes member report test', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '会员模块单测' &&
      step.command === 'mvn' &&
      step.args.includes('junsong-modules/junsong-member') &&
      step.args.some(arg => arg.includes('MemberReportServiceImplTest')) &&
      step.args.includes('test')
    ),
  )
})

test('three module regression includes finance code generator test', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '财务模块单测' &&
      step.args.some(arg => arg.includes('CodeGeneratorTest'))
    ),
    '财务模块单测应包含编码生成器测试'
  )
})

test('three module regression includes member dashboard batch test', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '会员模块单测' &&
      step.args.some(arg => arg.includes('MemMpDashboardControllerTest'))
    ),
    '会员模块单测应包含 Dashboard 查询收敛测试'
  )
})

test('three module regression includes system notification and masking tests', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '系统模块单测' &&
      step.command === 'mvn' &&
      step.args.includes('junsong-modules/junsong-system') &&
      step.args.some(arg =>
        arg.includes('SysNotificationServiceImplTest') &&
        arg.includes('SysNoticeReadServiceImplTest') &&
        arg.includes('SysConfigMaskingTest')
      ) &&
      step.args.includes('test')
    ),
    '系统模块单测应包含通知已读/未读和敏感配置脱敏测试'
  )
})

test('three module regression includes audit trail tests', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '会员模块单测' &&
      step.args.some(arg => arg.includes('MemAuditTrailTest'))
    ),
    '会员模块单测应包含 PII 导出审计测试'
  )

  assert.ok(
    steps.some(step =>
      step.name === '系统模块单测' &&
      step.args.some(arg => arg.includes('SysAuditTrailTest'))
    ),
    '系统模块单测应包含审计快照测试'
  )

  assert.ok(
    steps.some(step =>
      step.name === '财务模块单测' &&
      step.args.some(arg => arg.includes('FinAuditTrailTest'))
    ),
    '财务模块单测应包含审计快照测试'
  )
})

test('three module regression includes store finance report test', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '财务模块单测' &&
      step.args.some(arg => arg.includes('StoreFinanceReportServiceImplTest'))
    ),
    '财务模块单测应包含单门店财务报表测试 StoreFinanceReportServiceImplTest'
  )
})

test('three module regression includes decision console finance tests', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '财务模块单测' &&
      step.args.some(arg =>
        arg.includes('FinanceOperationDashboardServiceImplTest') &&
        arg.includes('OperatingProfitReportServiceImplTest') &&
        arg.includes('ExpenseAnomalyReportServiceImplTest') &&
        arg.includes('SalesOperationReportServiceImplTest') &&
        arg.includes('ProfitShareSettlementDashboardServiceImplTest') &&
        arg.includes('AccountingPeriodLockGuardTest') &&
        arg.includes('FinanceDrillDownServiceImplTest')
      )
    ),
    '财务模块单测应包含经营决策台全部 7 个新测试类（含钻取服务）'
  )
})

test('three module regression includes member contribution report test', () => {
  const steps = createThreeModuleRegressionSteps()

  assert.ok(
    steps.some(step =>
      step.name === '会员模块单测' &&
      step.args.some(arg => arg.includes('MemberContributionReportServiceImplTest'))
    ),
    '会员模块单测应包含会员经营贡献报表测试 MemberContributionReportServiceImplTest'
  )
})

test('three module regression keeps permission and admin gates after module checks', () => {
  const steps = createThreeModuleRegressionSteps()
  const names = steps.map(step => step.name)

  assert.deepEqual(names.slice(-2), ['全量权限扫描', 'admin 健康检查'])
})
