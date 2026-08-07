import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('PC config sync filters target periods by department and displays time range', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/components/ConfigSyncDialog/index.vue', 'utf8')
  assert.match(source, /filter\(.*deptId/)
  assert.match(source, /startTime/)
  assert.match(source, /endTime/)
})

test('mini-program config sync filters target periods by department and displays time range', () => {
  const source = fs.readFileSync('junsong-miniprogram/src/pages/config-sync/index.vue', 'utf8')
  assert.match(source, /filter\(.*deptId/)
  assert.match(source, /startTime/)
  assert.match(source, /endTime/)
})

test('config sync time ranges use the shared date-time formatter', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/components/ConfigSyncDialog/index.vue', 'utf8')
  assert.match(source, /formatDateTime\(period\.startTime\)/)
  assert.match(source, /formatDateTime\(period\.endTime\)/)
})
