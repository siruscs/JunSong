import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

function collapse(s) {
  return s.replace(/\s+/g, ' ').trim();
}

// 提取 <statement id="xxx"> ... </statement> 的语句体（支持 select/update/insert/delete/sql 标签）
function statementBody(xml, id) {
  const re = new RegExp(
    `<(?:select|update|insert|delete|sql)[^>]*id="${id}"[^>]*>([\\s\\S]*?)</(?:select|update|insert|delete|sql)>`,
    'i'
  );
  const m = xml.match(re);
  return m ? m[1] : null;
}

test('finance_stock_cost_layer.sql: 文件以 SET NAMES utf8mb4 开头', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

test('finance_stock_cost_layer.sql: 创建 fin_stock_cost_layer 表，租户+门店+商品唯一键', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /CREATE TABLE[\s\S]*?fin_stock_cost_layer/i);
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stock_cost_layer_tenant_dept_product\s*\(\s*tenant_id\s*,\s*dept_id\s*,\s*product_id\s*\)/i
  );
});

test('finance_stock_cost_layer.sql: 成本层表包含 tenant_id NOT NULL', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /tenant_id\s+BIGINT\s+NOT\s+NULL/i);
});

test('finance_stock_cost_layer.sql: 成本层表包含 avg_unit_cost（DECIMAL 6位小数）和 stock_amount（DECIMAL 2位）', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /avg_unit_cost\s+DECIMAL\s*\(\s*\d+\s*,\s*6\s*\)/i);
  assert.match(sql, /stock_amount\s+DECIMAL\s*\(\s*\d+\s*,\s*2\s*\)/i);
});

test('finance_stock_cost_layer.sql: 成本层表包含 version 乐观锁字段', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /version\s+INT/i);
});

test('finance_stock_cost_layer.sql: 创建 fin_stock_cost_ledger 成本流水表', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /CREATE TABLE[\s\S]*?fin_stock_cost_ledger/i);
});

test('finance_stock_cost_layer.sql: 成本流水表包含来源、原流水引用、数量、单位成本6位、金额2位、会计期间、调整原因、操作者、版本', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  const lower = sql.toLowerCase();
  // 来源类型
  assert.match(sql, /source_type\s+VARCHAR/i);
  // 原流水引用
  assert.match(sql, /source_ledger_id\s+BIGINT/i);
  // 成本流水类型
  assert.match(sql, /cost_change_type\s+VARCHAR/i);
  // 数量
  assert.match(sql, /quantity\s+INT/i);
  // 单位成本 6 位
  assert.match(sql, /unit_cost\s+DECIMAL\s*\(\s*\d+\s*,\s*6\s*\)/i);
  // 金额 2 位
  assert.match(sql, /amount\s+DECIMAL\s*\(\s*\d+\s*,\s*2\s*\)/i);
  // 会计期间
  assert.match(sql, /period_id\s+BIGINT/i);
  // 调整原因
  assert.match(sql, /adjust_reason\s+VARCHAR/i);
  // 操作者
  assert.match(sql, /operator\s+VARCHAR/i);
});

test('finance_stock_cost_layer.sql: 成本流水表包含 tenant_id NOT NULL 和 del_flag', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  // fin_stock_cost_ledger 部分必须包含 tenant_id NOT NULL
  const ledgerSection = sql.split(/CREATE TABLE[\s\S]*?fin_stock_cost_ledger/i)[1] || '';
  assert.match(ledgerSection, /tenant_id\s+BIGINT\s+NOT\s+NULL/i);
  assert.match(ledgerSection, /del_flag\s+CHAR\s*\(\s*1\s*\)/i);
});

test('finance_stock_cost_layer.sql: 不修改或覆盖第一期 fin_stock_ledger / fin_stock_position / fin_stock_snapshot 表结构', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  // 不允许 ALTER 修改第一期三张表的结构
  assert.doesNotMatch(
    sql,
    /ALTER\s+TABLE\s+fin_stock_(ledger|position|snapshot)\s+(ADD|DROP|MODIFY|CHANGE)/i
  );
});

test('finance_stock_cost_layer.sql: 幂等可重复执行（CREATE TABLE IF NOT EXISTS）', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /CREATE TABLE IF NOT EXISTS\s+fin_stock_cost_layer/i);
  assert.match(sql, /CREATE TABLE IF NOT EXISTS\s+fin_stock_cost_ledger/i);
});

test('finance_stock_cost_layer.sql: 包含对账输出（成本层与库存结存数量一致性检查）', () => {
  const sql = readSql('finance_stock_cost_layer.sql');
  assert.match(sql, /cost_layer_orphan_count|cost_layer_without_position|position_without_cost_layer/i);
});

// ── Mapper 契约测试 ──

function readMapperXml() {
  return readFileSync(
    join(
      repoRoot,
      'junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinStockCostLayerMapper.xml'
    ),
    'utf8'
  );
}

test('FinStockCostLayerMapper.xml: 存在且命名空间正确', () => {
  const xml = readMapperXml();
  assert.match(xml, /namespace="com\.junsong\.finance\.mapper\.FinStockCostLayerMapper"/);
});

test('FinStockCostLayerMapper.xml: selectCostLayerForUpdate 加行锁查询成本层', () => {
  const xml = readMapperXml();
  const body = statementBody(xml, 'selectCostLayerForUpdate');
  assert.ok(body, '必须存在 selectCostLayerForUpdate 语句');
  assert.match(collapse(body), /tenant_id\s*=\s*#\{tenantId\}/i);
  assert.match(collapse(body), /dept_id\s*=\s*#\{deptId\}/i);
  assert.match(collapse(body), /product_id\s*=\s*#\{productId\}/i);
  assert.match(collapse(body), /FOR UPDATE/i);
});

test('FinStockCostLayerMapper.xml: insertCostLayerIfAbsent 幂等创建成本层行', () => {
  const xml = readMapperXml();
  const body = statementBody(xml, 'insertCostLayerIfAbsent');
  assert.ok(body, '必须存在 insertCostLayerIfAbsent 语句');
  assert.match(collapse(body), /INSERT\s+IGNORE/i);
  assert.match(collapse(body), /fin_stock_cost_layer/i);
});

test('FinStockCostLayerMapper.xml: updateCostLayer 更新平均成本和金额并校验 version', () => {
  const xml = readMapperXml();
  const body = statementBody(xml, 'updateCostLayer');
  assert.ok(body, '必须存在 updateCostLayer 语句');
  assert.match(collapse(body), /avg_unit_cost\s*=/i);
  assert.match(collapse(body), /stock_amount\s*=/i);
  assert.match(collapse(body), /version\s*=\s*version\s*\+\s*1/i);
  assert.match(collapse(body), /version\s*=\s*#\{version\}/i);
  assert.match(collapse(body), /tenant_id\s*=\s*#\{tenantId\}/i);
  assert.match(collapse(body), /dept_id\s*=\s*#\{deptId\}/i);
  assert.match(collapse(body), /product_id\s*=\s*#\{productId\}/i);
});

test('FinStockCostLayerMapper.xml: insertCostLedger 写入成本流水', () => {
  const xml = readMapperXml();
  const body = statementBody(xml, 'insertCostLedger');
  assert.ok(body, '必须存在 insertCostLedger 语句');
  assert.match(collapse(body), /INSERT\s+INTO\s+fin_stock_cost_ledger/i);
  assert.match(collapse(body), /tenant_id/i);
  assert.match(collapse(body), /source_ledger_id/i);
  assert.match(collapse(body), /cost_change_type/i);
  assert.match(collapse(body), /unit_cost/i);
  assert.match(collapse(body), /amount/i);
  assert.match(collapse(body), /period_id/i);
});

test('FinStockCostLayerMapper.xml: 所有写入语句包含 tenant_id，禁止跨租户', () => {
  const xml = readMapperXml();
  const updateBody = statementBody(xml, 'updateCostLayer');
  const insertLedgerBody = statementBody(xml, 'insertCostLedger');
  assert.ok(updateBody && insertLedgerBody);
  assert.match(collapse(updateBody), /tenant_id\s*=\s*#\{tenantId\}/i);
  assert.match(collapse(insertLedgerBody), /tenant_id/i);
});

test('FinStockCostLayerMapper.xml: 成本层查询/更新不含 DELETE 语句', () => {
  const xml = readMapperXml().toLowerCase();
  assert.ok(!xml.includes('<delete'), '成本层 Mapper 不得包含 DELETE 语句');
});
