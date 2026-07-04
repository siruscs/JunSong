import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { spawnSync } from 'node:child_process'

import { createAdminHealthChecks, createAdminHealthReport } from './admin-health.mjs'

// R11-FIX-E: 使用 process.execPath 替代硬编码 'node'，兼容 PATH 没有 node 的环境
const NODE = process.execPath

test('createAdminHealthChecks returns offline checks by default', () => {
  const checks = createAdminHealthChecks({ includeDev: false })

  assert.deepEqual(
    checks.map((check) => check.name),
    ['module overview verifier', 'admin menu health unit tests', 'admin menu static health', 'backend permission health (full scan)', 'mybatis mapper empty statement unit tests', 'mybatis mapper empty statement scan', 'finance sql health unit tests', 'list order health unit tests', 'expense verified edit health unit tests', 'stock ledger health unit tests', 'stock negative sale config health unit tests', 'overview real data health unit tests', 'overview real data static health', 'store dashboard evolution health unit tests', 'r9 closure health unit tests', 'r10 readiness health unit tests', 'r10 config quality health unit tests', 'r11 readiness health unit tests', 'r11 store health knowledge health unit tests', 'r12 action effect health unit tests', 'r13 receivable cashflow health unit tests', 'r14 receivable closure health unit tests', 'r15 receivable command center health unit tests', 'r16 cashflow forecast health unit tests', 'R17 member growth action loop health', 'R18 R1-R18 closure health', 'R19 release governance health', 'R20 metrics governance health', 'R21 operations scheduler health', 'R22 action center touch health', 'R23 open platform health', 'R23 openapi drift check', 'R24 predictive ops health'],
  )
  assert.deepEqual(checks[0].command, [NODE, 'scripts/verify-module-overviews.mjs'])
  assert.deepEqual(checks[1].command, [NODE, '--test', 'scripts/admin-menu-health.test.mjs'])
  assert.deepEqual(checks[2].command, [NODE, 'scripts/admin-menu-health.mjs'])
  assert.deepEqual(checks[3].command, [NODE, 'scripts/backend-permission-health.mjs'])
  assert.deepEqual(checks[4].command, [NODE, '--test', 'scripts/backend-mybatis-health.test.mjs'])
  assert.deepEqual(checks[5].command, [NODE, 'scripts/backend-mybatis-health.mjs'])
  assert.deepEqual(checks[6].command, [NODE, '--test', 'scripts/finance-sql-health.test.mjs'])
  assert.deepEqual(checks[7].command, [NODE, '--test', 'scripts/list-order-health.test.mjs'])
  assert.deepEqual(checks[8].command, [NODE, '--test', 'scripts/expense-verified-edit-health.test.mjs'])
  assert.deepEqual(checks[9].command, [NODE, '--test', 'scripts/stock-ledger-health.test.mjs'])
  assert.deepEqual(checks[10].command, [NODE, '--test', 'scripts/stock-negative-sale-config-health.test.mjs'])
  assert.deepEqual(checks[11].command, [NODE, '--test', 'scripts/overview-real-data-health.test.mjs'])
  assert.deepEqual(checks[12].command, [NODE, 'scripts/overview-real-data-health.mjs'])
  assert.deepEqual(checks[13].command, [NODE, '--test', 'scripts/store-dashboard-evolution-health.test.mjs'])
  assert.deepEqual(checks[14].command, [NODE, '--test', 'scripts/r9-closure-health.test.mjs'])
  assert.deepEqual(checks[15].command, [NODE, '--test', 'scripts/r10-readiness-health.test.mjs'])
  assert.deepEqual(checks[16].command, [NODE, '--test', 'scripts/r10-config-quality-health.test.mjs'])
  assert.deepEqual(checks[17].command, [NODE, '--test', 'scripts/r11-readiness-health.test.mjs'])
  assert.deepEqual(checks[18].command, [NODE, '--test', 'scripts/r11-store-health-knowledge-health.test.mjs'])
  assert.deepEqual(checks[19].command, [NODE, '--test', 'scripts/r12-action-effect-health.test.mjs'])
  assert.deepEqual(checks[20].command, [NODE, '--test', 'scripts/r13-receivable-cashflow-health.test.mjs'])
  assert.deepEqual(checks[21].command, [NODE, '--test', 'scripts/r14-receivable-closure-health.test.mjs'])
  assert.deepEqual(checks[22].command, [NODE, '--test', 'scripts/r15-receivable-command-center-health.test.mjs'])
  assert.deepEqual(checks[23].command, [NODE, '--test', 'scripts/r16-cashflow-forecast-health.test.mjs'])
  assert.deepEqual(checks[24].command, [NODE, '--test', 'scripts/r17-member-growth-action-health.test.mjs'])
  assert.deepEqual(checks[25].command, [NODE, '--test', 'scripts/r18-r1-r18-closure-health.test.mjs'])
  assert.deepEqual(checks[26].command, [NODE, '--test', 'scripts/r19-release-governance-health.test.mjs'])
  assert.deepEqual(checks[27].command, [NODE, '--test', 'scripts/r20-metrics-governance-health.test.mjs'])
  assert.deepEqual(checks[28].command, [NODE, '--test', 'scripts/r21-operations-scheduler-health.test.mjs'])
  assert.deepEqual(checks[29].command, [NODE, '--test', 'scripts/r22-action-center-touch-health.test.mjs'])
  assert.deepEqual(checks[30].command, [NODE, '--test', 'scripts/r23-open-platform-health.test.mjs'])
  assert.deepEqual(checks[31].command, [NODE, 'scripts/r23-openapi-drift-check.mjs'])
  assert.deepEqual(checks[32].command, [NODE, '--test', 'scripts/r24-predictive-ops-health.test.mjs'])
  assert.ok(checks.some((check) => check.name.includes('R17 member growth action loop health')))
  assert.ok(checks.some((check) => check.name.includes('R18 R1-R18 closure health')))
  assert.ok(checks.some((check) => check.name.includes('R19 release governance health')))
  assert.ok(checks.some((check) => check.name.includes('R20 metrics governance health')))
  assert.ok(checks.some((check) => check.name.includes('R21 operations scheduler health')))
  assert.ok(checks.some((check) => check.name.includes('R22 action center touch health')))
  assert.ok(checks.some((check) => check.name.includes('R23 open platform health')))
  assert.ok(checks.some((check) => check.name.includes('R23 openapi drift check')))
  assert.ok(checks.some((check) => check.name.includes('R24 predictive ops health')))
})

test('createAdminHealthChecks includes DEV database check when requested', () => {
  const checks = createAdminHealthChecks({ includeDev: true })

  assert.equal(checks.length, 34)
  assert.deepEqual(checks[33], {
    name: 'admin menu DEV database health',
    command: [NODE, 'scripts/admin-menu-health.mjs', '--dev'],
  })
})

test('createAdminHealthReport returns machine-readable passing report', () => {
  const report = createAdminHealthReport({
    includeDev: false,
    runCommand: () => ({ status: 0, stdout: 'ok', stderr: '' }),
  })

  assert.equal(report.ok, true)
  assert.equal(report.mode, 'offline')
  assert.equal(report.checks.length, 33)
  assert.deepEqual(
    report.checks.map((check) => check.status),
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  )
})

test('createAdminHealthReport stops on first failure and records output', () => {
  const report = createAdminHealthReport({
    includeDev: true,
    runCommand: (check) => {
      if (check.name === 'admin menu static health') {
        return { status: 1, stdout: '', stderr: 'static failed' }
      }
      return { status: 0, stdout: 'ok', stderr: '' }
    },
  })

  assert.equal(report.ok, false)
  assert.equal(report.mode, 'dev')
  assert.equal(report.failedCheck, 'admin menu static health')
  assert.equal(report.checks.length, 3)
  assert.equal(report.checks[2].stderr, 'static failed')
})

test('overview-real-data-health script exists and is executable', () => {
  assert.ok(existsSync('scripts/overview-real-data-health.mjs'), 'script file should exist')
  // R11-FIX-E: 使用 process.execPath 替代硬编码 'node'
  const result = spawnSync(NODE, ['scripts/overview-real-data-health.mjs'], {
    encoding: 'utf8',
  })
  assert.equal(
    result.status,
    0,
    `script should exit 0\nstdout: ${result.stdout}\nstderr: ${result.stderr}`,
  )
})
