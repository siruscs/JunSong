import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const tables = ['open_app', 'open_app_secret', 'open_isv', 'open_contract']

test('open core schema repair creates the four missing tenant tables safely', () => {
  const sql = readFileSync('sql/prod_open_core_schema_repair.sql', 'utf8')
  assert.match(sql, /^SET NAMES utf8mb4;/)
  for (const table of tables) {
    assert.match(sql, new RegExp('CREATE TABLE IF NOT EXISTS `' + table + '`'))
  }
  assert.equal((sql.match(/`tenant_id` BIGINT NOT NULL DEFAULT 1/g) || []).length, 4)
  assert.match(sql, /uk_open_app_key.*`app_key`/)
  assert.match(sql, /uk_tenant_contract_no.*`tenant_id`,`contract_no`/)
  assert.match(sql, /v_table_count <> 4/)
  assert.match(sql, /v_tenant_count <> 4/)
  assert.match(sql, /v_column_count <> 61/)
  assert.match(sql, /v_tenant_shape_count <> 4/)
  assert.match(sql, /v_app_key_unique_count <> 1/)
  assert.match(sql, /HAVING COUNT\(\*\)=1.*MAX\(SEQ_IN_INDEX\)=1.*MIN\(COLUMN_NAME\)='app_key'/s)
  assert.match(sql, /v_existing NOT IN \(0,4\)/)
  assert.doesNotMatch(sql, /DROP\s+TABLE|TRUNCATE|DELETE\s+FROM/i)
})
