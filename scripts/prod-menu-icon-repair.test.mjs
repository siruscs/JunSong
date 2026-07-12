import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('menu icon repair is utf8, narrow, repeatable, and uses bundled icons', () => {
  const sql = readFileSync('sql/prod_menu_icon_repair.sql', 'utf8')
  assert.match(sql, /^SET NAMES utf8mb4;/)
  const expected = new Map([
    ['system:action-center:view', 'bell'],
    ['finance:predictiveOps:view', 'chart'],
    ['member:refund:list', 'money'],
    ['workflow:version:list', 'nested'],
    ['open:app:list', 'client']
  ])
  for (const [permission, icon] of expected) {
    assert.match(sql, new RegExp(permission.replaceAll(':', '\\:')))
    assert.match(sql, new RegExp("'" + icon + "'"))
  }
  assert.doesNotMatch(sql, /ROW_COUNT\(\)/)
  assert.match(sql, /target menu uniqueness reconciliation failed/)
  assert.match(sql, /target permission uniqueness reconciliation failed/)
  assert.match(sql, /menu icon result reconciliation failed/)
  assert.equal((sql.match(/UPDATE sys_menu SET icon=/g) || []).length, 5)
  assert.equal((sql.match(/AND menu_type='C'/g) || []).length, 5)
  assert.doesNotMatch(sql, /DELETE\s+FROM|TRUNCATE|DROP\s+TABLE/i)
  assert.doesNotMatch(sql, /SET\s+(parent_id|path|component|perms|visible|status)\s*=/i)
})
