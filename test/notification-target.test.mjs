import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveNotificationTarget } from '../src/utils/notificationTarget.js'

const allCapabilities = { workflowTodo: true, workflowDone: true, expenseList: true, operatingTask: true }

test('routes actionable workflow notifications with encoded task identifiers', () => {
  assert.equal(resolveNotificationTarget({ type: 'wf_todo', bizId: 'task/a b' }, allCapabilities), '/pages/workflow/detail?taskId=task%2Fa%20b')
  assert.equal(resolveNotificationTarget({ type: 'wf_timeout_urge', linkUrl: '/workflow/task', bizId: '88' }, allCapabilities), '/pages/workflow/detail?taskId=88')
  assert.equal(resolveNotificationTarget({ type: 'wf_timeout_transfer', bizId: '99' }, allCapabilities), '/pages/workflow/detail?taskId=99')
})

test('routes completed workflow and pending expense links to supported mini-program pages', () => {
  assert.equal(resolveNotificationTarget({ type: 'wf_finished', bizId: 'p1' }, allCapabilities), '/pages/workflow/todo?tab=done')
  assert.equal(resolveNotificationTarget({ type: 'wf_rejected', linkUrl: '/workflow/instance' }, allCapabilities), '/pages/workflow/todo?tab=done')
  assert.equal(resolveNotificationTarget({ type: 'finance_alert', linkUrl: '/finance/expense', bizId: '3' }, allCapabilities), '/pages/list/index?module=expense')
})

test('routes operating task notifications to operating-task page', () => {
  assert.equal(resolveNotificationTarget({ type: 'operating_task', bizId: '1024' }, allCapabilities), '/pages/operating-task/index?taskId=1024')
  assert.equal(resolveNotificationTarget({ type: 'operating_task_overdue', bizId: '2048' }, allCapabilities), '/pages/operating-task/index?taskId=2048')
  assert.equal(resolveNotificationTarget({ type: 'operating_task', linkUrl: '/finance/reviewTask' }, allCapabilities), '/pages/operating-task/index')
  assert.equal(resolveNotificationTarget({ linkUrl: '/finance/receivable', bizId: '55' }, allCapabilities), '/pages/operating-task/index?taskId=55')
})

test('does not misidentify operating task as workflow task', () => {
  // operating_task 类型不应跳转到 workflow/detail
  const target = resolveNotificationTarget({ type: 'operating_task', bizId: '100' }, allCapabilities)
  assert.ok(!target.includes('/pages/workflow/'), 'operating_task should not route to workflow page')
  assert.ok(target.includes('/pages/operating-task/'), 'operating_task should route to operating-task page')
})

test('fails closed for missing capabilities, malformed targets, and external or PC-only routes', () => {
  assert.equal(resolveNotificationTarget({ type: 'wf_todo', bizId: '12' }, {}), '')
  assert.equal(resolveNotificationTarget({ type: 'wf_todo' }, allCapabilities), '')
  assert.equal(resolveNotificationTarget({ linkUrl: 'https://example.com' }, allCapabilities), '')
  assert.equal(resolveNotificationTarget({ linkUrl: '/system/role' }, allCapabilities), '')
  assert.equal(resolveNotificationTarget({ linkUrl: '/finance/expense' }, { expenseList: false }), '')
  assert.equal(resolveNotificationTarget({ type: 'operating_task', bizId: '1' }, { operatingTask: false }), '')
})
