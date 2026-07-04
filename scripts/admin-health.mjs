import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// R11-FIX-E: 使用 process.execPath 替代硬编码 'node'，
// 兼容 PATH 中没有 node 的执行环境（如某些 CI/Codex shell）。
const NODE = process.execPath

export function createAdminHealthChecks({ includeDev = false } = {}) {
  const checks = [
    {
      name: 'module overview verifier',
      command: [NODE, 'scripts/verify-module-overviews.mjs'],
    },
    {
      name: 'admin menu health unit tests',
      command: [NODE, '--test', 'scripts/admin-menu-health.test.mjs'],
    },
    {
      name: 'admin menu static health',
      command: [NODE, 'scripts/admin-menu-health.mjs'],
    },
    {
      name: 'backend permission health (full scan)',
      command: [NODE, 'scripts/backend-permission-health.mjs'],
    },
    {
      name: 'mybatis mapper empty statement unit tests',
      command: [NODE, '--test', 'scripts/backend-mybatis-health.test.mjs'],
    },
    {
      name: 'mybatis mapper empty statement scan',
      command: [NODE, 'scripts/backend-mybatis-health.mjs'],
    },
    {
      name: 'finance sql health unit tests',
      command: [NODE, '--test', 'scripts/finance-sql-health.test.mjs'],
    },
    {
      name: 'list order health unit tests',
      command: [NODE, '--test', 'scripts/list-order-health.test.mjs'],
    },
    {
      name: 'expense verified edit health unit tests',
      command: [NODE, '--test', 'scripts/expense-verified-edit-health.test.mjs'],
    },
    {
      name: 'stock ledger health unit tests',
      command: [NODE, '--test', 'scripts/stock-ledger-health.test.mjs'],
    },
    {
      name: 'stock negative sale config health unit tests',
      command: [NODE, '--test', 'scripts/stock-negative-sale-config-health.test.mjs'],
    },
    {
      name: 'overview real data health unit tests',
      command: [NODE, '--test', 'scripts/overview-real-data-health.test.mjs'],
    },
    {
      name: 'overview real data static health',
      command: [NODE, 'scripts/overview-real-data-health.mjs'],
    },
    {
      name: 'store dashboard evolution health unit tests',
      command: [NODE, '--test', 'scripts/store-dashboard-evolution-health.test.mjs'],
    },
    {
      name: 'r9 closure health unit tests',
      command: [NODE, '--test', 'scripts/r9-closure-health.test.mjs'],
    },
    {
      name: 'r10 readiness health unit tests',
      command: [NODE, '--test', 'scripts/r10-readiness-health.test.mjs'],
    },
    {
      name: 'r10 config quality health unit tests',
      command: [NODE, '--test', 'scripts/r10-config-quality-health.test.mjs'],
    },
    {
      name: 'r11 readiness health unit tests',
      command: [NODE, '--test', 'scripts/r11-readiness-health.test.mjs'],
    },
    {
      name: 'r11 store health knowledge health unit tests',
      command: [NODE, '--test', 'scripts/r11-store-health-knowledge-health.test.mjs'],
    },
    {
      name: 'r12 action effect health unit tests',
      command: [NODE, '--test', 'scripts/r12-action-effect-health.test.mjs'],
    },
    {
      name: 'r13 receivable cashflow health unit tests',
      command: [NODE, '--test', 'scripts/r13-receivable-cashflow-health.test.mjs'],
    },
    {
      name: 'r14 receivable closure health unit tests',
      command: [NODE, '--test', 'scripts/r14-receivable-closure-health.test.mjs'],
    },
    {
      name: 'r15 receivable command center health unit tests',
      command: [NODE, '--test', 'scripts/r15-receivable-command-center-health.test.mjs'],
    },
    {
      name: 'r16 cashflow forecast health unit tests',
      command: [NODE, '--test', 'scripts/r16-cashflow-forecast-health.test.mjs'],
    },
    {
      name: 'R17 member growth action loop health',
      command: [NODE, '--test', 'scripts/r17-member-growth-action-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R18 R1-R18 closure health',
      command: [NODE, '--test', 'scripts/r18-r1-r18-closure-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R19 release governance health',
      command: [NODE, '--test', 'scripts/r19-release-governance-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R20 metrics governance health',
      command: [NODE, '--test', 'scripts/r20-metrics-governance-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R21 operations scheduler health',
      command: [NODE, '--test', 'scripts/r21-operations-scheduler-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R22 action center touch health',
      command: [NODE, '--test', 'scripts/r22-action-center-touch-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R23 open platform health',
      command: [NODE, '--test', 'scripts/r23-open-platform-health.test.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R23 openapi drift check',
      command: [NODE, 'scripts/r23-openapi-drift-check.mjs'],
      timeoutMs: 30_000,
    },
    {
      name: 'R24 predictive ops health',
      command: [NODE, '--test', 'scripts/r24-predictive-ops-health.test.mjs'],
      timeoutMs: 30_000,
    },
  ]

  if (includeDev) {
    checks.push({
      name: 'admin menu DEV database health',
      command: [NODE, 'scripts/admin-menu-health.mjs', '--dev'],
    })
  }

  return checks
}

function defaultRunCommand(check, { cwd = process.cwd(), inherit = false } = {}) {
  const [command, ...args] = check.command
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    stdio: inherit ? 'inherit' : 'pipe',
    shell: false,
  })

  return {
    status: result.status ?? 1,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  }
}

export function createAdminHealthReport({
  includeDev = false,
  cwd = process.cwd(),
  runCommand = (check) => defaultRunCommand(check, { cwd }),
} = {}) {
  const checks = createAdminHealthChecks({ includeDev })
  const report = {
    ok: true,
    mode: includeDev ? 'dev' : 'offline',
    checks: [],
  }

  for (const check of checks) {
    const result = runCommand(check)
    const checkResult = {
      name: check.name,
      command: check.command,
      status: result.status ?? 1,
      stdout: result.stdout || '',
      stderr: result.stderr || '',
    }

    report.checks.push(checkResult)

    if (checkResult.status !== 0) {
      report.ok = false
      report.failedCheck = check.name
      return report
    }
  }

  return report
}

export function runAdminHealthChecks({ includeDev = false, cwd = process.cwd() } = {}) {
  const checks = createAdminHealthChecks({ includeDev })
  const results = []

  for (const check of checks) {
    console.log(`\n[admin-health] ${check.name}`)
    const result = defaultRunCommand(check, { cwd, inherit: true })

    results.push({
      name: check.name,
      status: result.status,
    })

    if (result.status !== 0) {
      return {
        ok: false,
        results,
      }
    }
  }

  return { ok: true, results }
}

function isCliEntry() {
  return process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
}

if (isCliEntry()) {
  const includeDev = process.argv.includes('--dev')
  if (process.argv.includes('--json')) {
    const report = createAdminHealthReport({ includeDev })
    console.log(JSON.stringify(report, null, 2))
    if (!report.ok) {
      process.exit(1)
    }
    process.exit(0)
  }

  const result = runAdminHealthChecks({ includeDev })
  if (!result.ok) {
    process.exit(1)
  }
  console.log('\n[admin-health] all checks passed')
}
