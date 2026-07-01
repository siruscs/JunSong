import test from 'node:test'
import assert from 'node:assert/strict'

import { createAdminHealthChecks, createAdminHealthReport } from './admin-health.mjs'

test('createAdminHealthChecks returns offline checks by default', () => {
  const checks = createAdminHealthChecks({ includeDev: false })

  assert.deepEqual(
    checks.map((check) => check.name),
    ['module overview verifier', 'admin menu health unit tests', 'admin menu static health', 'backend permission health (full scan)', 'mybatis mapper empty statement unit tests', 'mybatis mapper empty statement scan', 'finance sql health unit tests', 'list order health unit tests'],
  )
  assert.deepEqual(checks[0].command, ['node', 'scripts/verify-module-overviews.mjs'])
  assert.deepEqual(checks[1].command, ['node', '--test', 'scripts/admin-menu-health.test.mjs'])
  assert.deepEqual(checks[2].command, ['node', 'scripts/admin-menu-health.mjs'])
  assert.deepEqual(checks[3].command, ['node', 'scripts/backend-permission-health.mjs'])
  assert.deepEqual(checks[4].command, ['node', '--test', 'scripts/backend-mybatis-health.test.mjs'])
  assert.deepEqual(checks[5].command, ['node', 'scripts/backend-mybatis-health.mjs'])
  assert.deepEqual(checks[6].command, ['node', '--test', 'scripts/finance-sql-health.test.mjs'])
  assert.deepEqual(checks[7].command, ['node', '--test', 'scripts/list-order-health.test.mjs'])
})

test('createAdminHealthChecks includes DEV database check when requested', () => {
  const checks = createAdminHealthChecks({ includeDev: true })

  assert.equal(checks.length, 9)
  assert.deepEqual(checks[8], {
    name: 'admin menu DEV database health',
    command: ['node', 'scripts/admin-menu-health.mjs', '--dev'],
  })
})

test('createAdminHealthReport returns machine-readable passing report', () => {
  const report = createAdminHealthReport({
    includeDev: false,
    runCommand: () => ({ status: 0, stdout: 'ok', stderr: '' }),
  })

  assert.equal(report.ok, true)
  assert.equal(report.mode, 'offline')
  assert.equal(report.checks.length, 8)
  assert.deepEqual(
    report.checks.map((check) => check.status),
    [0, 0, 0, 0, 0, 0, 0, 0],
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
