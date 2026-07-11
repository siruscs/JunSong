import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R9 review task log foundation exists', () => {
  const sql = read('sql/finance_review_task_log.sql').toLowerCase()
  assert.match(sql, /create table if not exists finance_review_task_log/)
  assert.match(sql, /before_status/)
  assert.match(sql, /after_status/)
  assert.match(sql, /handler_note/)
})

test('R9 governance task log foundation exists', () => {
  const sql = read('sql/sys_governance_task_log.sql').toLowerCase()
  assert.match(sql, /create table if not exists sys_governance_task_log/)
  assert.match(sql, /task_type/)
  assert.match(sql, /action_type/)
})

test('R9 member sale link foundation exists', () => {
  const sql = read('sql/finance_sale_member_link.sql').toLowerCase()
  assert.match(sql, /member_id/)
  assert.match(sql, /member_no/)
  assert.match(sql, /member_name/)
  assert.match(sql, /idx_fin_sale_member/)
})
