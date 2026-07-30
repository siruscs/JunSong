#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(new URL('..', import.meta.url).pathname)
const reportPath = process.env.PHASE1_PREFLIGHT_REPORT || path.join(root, 'artifacts', 'phase1-preflight.json')

function commandResult(command, args, cwd = root) {
  const result = spawnSync(command, args, { cwd, encoding: 'utf8', maxBuffer: 20 * 1024 * 1024 })
  return {
    status: result.status === 0 ? 'PASS' : 'FAIL',
    command: [command, ...args].join(' '),
    exitCode: result.status,
    output: `${result.stdout || ''}${result.stderr || ''}`.slice(-12000),
  }
}

const gates = [
  { id: 'workspace', run: () => commandResult('git', ['status', '--short']) },
  { id: 'backendCompile', run: () => commandResult('mvn', ['-pl', 'junsong-modules/junsong-workflow', '-am', '-DskipTests', 'compile']) },
  { id: 'frontendBuild', run: () => commandResult('npm', ['run', 'build'], path.join(root, 'junsong-ui-v3')) },
  { id: 'contractTests', run: () => commandResult('node', ['--test', 'scripts/workflow-closed-loop-contract.test.mjs', 'scripts/lowcode-runtime-contract.test.mjs']) },
  { id: 'workflowRegression', run: () => commandResult('mvn', ['-pl', 'junsong-modules/junsong-workflow', '-am', '-Dtest=WorkflowTaskServiceTest,WorkflowInstanceServiceTest,TaskAuthorizationServiceTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test']) },
  { id: 'concurrencyAndPermissionContracts', run: () => commandResult('node', ['--test', 'scripts/frontend-submit-lock-concurrency.test.mjs', 'scripts/frontend-idempotency-integration.test.mjs']) },
  { id: 'databaseReconciliation', run: () => ({ status: 'BLOCKED', reason: '未提供 DEV 数据库连接和只读授权；不得伪造通过' }) },
  { id: 'adminWjsChain', run: () => ({ status: 'BLOCKED', reason: '未执行真实 ADMIN/WJS 链路；不得以静态测试替代' }) },
]

const results = gates.map((gate) => ({ id: gate.id, ...gate.run() }))
const report = { generatedAt: new Date().toISOString(), root, results }
fs.mkdirSync(path.dirname(reportPath), { recursive: true })
fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`)
console.log(JSON.stringify(report, null, 2))
process.exitCode = results.some((item) => item.status === 'FAIL' || item.status === 'BLOCKED') ? 1 : 0
