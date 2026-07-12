import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

// Task 1 契约测试：租户安全的库存表结构与幂等迁移。
// 断言迁移文件建立 tenant_id 隔离键、报表索引、来源对账键，并输出对账计数。
const sql = readFileSync('sql/finance_stock_report_foundation.sql', 'utf8')
const normalized = sql.replace(/\s+/g, ' ')

test('迁移文件以 SET NAMES utf8mb4 开始', () => {
  assert.match(sql, /^SET NAMES utf8mb4;/)
})

test('三张库存表都声明 tenant_id BIGINT NOT NULL', () => {
  // fin_stock_ledger / fin_stock_position / fin_stock_snapshot 均须租户非空
  const occurrences = normalized.match(/tenant_id BIGINT NOT NULL/gi) || []
  assert.ok(occurrences.length >= 3, `期望至少三处 tenant_id BIGINT NOT NULL，实际 ${occurrences.length}`)
})

test('position 唯一键为 (tenant_id, dept_id, product_id)', () => {
  assert.match(normalized, /UNIQUE KEY uk_stock_position_tenant_dept_product\s*\(tenant_id, dept_id, product_id\)/i)
})

test('snapshot 唯一键为 (tenant_id, snapshot_date, dept_id, product_id)', () => {
  assert.match(normalized, /UNIQUE KEY uk_stock_snapshot_tenant_date_dept_product\s*\(tenant_id, snapshot_date, dept_id, product_id\)/i)
})

test('流水表包含报表组合索引 (tenant_id, dept_id, product_id, create_time)', () => {
  assert.match(normalized, /KEY idx_stock_ledger_tenant_dept_product_time\s*\(tenant_id, dept_id, product_id, create_time\)/i)
})

test('流水表包含来源幂等对账索引 (tenant_id, dept_id, reference_type, reference_id, product_id)', () => {
  assert.match(normalized, /KEY idx_stock_ledger_tenant_reference\s*\(tenant_id, dept_id, reference_type, reference_id, product_id\)/i)
})

test('迁移仅回填能从来源单据唯一推导租户的流水，禁止用 tenant_id = 1 覆盖未知存量', () => {
  // 允许把 tenant 为 0/NULL 的行清理为待推导，但不得直接 SET tenant_id = 1 兜底
  assert.doesNotMatch(normalized, /SET tenant_id = 1/i)
  // 必须从采购/销售单推导租户
  assert.match(normalized, /fin_purchase/i)
  assert.match(normalized, /fin_sale_record/i)
})

test('发现无法推导或重复业务键时使用 SIGNAL 阻断迁移', () => {
  assert.match(normalized, /SIGNAL SQLSTATE '45000'/i)
})

test('输出零租户对账计数', () => {
  assert.match(normalized, /AS zero_tenant_count/i)
})

test('输出结存无流水对账计数', () => {
  assert.match(normalized, /AS position_without_ledger_count/i)
})

test('输出流水累计与结存差异对账计数', () => {
  assert.match(normalized, /AS ledger_position_mismatch_count/i)
})

test('输出重复业务键对账计数', () => {
  assert.match(normalized, /AS duplicate_position_key_count/i)
})

test('输出快照重复行对账计数', () => {
  assert.match(normalized, /AS duplicate_snapshot_key_count/i)
})

test('迁移非破坏：不得 DROP TABLE / TRUNCATE / DELETE FROM', () => {
  assert.doesNotMatch(sql, /DROP\s+TABLE|TRUNCATE|DELETE\s+FROM/i)
})

// 基础 DDL 也须在全新安装时直接建立租户安全键。
const ledgerDdl = readFileSync('sql/stock_ledger_foundation.sql', 'utf8').replace(/\s+/g, ' ')
const positionDdl = readFileSync('sql/stock_position.sql', 'utf8').replace(/\s+/g, ' ')

test('基础流水/快照 DDL 声明 tenant_id 并建立租户安全键', () => {
  assert.match(ledgerDdl, /tenant_id BIGINT NOT NULL/i)
  assert.match(ledgerDdl, /uk_stock_snapshot_tenant_date_dept_product\s*\(tenant_id, snapshot_date, dept_id, product_id\)/i)
})

test('基础结存 DDL 声明 tenant_id 并建立租户安全唯一键', () => {
  assert.match(positionDdl, /tenant_id BIGINT NOT NULL/i)
  assert.match(positionDdl, /uk_stock_position_tenant_dept_product\s*\(tenant_id, dept_id, product_id\)/i)
})
