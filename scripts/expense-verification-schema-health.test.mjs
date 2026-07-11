import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const sql = fs.readFileSync('sql/finance_expense_verification_batch.sql', 'utf8')

test('verification schema has batch and detail integrity', () => {
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `fin_expense_verify_batch`/)
  assert.match(sql, /UNIQUE KEY `uk_verify_batch_request` \(`tenant_id`, `request_id`\)/)
  assert.match(sql, /UNIQUE KEY `uk_verify_reverse_request` \(`tenant_id`, `reverse_request_id`\)/)
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `fin_expense_verify_detail`/)
  assert.match(sql, /UNIQUE KEY `uk_verify_expense` \(`batch_id`, `expense_id`\)/)
  assert.match(sql, /`original_status` varchar\(16\)/)
  assert.match(sql, /`original_advance_id` bigint/)
  assert.match(sql, /`period_id` bigint/)
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `fin_advance_verify_detail`/)
  assert.match(sql, /`generated_flag` char\(1\)/)
  assert.match(sql, /`relation_type` varchar\(16\)/)
  assert.match(sql, /KEY `idx_verify_batch_dept_status` \(`tenant_id`, `dept_id`, `status`\)/)
  assert.match(sql, /KEY `idx_verify_expense_dept` \(`tenant_id`, `dept_id`\)/)
  assert.match(sql, /KEY `idx_verify_advance_dept` \(`tenant_id`, `dept_id`\)/)
})

test('verification and reversal use separate permissions', () => {
  assert.match(sql, /finance:expense:verify/)
  assert.match(sql, /finance:expense:unverify/)
  assert.match(sql, /UPDATE `sys_menu`[\s\S]+`perms` = 'finance:expense:verify'[\s\S]+`menu_name` = '费用核销'[\s\S]+`perms` = 'finance:expense:edit'[\s\S]+`parent_id` = @expense_menu_id/)
  assert.match(sql, /NOT EXISTS \([\s\S]+`perms` IN \('finance:expense:edit', 'finance:expense:verify'\)/)
  assert.doesNotMatch(sql, /MAX\(`menu_id`\)/)
  assert.match(sql, /DELETE rm[\s\S]+FROM `sys_role_menu` rm[\s\S]+JOIN `sys_role` r[\s\S]+JOIN `sys_menu` m[\s\S]+m\.`perms` = 'finance:expense:verify'[\s\S]+r\.`role_key` NOT IN \('finance', 'finance_staff', 'finance_manager'\)/)
  assert.match(sql, /r\.`role_key` IN \('finance', 'finance_staff', 'finance_manager'\)/)
  assert.match(sql, /r\.`role_key` IN \('finance_manager'\)/)
})
