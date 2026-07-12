import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const sql = readFileSync('sql/prod_workflow_lowcode_menu_config_repair.sql', 'utf8')

test('repair is UTF-8, stable, scoped, and repeatable', () => {
  assert.match(sql, /^SET NAMES utf8mb4;/)
  assert.match(sql, /2220[\s\S]*'工作流中心'/)
  assert.match(sql, /2280[\s\S]*'低代码平台'/)
  for (const component of [
    'workflow/definition/index', 'workflow/start/index', 'workflow/instance/index',
    'workflow/task/index', 'workflow/history/index', 'workflow/analytics/index',
    'workflow/timeout/index', 'workflow/version/index', 'workflow/field-permission/index',
    'workflow/intervene/index', 'workflow/monitor/index', 'lowcode/admin/index'
  ]) assert.ok(sql.includes(component), component)
  assert.ok(!sql.includes("'lowcode/SchemaList','C'"))
  assert.ok(!sql.includes("'report','','C'"))
  for (const permission of [
    'workflow:definition:list', 'workflow:instance:list', 'workflow:task:list',
    'workflow:history:list', 'workflow:fieldPermission:list',
    'lowcode:meta:list', 'lowcode:biz:list', 'lowcode:report:list', 'lowcode:report:stat'
  ]) assert.ok(sql.includes(permission), permission)
  assert.match(sql, /role_key\s*=\s*'admin'/)
  assert.match(sql, /INSERT IGNORE INTO sys_role_menu\(role_id,menu_id,tenant_id\)/)
  assert.match(sql, /DELETE rm FROM sys_role_menu rm/)
  assert.match(sql, /r\.role_key<>'admin'/)
  assert.doesNotMatch(sql, /DELETE\s+FROM\s+sys_menu/i)
  assert.doesNotMatch(sql, /MAX\s*\(\s*menu_id\s*\)/i)
  assert.match(sql, /m\.component <=> e\.component/)
  assert.match(sql, /m\.order_num <=> e\.order_num/)
  assert.match(sql, /m\.icon <=> e\.icon/)
  const keys = [
    'r22.touch.wework.enabled', 'r22.touch.wework.dryRun',
    'r22.touch.wework.webhookUrl', 'r22.touch.rateLimit.perTarget24h',
    'r23.openapi.dailyQuota.enabled', 'r23.openapi.dailyQuota.default'
  ]
  for (const key of keys) assert.ok(sql.includes(key), key)
  for (const id of [109, 110, 111, 112, 113, 114]) assert.match(sql, new RegExp(`config_id=${id}`))
  assert.match(sql, /tenant_id=1/)
  assert.match(sql, /HEX\((?:m\.)?menu_name\)/)
  assert.match(sql, /HEX\(config_name\)/)
  assert.match(sql, /INSERT IGNORE INTO sys_role_menu/)
})
