import test from 'node:test'
import assert from 'node:assert/strict'
import { canRequestListScope, resolveListWorkScope, shouldRestoreListPage } from '../src/utils/listWorkScope.js'

test('projects the current department and a multiple-department scope label', () => {
  const scope = resolveListWorkScope({
    currentDeptId: 20,
    currentDept: { id: 20, name: '盛和里' },
    depts: [{ id: 10, name: '总部' }, { id: 20, name: '盛和里' }],
    version: 4
  }, 10)

  assert.deepEqual(scope, {
    currentDeptId: 20,
    currentDeptName: '盛和里',
    scopeLabel: '当前部门 · 共 2 个部门',
    contextVersion: 4,
    departmentChanged: true
  })
})

test('uses a single-department label and compares ids by normalized value', () => {
  const scope = resolveListWorkScope({
    currentDeptId: 7,
    currentDept: { deptId: 7, deptName: '东城店' },
    depts: [{ deptId: 7, deptName: '东城店' }],
    version: 2
  }, '7')

  assert.equal(scope.currentDeptName, '东城店')
  assert.equal(scope.scopeLabel, '当前数据范围')
  assert.equal(scope.departmentChanged, false)
})

test('fails closed when the context has no authorized current department', () => {
  const scope = resolveListWorkScope({ depts: [], version: 3 }, 9)
  assert.equal(scope.currentDeptId, null)
  assert.equal(scope.currentDeptName, '未选择部门')
  assert.equal(scope.scopeLabel, '暂无可用数据范围')
  assert.equal(scope.departmentChanged, true)
})

test('rejects an isolated current department outside the authorized collection', () => {
  const scope = resolveListWorkScope({
    currentDeptId: 99,
    currentDept: { id: 99, name: '已撤权门店' },
    depts: [{ id: 7, name: '东城店' }],
    version: 5
  }, 99)

  assert.equal(scope.currentDeptId, null)
  assert.equal(scope.currentDeptName, '未选择部门')
  assert.equal(canRequestListScope(scope.currentDeptId), false)
})

test('restores a failed pagination page only within the same work context', () => {
  assert.equal(shouldRestoreListPage(false, true), true)
  assert.equal(shouldRestoreListPage(false, false), false)
  assert.equal(shouldRestoreListPage(true, true), false)
})
