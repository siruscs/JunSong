import { readFileSync } from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const sql = readFileSync('sql/stock_ledger_foundation.sql', 'utf8').replace(/\s+/g, ' ').toLowerCase();

test('stock ledger foundation creates ledger and snapshot tables', () => {
  assert.match(sql, /create table if not exists fin_stock_ledger/);
  assert.match(sql, /create table if not exists fin_stock_snapshot/);
});

test('stock ledger records before and after quantity for traceability', () => {
  assert.match(sql, /before_quantity/);
  assert.match(sql, /after_quantity/);
  assert.match(sql, /change_quantity/);
});

test('stock ledger has reference fields for business traceability', () => {
  assert.match(sql, /reference_type/);
  assert.match(sql, /reference_id/);
  assert.match(sql, /reference_no/);
});

test('stock snapshot has unique daily dept product constraint', () => {
  assert.match(sql, /unique key uk_stock_snapshot_date_dept_product\s*\(\s*snapshot_date,\s*dept_id,\s*product_id\s*\)/);
});

test('stock ledger has required indexes', () => {
  assert.match(sql, /key idx_stock_ledger_dept_product\s*\(/);
  assert.match(sql, /key idx_stock_ledger_type\s*\(/);
  assert.match(sql, /key idx_stock_ledger_create_time\s*\(/);
});

const positionSql = readFileSync('sql/stock_position.sql', 'utf8').replace(/\s+/g, ' ').toLowerCase();

test('stock position table provides concurrency-safe current stock row', () => {
  assert.match(positionSql, /create table if not exists fin_stock_position/);
  assert.match(positionSql, /unique key uk_stock_position_dept_product\s*\(\s*dept_id,\s*product_id\s*\)/);
  assert.match(positionSql, /quantity int/);
});