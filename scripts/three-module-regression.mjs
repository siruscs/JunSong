#!/usr/bin/env node

/**
 * 三大模块治理回归测试
 *
 * 一条命令完成三大模块治理回归：
 *   1. 会员模块单测（积分兑换、PII 脱敏、Controller、报表、Dashboard 收敛）
 *   2. 系统模块单测（Webhook + 通知已读 + 敏感配置脱敏）
 *   3. 财务模块单测（OCR + 报表数据权限 + 编码生成器）
 *   4. 全量权限扫描
 *   5. admin 健康检查
 *
 * 用法:
 *   npm run three-modules:regression
 *   node scripts/three-module-regression.mjs
 */

import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const ROOT = path.resolve(__dirname, '..')

export function createThreeModuleRegressionSteps() {
  return [
    {
      name: '会员模块单测',
      command: 'mvn',
      args: [
        '-pl', 'junsong-modules/junsong-member',
        '-Dtest=MemPointsExchangeServiceTest,PiiMaskingTest,MemMemberControllerTest,MemberReportServiceImplTest,MemMpDashboardControllerTest,MemberLifecycleTaskTest,MemAuditTrailTest,MemberContributionReportServiceImplTest',
        'test',
        '-DskipTests=false',
      ],
    },
    {
      name: '系统模块单测',
      command: 'mvn',
      args: [
        '-pl', 'junsong-modules/junsong-system',
        '-Dtest=WebhookUrlValidatorTest,SysNotificationServiceImplTest,SysNoticeReadServiceImplTest,SysConfigMaskingTest,SysAuditTrailTest',
        'test',
        '-DskipTests=false',
      ],
    },
    {
      name: '财务模块单测',
      command: 'mvn',
      args: [
        '-pl', 'junsong-modules/junsong-finance',
        '-Dtest=ExpenseOcrServiceImplTest,FinanceReportServiceImplTest,FinanceDrillDownServiceImplTest,StoreFinanceReportServiceImplTest,AuthorizedStoreFinanceReportServiceImplTest,CodeGeneratorTest,FinAttachmentMetadataTest,FinAuditTrailTest,FinanceOperationDashboardServiceImplTest,OperatingProfitReportServiceImplTest,ExpenseAnomalyReportServiceImplTest,SalesOperationReportServiceImplTest,ProfitShareSettlementDashboardServiceImplTest,AccountingPeriodLockGuardTest',
        'test',
        '-DskipTests=false',
      ],
    },
    {
      name: '全量权限扫描',
      command: 'node',
      args: ['scripts/backend-permission-health.mjs'],
    },
    {
      name: 'admin 健康检查',
      command: 'node',
      args: ['scripts/admin-health.mjs'],
    },
  ]
}

function runStep(step) {
  console.log(`\n${'='.repeat(60)}`)
  console.log(`[regression] ${step.name}`)
  console.log(`[regression] ${step.command} ${step.args.join(' ')}`)
  console.log('='.repeat(60))

  const result = spawnSync(step.command, step.args, {
    cwd: ROOT,
    encoding: 'utf8',
    stdio: 'inherit',
    shell: false,
  })

  return {
    name: step.name,
    status: result.status ?? 1,
  }
}

function main() {
  const startTime = Date.now()
  const results = []

  console.log('[regression] 三大模块治理回归测试')
  console.log(`[regression] 开始时间: ${new Date().toISOString()}`)

  const steps = createThreeModuleRegressionSteps()

  for (const step of steps) {
    const result = runStep(step)
    results.push(result)

    if (result.status !== 0) {
      console.log(`\n[regression] ✗ ${step.name} 失败 (exit code ${result.status})`)
      console.log('[regression] 后续步骤已跳过')
      break
    }

    console.log(`\n[regression] ✓ ${step.name} 通过`)
  }

  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1)
  const passed = results.every(r => r.status === 0)

  console.log(`\n${'='.repeat(60)}`)
  console.log(`[regression] 回归结果: ${passed ? '✓ 全部通过' : '✗ 存在失败'}`)
  console.log(`[regression] 耗时: ${elapsed}s`)
  console.log(`[regression] 完成步骤: ${results.length}/${steps.length}`)
  for (const r of results) {
    console.log(`  ${r.status === 0 ? '✓' : '✗'} ${r.name}`)
  }
  console.log('='.repeat(60))

  process.exit(passed ? 0 : 1)
}

if (process.argv[1] === __filename) {
  main()
}
