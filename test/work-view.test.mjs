import test from 'node:test'
import assert from 'node:assert/strict'
import { deriveWorkView, prioritizeModuleKeys } from '../src/utils/workView.js'

test('defaults a single-department store operator to store view', () => {
  assert.equal(deriveWorkView({ depts: [{ deptId: 202 }], modules: ['member', 'sale'] }).key, 'store')
})

test('uses management view for cross-department or management capabilities', () => {
  assert.equal(deriveWorkView({ depts: [{ deptId: 202 }, { deptId: 220 }], modules: ['sale'] }).key, 'management')
  assert.equal(deriveWorkView({ depts: [{ deptId: 202 }], modules: ['expense', 'accountingPeriod'] }).key, 'management')
})

test('prioritizes store and management modules without adding unauthorized keys', () => {
  const authorized = ['member', 'sale', 'expense', 'wfTodo']
  assert.deepEqual(prioritizeModuleKeys(authorized, 'store').slice(0, 3), ['member', 'sale', 'expense'])
  assert.deepEqual(prioritizeModuleKeys(authorized, 'management').slice(0, 2), ['wfTodo', 'expense'])
  assert.deepEqual(new Set(prioritizeModuleKeys(authorized, 'management')), new Set(authorized))
})
