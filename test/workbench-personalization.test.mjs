import test from 'node:test'
import assert from 'node:assert/strict'

import {
  filterEntries,
  filterModuleGroups,
  recordRecent,
  sanitizeModuleKeys
} from '../src/utils/workbenchPersonalization.js'

const memberEntries = [
  { key: 'dashboard', title: '会员运营看板', desc: '会员增长与分层洞察' },
  { key: 'growth', title: '成长体系', desc: '等级、成长值与签到' },
  { key: 'actions', title: '增长动作', desc: '待执行与已完成动作' }
]

const groups = [
  {
    name: '财务管理',
    items: [
      { key: 'sale', title: '销售记录', desc: '销售登记和收款' },
      { key: 'expense', title: '费用记录', desc: '费用登记与核销' }
    ]
  },
  {
    name: 'Member Service',
    items: [
      { key: 'member', title: 'Member Profile', desc: 'Customer lookup' }
    ]
  }
]

test('searches title, description, and group name case-insensitively', () => {
  assert.deepEqual(filterModuleGroups(groups, '收款')[0].items.map((item) => item.key), ['sale'])
  assert.deepEqual(filterModuleGroups(groups, '费用')[0].items.map((item) => item.key), ['expense'])
  assert.equal(filterModuleGroups(groups, '财务').length, 1)
  assert.deepEqual(filterModuleGroups(groups, 'mEmBeR')[0].items.map((item) => item.key), ['member'])
  assert.equal(filterModuleGroups(groups, '不存在').length, 0)
})

test('filters custom entries by title and description and returns only matches', () => {
  assert.deepEqual(filterEntries(memberEntries, '成长').map((item) => item.key), ['growth'])
  assert.deepEqual(filterEntries(memberEntries, '已完成').map((item) => item.key), ['actions'])
  assert.deepEqual(filterEntries(memberEntries, '不存在'), [])
  assert.deepEqual(filterEntries(memberEntries, ''), memberEntries)
})

test('sanitize keeps only unique authorized keys in original order', () => {
  assert.deepEqual(
    sanitizeModuleKeys(['sale', 'hidden', 'sale', 'expense'], ['sale', 'expense']),
    ['sale', 'expense']
  )
})

test('recent keys are authorized, unique, newest first, and never exceed six', () => {
  const authorized = ['sale', 'expense', 'member', 'advance', 'purchase', 'stock', 'workflow']

  assert.deepEqual(
    recordRecent(['sale', 'expense', 'hidden'], 'sale', authorized),
    ['sale', 'expense']
  )
  assert.deepEqual(
    recordRecent(['sale', 'expense'], 'hidden', authorized),
    ['sale', 'expense']
  )
  assert.deepEqual(
    recordRecent(['sale', 'expense', 'member', 'advance', 'purchase', 'stock'], 'workflow', authorized, 99),
    ['workflow', 'sale', 'expense', 'member', 'advance', 'purchase']
  )
})
