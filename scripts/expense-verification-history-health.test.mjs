import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const migrationPath = 'sql/finance_expense_verification_history_migration.sql'

test('history migration exists and uses deterministic, repeatable batch identities', () => {
  assert.equal(fs.existsSync(migrationPath), true, `${migrationPath} must exist`)
  const sql = fs.readFileSync(migrationPath, 'utf8')
  assert.match(sql, /CONCAT\('HXL-',\s*LEFT\(SHA2\(CONCAT_WS\(':',\s*e\.tenant_id,\s*e\.dept_id,\s*e\.expense_id\),\s*256\),\s*48\)\)/i)
  assert.match(sql, /CONCAT\('HXR-',\s*LEFT\(SHA2\(CONCAT_WS\(':',\s*e\.tenant_id,\s*e\.dept_id,\s*e\.expense_id\),\s*256\),\s*48\)\)/i)
  assert.equal('HXL-'.length + 48 <= 64, true)
  assert.equal('HXR-'.length + 48 <= 64, true)
  assert.doesNotMatch(sql, /CONCAT\('HISTORY-LEGACY-'[^\n]*e\.expense_id/i)
  assert.doesNotMatch(sql, /UUID\s*\(|RAND\s*\(/i)
  assert.match(sql, /NOT\s+EXISTS\s*\(/i)
  assert.match(sql, /tenant_id\s*=\s*e\.tenant_id/i)
  assert.match(sql, /dept_id\s*=\s*e\.dept_id/i)
})

test('historical migration never infers a NORMAL batch or advance relationship', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8')
  assert.doesNotMatch(sql, /source_type[^;]*'NORMAL'/i)
  assert.doesNotMatch(sql, /INSERT\s+INTO\s+fin_advance_verify_detail/i)
  const withoutTemporaryCleanup = sql.replace(/DROP\s+TEMPORARY\s+TABLE(?:\s+IF\s+EXISTS)?[^;]*;/gi, '')
  assert.doesNotMatch(withoutTemporaryCleanup, /\b(?:UPDATE|DELETE|TRUNCATE|DROP)\b/i)
})

test('every unresolved verified expense is isolated in a LEGACY batch', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8')
  assert.match(sql, /source_type[^\n]*'LEGACY'/i)
  assert.match(sql, /e\.status\s*=\s*'1'/i)
  assert.match(sql, /CONCAT\('HXR-'[^\n]*SHA2\(CONCAT_WS\(':',[^\n]*e\.expense_id/i)
  assert.match(sql, /'1'\s*,\s*e\.advance_id\s*,\s*e\.period_id/i)
  assert.match(sql, /b\.source_type\s*=\s*'LEGACY'/i)
  assert.match(sql, /b\.batch_no\s*=\s*CONCAT\('HXL-'/i)
  assert.match(sql, /b\.status\s*=\s*'VERIFIED'/i)
})

test('malformed identity collisions and orphan details remain visible as exceptions', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8')
  assert.match(sql, /HISTORY_MIGRATION_EXCEPTION/i)
  assert.match(sql, /NOT\s+EXISTS\s*\([^;]*FROM\s+fin_expense_verify_batch\s+b\s+JOIN\s+fin_expense_verify_detail\s+d/is)
  assert.match(sql, /d\.batch_id\s*=\s*b\.batch_id/i)
  assert.match(sql, /d\.tenant_id\s*=\s*b\.tenant_id/i)
  assert.match(sql, /d\.dept_id\s*=\s*b\.dept_id/i)
  assert.match(sql, /d\.expense_id\s*=\s*e\.expense_id/i)
})

test('migration is transactional and idempotent only for its deterministic LEGACY identity', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8')
  assert.match(sql, /START\s+TRANSACTION\s*;/i)
  assert.match(sql, /COMMIT\s*;/i)
  assert.match(sql, /NOT\s+EXISTS\s*\([^;]*request_id\s*=\s*CONCAT\('HXR-'/is)
  assert.match(sql, /JOIN\s+fin_expense_verify_batch\s+b[^;]*b\.request_id\s*=\s*CONCAT\('HXR-'/is)
  assert.match(sql, /b\.tenant_id\s*=\s*e\.tenant_id/i)
  assert.match(sql, /b\.dept_id\s*=\s*e\.dept_id/i)
  assert.match(sql, /b\.source_type\s*=\s*'LEGACY'/i)
})

test('reconciliation is tenant and department scoped and reports unresolved exceptions', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8')
  assert.match(sql, /BEFORE_VERIFIED_EXPENSE_AMOUNT/i)
  assert.match(sql, /AFTER_VERIFIED_EXPENSE_AMOUNT/i)
  assert.match(sql, /BEFORE_VERIFIED_EXPENSE_COUNT/i)
  assert.match(sql, /AFTER_VERIFIED_EXPENSE_COUNT/i)
  assert.match(sql, /BEFORE_VERIFY_BATCH_COUNT/i)
  assert.match(sql, /AFTER_VERIFY_BATCH_COUNT/i)
  assert.match(sql, /BEFORE_VERIFY_DETAIL_COUNT/i)
  assert.match(sql, /AFTER_VERIFY_DETAIL_COUNT/i)
  assert.match(sql, /BEFORE_UNVERIFIED_ADVANCE_AMOUNT/i)
  assert.match(sql, /AFTER_UNVERIFIED_ADVANCE_AMOUNT/i)
  assert.match(sql, /BEFORE_ACTIVE_ADVANCE_AMOUNT/i)
  assert.match(sql, /AFTER_ACTIVE_ADVANCE_AMOUNT/i)
  assert.match(sql, /SUM\s*\(\s*e\.expense_amount\s*\)/i)
  assert.match(sql, /SUM\s*\(\s*a\.advance_amount\s*\)/i)
  assert.match(sql, /GROUP\s+BY\s+(?:e\.|b\.|d\.|a\.)?tenant_id\s*,\s*(?:e\.|b\.|d\.|a\.)?dept_id/i)
  assert.match(sql, /HISTORY_MIGRATION_EXCEPTION/i)
  assert.match(sql, /reconciliation_mismatch/i)
  assert.match(sql, /CREATE\s+TEMPORARY\s+TABLE/i)
  assert.match(sql, /reconciliation_delta/i)
  assert.match(sql, /before_verify_batch_count/i)
  assert.match(sql, /before_verify_detail_count/i)
  assert.match(sql, /verify_batch_reconciliation_delta/i)
  assert.match(sql, /verify_detail_reconciliation_delta/i)
  assert.match(sql, /unresolved_verified_expense/i)
  assert.match(sql, /FROM\s+fin_expense_verify_batch\s+b[^;]*JOIN\s+fin_expense_verify_detail\s+d/is)
})
