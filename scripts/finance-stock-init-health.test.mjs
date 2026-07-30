import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');
const financeModule = join(repoRoot, 'junsong-modules', 'junsong-finance');
const pcUi = join(repoRoot, 'junsong-ui-v3');

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

function readMapperXml(name) {
  return readFileSync(
    join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', name),
    'utf8'
  );
}

function readService(name) {
  return readFileSync(
    join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'impl', name),
    'utf8'
  );
}

function readVo(name) {
  return readFileSync(
    join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'vo', name),
    'utf8'
  );
}

function readUi(rel) {
  return readFileSync(join(pcUi, rel), 'utf8');
}

function collapse(s) {
  return s.replace(/\s+/g, ' ').trim();
}

// 提取 <statement id="xxx"> ... </statement> 的语句体
function statementBody(xml, id) {
  const re = new RegExp(
    `<(?:select|update|insert|delete|sql)[^>]*id="${id}"[^>]*>([\\s\\S]*?)</(?:select|update|insert|delete|sql)>`,
    'i'
  );
  const m = xml.match(re);
  return m ? m[1] : null;
}

// ============================================================================
// SQL 契约测试：finance_stock_init.sql
// ============================================================================

test('finance_stock_init.sql: 文件以 SET NAMES utf8mb4 开头', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

test('finance_stock_init.sql: 创建 fin_stock_init_batch 批次头表（CREATE TABLE IF NOT EXISTS）', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /CREATE TABLE IF NOT EXISTS\s+fin_stock_init_batch/i);
});

test('finance_stock_init.sql: 创建 fin_stock_init_item 批次行表（CREATE TABLE IF NOT EXISTS）', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /CREATE TABLE IF NOT EXISTS\s+fin_stock_init_item/i);
});

test('finance_stock_init.sql: 两张表均包含 tenant_id BIGINT NOT NULL', () => {
  const sql = readSql('finance_stock_init.sql');
  const batchSection = sql.split(/CREATE TABLE IF NOT EXISTS\s+fin_stock_init_batch/i)[1] || '';
  const itemSection = sql.split(/CREATE TABLE IF NOT EXISTS\s+fin_stock_init_item/i)[1] || '';
  assert.match(batchSection, /tenant_id\s+BIGINT\s+NOT\s+NULL/i);
  assert.match(itemSection, /tenant_id\s+BIGINT\s+NOT\s+NULL/i);
});

test('finance_stock_init.sql: 头表包含 uk_stock_init_tenant_no (tenant_id, batch_no) 唯一键', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stock_init_tenant_no\s*\(\s*tenant_id\s*,\s*batch_no\s*\)/i
  );
});

test('finance_stock_init.sql: 头表包含 uk_stock_init_post_key (tenant_id, post_idempotency_key) 唯一键', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stock_init_post_key\s*\(\s*tenant_id\s*,\s*post_idempotency_key\s*\)/i
  );
});

test('finance_stock_init.sql: 行表包含 uk_stock_init_item_batch_product (batch_id, product_id) 唯一键', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stock_init_item_batch_product\s*\(\s*batch_id\s*,\s*product_id\s*\)/i
  );
});

test('finance_stock_init.sql: 行表包含 quantity DECIMAL(18,2)', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /quantity\s+DECIMAL\s*\(\s*18\s*,\s*2\s*\)/i);
});

test('finance_stock_init.sql: 行表包含 unit_cost DECIMAL(18,6)', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /unit_cost\s+DECIMAL\s*\(\s*18\s*,\s*6\s*\)/i);
});

test('finance_stock_init.sql: 行表包含 amount DECIMAL(18,2)', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /amount\s+DECIMAL\s*\(\s*18\s*,\s*2\s*\)/i);
});

test('finance_stock_init.sql: 两张表均包含 version INT NOT NULL DEFAULT 0', () => {
  const sql = readSql('finance_stock_init.sql');
  const batchSection = sql.split(/CREATE TABLE IF NOT EXISTS\s+fin_stock_init_batch/i)[1] || '';
  const itemSection = sql.split(/CREATE TABLE IF NOT EXISTS\s+fin_stock_init_item/i)[1] || '';
  assert.match(batchSection, /version\s+INT\s+NOT\s+NULL\s+DEFAULT\s+0/i);
  assert.match(itemSection, /version\s+INT\s+NOT\s+NULL\s+DEFAULT\s+0/i);
});

test('finance_stock_init.sql: 非破坏 —— 不得 DROP TABLE 而不带 IF EXISTS', () => {
  const sql = readSql('finance_stock_init.sql');
  // 允许 DROP TABLE IF EXISTS（回滚说明中），但不允许 DROP TABLE 不带 IF EXISTS
  assert.doesNotMatch(sql, /DROP\s+TABLE\s+(?!IF\s+EXISTS)/i);
});

test('finance_stock_init.sql: 包含对账 SELECT 输出', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /reconciliation_type/i);
});

test('finance_stock_init.sql: 包含 HEX 验证 SELECT', () => {
  const sql = readSql('finance_stock_init.sql');
  assert.match(sql, /HEX\s*\(\s*menu_name\s*\)/i);
});

// ============================================================================
// Mapper 契约测试：FinStockInitBatchMapper.xml
// ============================================================================

test('FinStockInitBatchMapper.xml: 存在且命名空间正确', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml');
  assert.match(xml, /namespace="com\.junsong\.finance\.mapper\.FinStockInitBatchMapper"/);
});

test('FinStockInitBatchMapper.xml: 所有 SELECT/UPDATE 语句包含 tenant_id = #{tenantId} 谓词', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml');
  const selectIds = ['selectBatchById', 'selectBatchForUpdate', 'selectBatchByPostIdempotencyKey',
                     'listBatches', 'countByBatchNo', 'countByPostIdempotencyKey',
                     'listBatchItems', 'selectBatchItemsForUpdate'];
  for (const id of selectIds) {
    const body = statementBody(xml, id);
    assert.ok(body, `必须存在 ${id} 语句`);
    assert.match(collapse(body), /tenant_id\s*=\s*#\{tenantId\}/i, `${id} 必须包含 tenant_id = #{tenantId}`);
  }
  const updateIds = ['updateBatchStatus', 'updateBatchItemPostingRefs'];
  for (const id of updateIds) {
    const body = statementBody(xml, id);
    assert.ok(body, `必须存在 ${id} 语句`);
    assert.match(collapse(body), /tenant_id\s*=\s*#\{tenantId\}/i, `${id} 必须包含 tenant_id = #{tenantId}`);
  }
});

test('FinStockInitBatchMapper.xml: selectBatchForUpdate 包含 FOR UPDATE 行锁', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml');
  const body = statementBody(xml, 'selectBatchForUpdate');
  assert.ok(body, '必须存在 selectBatchForUpdate 语句');
  assert.match(collapse(body), /FOR UPDATE/i);
});

test('FinStockInitBatchMapper.xml: selectBatchItemsForUpdate 包含 FOR UPDATE 和 ORDER BY dept_id, product_id', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml');
  const body = statementBody(xml, 'selectBatchItemsForUpdate');
  assert.ok(body, '必须存在 selectBatchItemsForUpdate 语句');
  assert.match(collapse(body), /FOR UPDATE/i);
  assert.match(collapse(body), /ORDER BY\s+dept_id\s*,\s*product_id/i);
});

test('FinStockInitBatchMapper.xml: updateBatchStatus 包含 version = version + 1 和 WHERE status = #{fromStatus} AND version = #{version}', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml');
  const body = statementBody(xml, 'updateBatchStatus');
  assert.ok(body, '必须存在 updateBatchStatus 语句');
  assert.match(collapse(body), /version\s*=\s*version\s*\+\s*1/i);
  assert.match(collapse(body), /status\s*=\s*#\{fromStatus\}/i);
  assert.match(collapse(body), /version\s*=\s*#\{version\}/i);
});

test('FinStockInitBatchMapper.xml: 不含物理 DELETE 语句', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml').toLowerCase();
  assert.ok(!xml.includes('<delete'), '期初库存 Mapper 不得包含 DELETE 语句');
});

test('FinStockInitBatchMapper.xml: insertBatch 使用 useGeneratedKeys', () => {
  const xml = readMapperXml('FinStockInitBatchMapper.xml');
  const insertMatch = xml.match(/<insert[^>]*id="insertBatch"[^>]*>/i);
  assert.ok(insertMatch, '必须存在 insertBatch 语句');
  assert.match(insertMatch[0], /useGeneratedKeys\s*=\s*"true"/i);
});

// ============================================================================
// Service 契约测试：FinStockInitServiceImpl.java
// ============================================================================

test('FinStockInitServiceImpl.java: 存在且标注 @Service', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.match(content, /@Service/i);
});

test('FinStockInitServiceImpl.java: 所有写方法标注 @Transactional(rollbackFor = Exception.class)', () => {
  const content = readService('FinStockInitServiceImpl.java');
  // 写方法：createStockInit / validateStockInit / submitStockInit / approveStockInit / postStockInit
  const writeMethods = ['createStockInit', 'validateStockInit', 'submitStockInit', 'approveStockInit', 'postStockInit'];
  for (const method of writeMethods) {
    // 定位方法签名，检查其前 300 字符内是否含 @Transactional(rollbackFor = Exception.class)
    const re = new RegExp(`public\\s+\\S+\\s+${method}\\s*\\(`);
    const m = content.match(re);
    assert.ok(m, `必须存在方法 ${method}`);
    const methodIdx = content.indexOf(m[0]);
    const preceding = content.substring(Math.max(0, methodIdx - 300), methodIdx);
    assert.match(
      preceding,
      /@Transactional\s*\(\s*rollbackFor\s*=\s*Exception\.class\s*\)/,
      `${method} 必须标注 @Transactional(rollbackFor = Exception.class)`
    );
  }
});

test('FinStockInitServiceImpl.java: 包含 TenantContext.getTenantId() 与 null 检查', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.match(content, /TenantContext\.getTenantId\(\)/);
  // null 检查：租户上下文缺失即拒绝
  assert.match(content, /tenantId\s*==\s*null|租户上下文缺失/i);
});

test('FinStockInitServiceImpl.java: 包含 assertDeptAuthorized 部门授权校验调用', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.match(content, /assertDeptAuthorized\s*\(/);
});

test('FinStockInitServiceImpl.java: 包含 post_idempotency_key 幂等校验逻辑', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.match(content, /PostIdempotencyKey|postIdempotencyKey/i);
  // 幂等键预检查：countByPostIdempotencyKey 或 selectBatchByPostIdempotencyKey
  assert.match(content, /countByPostIdempotencyKey|selectBatchByPostIdempotencyKey/i);
});

test('FinStockInitServiceImpl.java: 金额计算使用 BigDecimal 和 HALF_UP', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.match(content, /BigDecimal/i);
  assert.match(content, /HALF_UP/i);
});

test('FinStockInitServiceImpl.java: batchNo 服务端生成（SI 前缀）', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.match(content, /generateBatchNo\s*\(/i);
  assert.match(content, /"SI"\s*\+/);
});

test('FinStockInitServiceImpl.java: 不接受客户端传入 batchNo（不调用 request.getBatchNo）', () => {
  const content = readService('FinStockInitServiceImpl.java');
  assert.doesNotMatch(content, /request\.getBatchNo\s*\(/i);
});

test('StockInitCreateRequest.java: VO 不包含 batchNo 字段', () => {
  const voPath = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'vo', 'StockInitCreateRequest.java');
  if (!existsSync(voPath)) {
    assert.fail('StockInitCreateRequest.java 未创建');
  }
  const content = readFileSync(voPath, 'utf8');
  assert.doesNotMatch(content, /private\s+String\s+batchNo/i);
  assert.doesNotMatch(content, /getBatchNo|setBatchNo/i);
});

// ============================================================================
// PC API 契约测试：stockInit.ts
// ============================================================================

test('stockInit.ts: 导出 7 个核心 API 方法', () => {
  const content = readUi('src/api/finance/stockInit.ts');
  const required = ['createStockInit', 'listStockInit', 'getStockInitDetail',
                    'validateStockInit', 'submitStockInit', 'approveStockInit', 'postStockInit'];
  for (const fn of required) {
    assert.match(content, new RegExp(`export\\s+function\\s+${fn}\\s*\\(`), `必须导出 ${fn}`);
  }
});

test('stockInit.ts: StockInitCreateRequest 接口不包含 batchNo', () => {
  const content = readUi('src/api/finance/stockInit.ts');
  // 提取 StockInitCreateRequest 接口体
  const m = content.match(/export\s+interface\s+StockInitCreateRequest\s*\{([^}]*)\}/s);
  assert.ok(m, '必须存在 StockInitCreateRequest 接口');
  assert.doesNotMatch(m[1], /batchNo/i, 'StockInitCreateRequest 不得包含 batchNo 字段');
});

test('stockInit.ts: StockInitPostRequest 接口包含 postIdempotencyKey', () => {
  const content = readUi('src/api/finance/stockInit.ts');
  const m = content.match(/export\s+interface\s+StockInitPostRequest\s*\{([^}]*)\}/s);
  assert.ok(m, '必须存在 StockInitPostRequest 接口');
  assert.match(m[1], /postIdempotencyKey/i);
});

// ============================================================================
// PC 页面契约测试：stockInit/index.vue
// ============================================================================

test('stockInit/index.vue: batchNo 显示"系统生成"标签', () => {
  const content = readUi('src/views/finance/stockInit/index.vue');
  assert.match(content, /系统生成/);
});

test('stockInit/index.vue: 创建人显示"当前登录用户"标签', () => {
  const content = readUi('src/views/finance/stockInit/index.vue');
  assert.match(content, /当前登录用户/);
});

test('stockInit/index.vue: 门店选择使用 el-select（非 el-input）', () => {
  const content = readUi('src/views/finance/stockInit/index.vue');
  // 创建表单中的门店选择必须是 el-select
  assert.match(content, /<el-select[^>]*v-model="createForm\.deptId"/i);
});

test('stockInit/index.vue: 数量与单位成本使用 el-input-number', () => {
  const content = readUi('src/views/finance/stockInit/index.vue');
  // 数量列
  assert.match(content, /<el-input-number[^>]*v-model="scope\.row\.quantity"/i);
  // 单位成本列
  assert.match(content, /<el-input-number[^>]*v-model="scope\.row\.unitCost"/i);
});

test('stockInit/index.vue: 所有触发 API 的操作按钮均有 :loading 绑定', () => {
  const content = readUi('src/views/finance/stockInit/index.vue');
  // 验证关键操作按钮（validate/submit/post/export/create-dialog-submit/approve-dialog-submit）有 :loading
  const loadingCount = (content.match(/:loading=/g) || []).length;
  assert.ok(loadingCount >= 5, `操作按钮应有至少 5 处 :loading 绑定，实际 ${loadingCount}`);
  // 验证 validate/submit/post 按钮各有 :loading
  assert.match(content, /:loading="actionLoadingKey\s*===\s*`validate-/);
  assert.match(content, /:loading="actionLoadingKey\s*===\s*`submit-/);
  assert.match(content, /:loading="actionLoadingKey\s*===\s*`post-/);
  // 导出按钮有 :loading
  assert.match(content, /:loading="exportLoading"/);
  // 创建对话框确定按钮有 :loading
  assert.match(content, /:loading="createLoading"/);
});
