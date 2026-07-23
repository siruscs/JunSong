import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeDeptOptions, resolveCurrentDept } from '../src/utils/homeControl.js'

test('resolveCurrentDept does not fall back to stale first department for an explicit missing current id', () => {
  const depts = normalizeDeptOptions([
    { deptId: 10, deptName: '旧门店' },
    { deptId: 11, deptName: '备用门店' }
  ])

  assert.equal(resolveCurrentDept(depts, 99), null)
})

test('resolveCurrentDept still falls back to first department when current id is absent', () => {
  const depts = normalizeDeptOptions([
    { deptId: 10, deptName: '默认门店' }
  ])

  assert.equal(resolveCurrentDept(depts, null)?.name, '默认门店')
})
