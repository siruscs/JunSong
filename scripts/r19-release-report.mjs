import { existsSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const NODE = process.execPath

const checks = [
  { name: 'r18 closure health', command: [NODE, '--test', 'scripts/r18-r1-r18-closure-health.test.mjs'] },
  { name: 'r19 release governance health', command: [NODE, '--test', 'scripts/r19-release-governance-health.test.mjs'] },
  { name: 'admin health unit tests', command: [NODE, '--test', 'scripts/admin-health.test.mjs'] },
  { name: 'backend permission health', command: [NODE, 'scripts/backend-permission-health.mjs'] },
  { name: 'backend mybatis health', command: [NODE, 'scripts/backend-mybatis-health.mjs'] },
]

const requiredFiles = [
  'docs/superpowers/plans/2026-07-03-r18-r1-r18-closure-execution-report.zh-CN.md',
  'docs/superpowers/plans/2026-07-03-r1-r18-release-inventory.zh-CN.md',
  'docs/superpowers/plans/2026-07-03-r1-r18-risk-and-backlog.zh-CN.md',
  'docs/superpowers/plans/2026-07-03-r19-release-governance-checklist.zh-CN.md',
  'docs/superpowers/plans/2026-07-03-r19-backlog-decision.zh-CN.md',
  'docs/superpowers/plans/2026-07-03-r19-release-governance-execution-report.zh-CN.md',
]

function run(check, { execute = false } = {}) {
  if (!execute) {
    return { name: check.name, command: check.command, status: 'not_run' }
  }
  const [cmd, ...args] = check.command
  const result = spawnSync(cmd, args, { encoding: 'utf8', stdio: 'pipe' })
  return {
    name: check.name,
    command: check.command,
    status: result.status ?? 1,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  }
}

export function createR19ReleaseReport({ execute = false } = {}) {
  const fileChecks = requiredFiles.map((file) => ({
    file,
    exists: existsSync(file),
  }))
  const commandChecks = checks.map((check) => run(check, { execute }))
  return {
    ok: fileChecks.every((item) => item.exists)
      && commandChecks.every((item) => item.status === 'not_run' || item.status === 0),
    generatedAt: new Date().toISOString(),
    mode: execute ? 'executed' : 'metadata',
    mainline: 'R19 release governance and post-closure stabilization',
    forbiddenScope: ['R20 metrics dictionary', 'R21 scheduler', 'R22 action center', 'R23 open platform', 'R24 predictive model', 'R25 hardening project'],
    files: fileChecks,
    checks: commandChecks,
  }
}

function isCliEntry() {
  return process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
}

if (isCliEntry()) {
  const execute = process.argv.includes('--execute')
  const report = createR19ReleaseReport({ execute })
  console.log(JSON.stringify(report, null, 2))
  if (!report.ok) {
    process.exit(1)
  }
}
