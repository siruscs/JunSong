import test from 'node:test'
import assert from 'node:assert/strict'

import * as workContextModule from '../src/utils/workContext.js'

const { createWorkContext } = workContextModule

const departments = [
  { deptId: 202, deptName: '盛和里' },
  { deptId: 220, deptName: '兴议家园店' }
]

test('stores the complete department collection and selected department', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })

  assert.equal(context.snapshot().depts.length, 2)
  assert.equal(context.snapshot().currentDept.name, '盛和里')
})

test('increments version when department changes and rejects stale responses', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })
  const oldVersion = context.captureVersion()

  context.selectDept(220)

  assert.equal(context.snapshot().currentDept.name, '兴议家园店')
  assert.equal(context.isCurrent(oldVersion), false)
  assert.equal(context.isCurrent(context.captureVersion()), true)
})

test('does not increment version when the same normalized user and department are hydrated again', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102, userName: 'wjs' }, depts: departments, currentDeptId: 202 })
  const version = context.captureVersion()

  context.hydrate({
    user: { userId: '102', userName: '更新后的用户名' },
    depts: [
      { deptId: 202, deptName: '盛和里（更新）' },
      { deptId: 220, deptName: '兴议家园店' },
      { deptId: 221, deptName: '新部门' }
    ],
    currentDeptId: '202'
  })

  const snapshot = context.snapshot()
  assert.equal(snapshot.version, version)
  assert.equal(snapshot.user.userName, '更新后的用户名')
  assert.equal(snapshot.depts.length, 3)
  assert.equal(snapshot.currentDept.name, '盛和里（更新）')
})

test('increments version when hydrate changes the normalized department', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })
  const version = context.captureVersion()

  context.hydrate({ user: { userId: '102' }, depts: departments, currentDeptId: '220' })

  assert.equal(context.captureVersion(), version + 1)
})

test('increments version when hydrate changes the normalized user identity', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })
  const version = context.captureVersion()

  context.hydrate({ user: { userId: 103 }, depts: departments, currentDeptId: 202 })

  assert.equal(context.captureVersion(), version + 1)
})

test('fails closed when selecting an unauthorized department', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })

  assert.throws(() => context.selectDept(999), /无权访问该部门/)
  assert.equal(context.snapshot().currentDeptId, 202)
})

test('keeps all 15 stored departments when server department arrays are empty', () => {
  assert.equal(typeof workContextModule.resolveDeptCollection, 'function')

  const storedDepts = Array.from({ length: 15 }, (_, index) => ({
    deptId: 201 + index,
    deptName: `部门${index + 1}`
  }))
  const result = workContextModule.resolveDeptCollection(
    { depts: [], user: { depts: [] } },
    { depts: storedDepts }
  )

  assert.equal(result.length, 15)
  assert.deepEqual(result, storedDepts)
})

test('persists a hydrated snapshot so a new work context restores all 15 departments', () => {
  assert.equal(typeof workContextModule.mergePersistedUser, 'function')

  const storedDepts = Array.from({ length: 15 }, (_, index) => ({
    deptId: 201 + index,
    deptName: `部门${index + 1}`
  }))
  const firstContext = createWorkContext()
  const snapshot = firstContext.hydrate({
    user: { userId: 102, userName: 'wjs' },
    depts: storedDepts,
    currentDeptId: 202
  })
  const persisted = workContextModule.mergePersistedUser(
    { modules: ['member'], permissions: ['member:list'] },
    { nickName: 'WJS' },
    snapshot
  )

  assert.equal(persisted.depts.length, 15)
  assert.equal(persisted.currentDeptId, 202)
  assert.equal(persisted.deptId, 202)
  assert.deepEqual(persisted.modules, ['member'])
  assert.deepEqual(persisted.permissions, ['member:list'])

  const restoredContext = createWorkContext()
  restoredContext.hydrate({
    user: persisted,
    depts: persisted.depts,
    currentDeptId: persisted.currentDeptId
  })

  assert.equal(restoredContext.snapshot().depts.length, 15)
  assert.equal(restoredContext.snapshot().currentDeptId, 202)
})

test('does not replace stored departments with an empty incoming collection', () => {
  assert.equal(typeof workContextModule.mergePersistedUser, 'function')
  const existing = { depts: departments, modules: ['finance'], permissions: ['finance:list'] }

  const persisted = workContextModule.mergePersistedUser(existing, {
    depts: [],
    modules: ['member'],
    permissions: ['member:list']
  })

  assert.deepEqual(persisted.depts, departments)
  assert.deepEqual(persisted.modules, ['member'])
  assert.deepEqual(persisted.permissions, ['member:list'])
})
