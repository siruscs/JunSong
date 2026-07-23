import test from 'node:test'
import assert from 'node:assert/strict'
import { inventorySql, scanSqlSource } from './security-sql-inventory.mjs'

test('SQL parser finds mapper substitutions and multiline DDL builders', () => {
  const mapper = scanSqlSource('FixtureMapper.xml', '<mapper><select>${params.dataScope}</select></mapper>')
  assert.equal(mapper[0].reviewStatus, 'ALLOWLIST_REQUIRED')

  const java = scanSqlSource('Generator.java', `
    StringBuilder sql = new StringBuilder("CREATE TABLE ");
    sql.append(tableName);
    sql.append(" COMMENT '").append(comment).append("'");
  `)
  assert.ok(java.some((row) => row.kind === 'JAVA_SQL_BUILDER'))
})

test('SQL inventory reports mapper substitutions separately from non-SQL placeholders', () => {
  const rows = inventorySql(process.cwd())
  assert.ok(rows.some((row) => row.kind === 'MAPPER_SUBSTITUTION'))
  assert.ok(rows.some((row) => row.kind === 'ANNOTATION_SQL_CONCAT' || row.kind === 'JAVA_SQL_CONCAT'))
  assert.ok(rows.every((row) => row.file && row.line > 0 && row.reviewStatus))
  assert.ok(rows.filter((row) => row.kind === 'MAPPER_SUBSTITUTION').every((row) => row.file.endsWith('.xml')))
})

test('known backend-generated dataScope substitutions remain explicit review items', () => {
  const rows = inventorySql(process.cwd())
  const scopes = rows.filter((row) => row.expression.includes('dataScope'))
  assert.ok(scopes.length > 0)
  assert.ok(scopes.every((row) => row.reviewStatus === 'ALLOWLIST_REQUIRED'))
})
