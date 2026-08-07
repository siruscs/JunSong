import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('finance business codes are unique within tenant and institution', () => {
  const sql = fs.readFileSync('sql/finance_business_code_scope_unique_indexes.sql', 'utf8')
  assert.match(sql, /uk_product_code_scope[\s\S]*tenant_id, dept_id, product_code/)
  assert.match(sql, /uk_supplier_code_scope[\s\S]*tenant_id, dept_id, supplier_code/)
})

test('level sync loads tenant-scoped department levels with global fallback', () => {
  const source = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberConfigSyncServiceImpl.java', 'utf8')
  assert.match(source, /loadLevelSources\(tenantId, sourceDeptId\)/)
  assert.match(source, /tenant_id = \?/)
  assert.match(source, /dept_id = \? or dept_id = 0/)
  assert.match(source, /not exists/i)
})

test('campaign policy source lookup binds tenant, policy and department in order', () => {
  const source = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberConfigSyncServiceImpl.java', 'utf8')
  assert.match(source, /tenantScoped \? "p\.tenant_id=\? and " : ""/)
  assert.match(source, /args\[0\], args\[1\], args\[2\]/)
  assert.match(source, /mem_campaign_policy_package[\s\S]*args\[2\]/)
})

test('config sync snapshots support JDBC Java time values', () => {
  const source = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberConfigSyncServiceImpl.java', 'utf8')
  assert.match(source, /findAndRegisterModules\(\)/)
})

test('member level codes are unique within tenant and institution', () => {
  const sql = fs.readFileSync('sql/member_level_config_scope.sql', 'utf8')
  assert.match(sql, /DROP INDEX.*uk_tenant_type_code/i)
  assert.match(sql, /@has_global_unique\s*>\s*0/)
  assert.match(sql, /UNIQUE KEY.*uk_tenant_dept_type_code.*tenant_id, dept_id, type_code/i)
})

test('campaign policy codes are unique within tenant and institution', () => {
  const sql = fs.readFileSync('sql/member_campaign_policy_dept_unique.sql', 'utf8')
  assert.match(sql, /DROP INDEX uk_mem_campaign_policy_no/i)
  assert.match(sql, /uk_mem_campaign_policy_dept_no \(tenant_id, dept_id, policy_no, version\)/i)
})
