import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.existsSync(path) ? fs.readFileSync(path, 'utf8') : ''

test('R25 deliverables exist', () => {
  for (const path of [
    'sql/r25_enterprise_hardening.sql',
    'scripts/r25-performance-baseline.mjs',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/EnterpriseHardeningController.java',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysOperationAuditServiceImpl.java',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysDataArchiveServiceImpl.java',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysOperationAlertServiceImpl.java',
    'junsong-ui-v3/src/views/system/hardening/index.vue'
  ]) {
    assert.ok(fs.existsSync(path), `missing ${path}`)
  }
})

test('R25 SQL defines audit archive alert and permissions', () => {
  const sql = read('sql/r25_enterprise_hardening.sql')
  for (const token of [
    'sys_operation_audit_snapshot',
    'before_snapshot',
    'after_snapshot',
    'sys_data_retention_policy',
    'sys_data_archive_run',
    'sys_operation_alert_rule',
    'sys_operation_alert_event',
    'system:hardening:view',
    'system:hardening:audit',
    'system:hardening:archive',
    'system:hardening:alert'
  ]) {
    assert.match(sql, new RegExp(token), `missing SQL token ${token}`)
  }
})

test('R25 sensitive data masking is explicit', () => {
  const corpus = [
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysOperationAuditServiceImpl.java'),
    read('junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/OpenAppController.java'),
    read('junsong-common/junsong-common-log/src/main/java/com/junsong/common/log/aspect/LogAspect.java')
  ].join('\n')
  for (const token of ['maskSensitive', 'webhookUrl', 'appSecret', 'idCard', 'mobile']) {
    assert.match(corpus, new RegExp(token), `missing masking token ${token}`)
  }
  assert.doesNotMatch(corpus, /System\.out\.println\((password|token|appSecret|webhookUrl)/)
})

test('R25 archive service defaults to dry-run and no physical delete', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysDataArchiveServiceImpl.java')
  assert.match(src, /previewArchive/)
  assert.match(src, /dryRun/)
  assert.doesNotMatch(src, /\bdelete\s+from\b/i, 'R25 archive service must not physically delete rows')
})

test('R25 alert events are deduplicated and do not auto-execute actions', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysOperationAlertServiceImpl.java')
  assert.match(src, /dedupKey/)
  assert.match(src, /OPEN|ACKED|RESOLVED/)
  assert.doesNotMatch(src, /executeAction|touchAction|adjustStock|updateMemberLevel/i)
})
