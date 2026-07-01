import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export function createAdminHealthChecks({ includeDev = false } = {}) {
  const checks = [
    {
      name: 'module overview verifier',
      command: ['node', 'scripts/verify-module-overviews.mjs'],
    },
    {
      name: 'admin menu health unit tests',
      command: ['node', '--test', 'scripts/admin-menu-health.test.mjs'],
    },
    {
      name: 'admin menu static health',
      command: ['node', 'scripts/admin-menu-health.mjs'],
    },
    {
      name: 'backend permission health (full scan)',
      command: ['node', 'scripts/backend-permission-health.mjs'],
    },
    {
      name: 'mybatis mapper empty statement unit tests',
      command: ['node', '--test', 'scripts/backend-mybatis-health.test.mjs'],
    },
    {
      name: 'mybatis mapper empty statement scan',
      command: ['node', 'scripts/backend-mybatis-health.mjs'],
    },
    {
      name: 'finance sql health unit tests',
      command: ['node', '--test', 'scripts/finance-sql-health.test.mjs'],
    },
    {
      name: 'list order health unit tests',
      command: ['node', '--test', 'scripts/list-order-health.test.mjs'],
    },
  ]

  if (includeDev) {
    checks.push({
      name: 'admin menu DEV database health',
      command: ['node', 'scripts/admin-menu-health.mjs', '--dev'],
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
