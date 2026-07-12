import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const sql = readFileSync('sql/prod_workflow_lowcode_schema_repair.sql', 'utf8')
const tables = [
  'wf_node_field_permission', 'wf_node_timeout', 'wf_timeout_trigger_log',
  'wf_task_addsign', 'wf_task_attachment', 'wf_task_cc',
  'lc_biz_object', 'lc_biz_field', 'lc_biz_page_schema', 'lc_biz_node_assignee',
  'lc_biz_branch_rule', 'lc_biz_instance', 'lc_biz_config_snapshot',
  'lc_biz_node_timer', 'lc_biz_template', 'lc_biz_action', 'lc_biz_post_action'
]

test('migration creates every tenant-aware extension table without destructive DDL', () => {
  assert.match(sql, /^SET NAMES utf8mb4;/)
  for (const table of tables) {
    assert.match(sql, new RegExp('CREATE TABLE IF NOT EXISTS `' + table + '`'))
  }
  assert.equal((sql.match(/`tenant_id` BIGINT NOT NULL DEFAULT 1/g) || []).length, tables.length)
  assert.doesNotMatch(sql, /DROP\s+TABLE/i)
  assert.doesNotMatch(sql, /TRUNCATE/i)
  assert.match(sql, /information_schema\.TABLES/)
  assert.match(sql, /information_schema\.COLUMNS/)
  assert.match(sql, /v_table_count <> 17/)
  assert.match(sql, /v_tenant_column_count <> 17/)
  assert.match(sql, /workflow required column reconciliation failed/)
  assert.match(sql, /workflow tenant unique index reconciliation failed/)
  assert.match(sql, /SEQ_IN_INDEX=1 AND s\.COLUMN_NAME='tenant_id'/)
  assert.match(sql, /SIGNAL SQLSTATE '45000'/)
})
