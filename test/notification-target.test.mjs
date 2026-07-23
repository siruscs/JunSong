import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveNotificationTarget } from '../src/utils/notificationTarget.js'

const allCapabilities = { workflowTodo: true, workflowDone: true, expenseList: true }

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

test('fails closed for missing capabilities, malformed targets, and external or PC-only routes', () => {
  assert.equal(resolveNotificationTarget({ type: 'wf_todo', bizId: '12' }, {}), '')
  assert.equal(resolveNotificationTarget({ type: 'wf_todo' }, allCapabilities), '')
  assert.equal(resolveNotificationTarget({ linkUrl: 'https://example.com' }, allCapabilities), '')
  assert.equal(resolveNotificationTarget({ linkUrl: '/system/role' }, allCapabilities), '')
  assert.equal(resolveNotificationTarget({ linkUrl: '/finance/expense' }, { expenseList: false }), '')
})
