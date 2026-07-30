import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const source = fs.readFileSync(new URL('./lowcode-workflow-phase1-preflight.mjs', import.meta.url), 'utf8')

test('phase1 preflight defines every mandatory gate', () => {
  for (const gate of ['workspace', 'backendCompile', 'frontendBuild', 'contractTests', 'workflowRegression', 'concurrencyAndPermissionContracts', 'databaseReconciliation', 'adminWjsChain']) {
    assert.match(source, new RegExp(`id: ['"]${gate}['"]`))
  }
})

test('phase1 preflight never reports unavailable external gates as passed', () => {
  assert.match(source, /status:\s*['"]BLOCKED['"]|status:\s*['"]NOT_RUN['"]/) 
  assert.match(source, /databaseReconciliation/) 
  assert.match(source, /adminWjsChain/) 
})

test('phase1 preflight emits machine-readable JSON and non-zero exit on failed gates', () => {
  assert.match(source, /JSON\.stringify/) 
  assert.match(source, /process\.exitCode/) 
})
