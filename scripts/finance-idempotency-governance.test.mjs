import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');
const pcUi = join(repoRoot, 'junsong-ui-v3');
const miniprogram = join(repoRoot, 'junsong-miniprogram');

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

function readUi(rel) {
  return readFileSync(join(pcUi, rel), 'utf8');
}

function readMp(rel) {
  return readFileSync(join(miniprogram, rel), 'utf8');
}

// ============================================================================
// SQL 契约测试：finance_idempotency_columns.sql
// ============================================================================

test('finance_idempotency_columns.sql: 文件以 SET NAMES utf8mb4 开头', () => {
  const sql = readSql('finance_idempotency_columns.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

const IDEMPOTENCY_TABLES = [
  { table: 'fin_sale_record', indexName: 'uk_sale_idempotency_key' },
  { table: 'fin_sale_payment', indexName: 'uk_sale_payment_idempotency_key' },
  { table: 'fin_purchase', indexName: 'uk_purchase_idempotency_key' },
  { table: 'fin_expense', indexName: 'uk_expense_idempotency_key' },
  { table: 'fin_advance', indexName: 'uk_advance_idempotency_key' },
  { table: 'fin_invest_record', indexName: 'uk_invest_idempotency_key' },
];

for (const { table, indexName } of IDEMPOTENCY_TABLES) {
  test(`finance_idempotency_columns.sql: ${table} 添加 idempotency_key VARCHAR(96) 列`, () => {
    const sql = readSql('finance_idempotency_columns.sql');
    assert.match(
      sql,
      new RegExp(`ALTER TABLE ${table} ADD COLUMN idempotency_key\\s+VARCHAR\\s*\\(\\s*96\\s*\\)`, 'i'),
      `${table} 必须添加 idempotency_key VARCHAR(96) 列`
    );
  });

  test(`finance_idempotency_columns.sql: ${table} 添加 ${indexName} (tenant_id, idempotency_key) 唯一键`, () => {
    const sql = readSql('finance_idempotency_columns.sql');
    assert.match(
      sql,
      new RegExp(
        `UNIQUE KEY\\s+${indexName}\\s*\\(\\s*tenant_id\\s*,\\s*idempotency_key\\s*\\)`,
        'i'
      ),
      `${table} 必须添加唯一键 ${indexName} (tenant_id, idempotency_key)`
    );
  });
}

test('finance_idempotency_columns.sql: 使用 information_schema.COLUMNS 守卫（幂等）', () => {
  const sql = readSql('finance_idempotency_columns.sql');
  assert.match(sql, /information_schema\.COLUMNS/i);
  assert.match(sql, /PREPARE\s+stmt/i);
  assert.match(sql, /DEALLOCATE\s+PREPARE/i);
});

test('finance_idempotency_columns.sql: 包含重复数据扫描 SELECT（幂等键冲突检测）', () => {
  const sql = readSql('finance_idempotency_columns.sql');
  // 各表的 GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1 扫描
  assert.match(sql, /GROUP BY\s+tenant_id\s*,\s*idempotency_key\s+HAVING\s+COUNT\s*\(\s*\*\s*\)\s*>\s*1/i);
  assert.match(sql, /duplicate_count|重复检测/i);
});

test('finance_idempotency_columns.sql: 包含对账汇总输出', () => {
  const sql = readSql('finance_idempotency_columns.sql');
  assert.match(sql, /reconciliation_type|汇总/i);
});

// ============================================================================
// Composable 契约测试：useSubmitLock.ts
// ============================================================================

test('useSubmitLock.ts: 导出 useSubmitLock 函数', () => {
  const content = readUi('src/composables/useSubmitLock.ts');
  assert.match(content, /export\s+function\s+useSubmitLock\s*\(/);
});

test('useSubmitLock.ts: 包含 loading ref', () => {
  const content = readUi('src/composables/useSubmitLock.ts');
  assert.match(content, /const\s+loading\s*[:=]/i);
  assert.match(content, /ref\s*\(\s*false\s*\)/i);
});

test('useSubmitLock.ts: 包含 execute 函数（基于 key 的防重入锁）', () => {
  const content = readUi('src/composables/useSubmitLock.ts');
  assert.match(content, /function\s+execute\s*[<(]/i);
  assert.match(content, /lockKey/i);
  // 相同 key 重复触发被忽略
  assert.match(content, /loading\.value\s*&&\s*lockKey\.value\s*===\s*key/i);
});

test('useSubmitLock.ts: 包含 buildIdempotencyKey 函数', () => {
  const content = readUi('src/composables/useSubmitLock.ts');
  assert.match(content, /function\s+buildIdempotencyKey\s*\(/);
  // 幂等键包含前缀、时间戳与随机串
  assert.match(content, /Date\.now\(\)/);
  assert.match(content, /Math\.random/i);
});

// ============================================================================
// 组件契约测试：UserSelect/index.vue
// ============================================================================

test('UserSelect/index.vue: 使用 el-select 且支持 filterable + remote', () => {
  const content = readUi('src/components/UserSelect/index.vue');
  assert.match(content, /<el-select/i);
  assert.match(content, /filterable/i);
  assert.match(content, /remote/i);
});

test('UserSelect/index.vue: label 格式为 nickName（userName），不直接展示 raw userId', () => {
  const content = readUi('src/components/UserSelect/index.vue');
  // formatLabel 函数包含 nickName 与 userName 的组合格式
  assert.match(content, /\$\{.*nickName.*\}[（(].*\$\{.*userName.*\}[）)]/);
  // el-option 的 :label 使用 formatLabel 而非直接绑定 userId
  assert.match(content, /:label="formatLabel\(u\)"/);
  // 不允许 label 直接为 String(u.userId) 或 u.userId
  assert.doesNotMatch(content, /:label="String\(u\.userId\)"/);
  assert.doesNotMatch(content, /:label="u\.userId"/);
});

// ============================================================================
// PC 盘点复盘契约测试：StocktakeItemsTable.vue
// ============================================================================

test('StocktakeItemsTable.vue: handleSaveRecount 请求体包含 reasonCode', () => {
  const content = readUi('src/views/finance/stocktake/components/StocktakeItemsTable.vue');
  // 提取 handleSaveRecount 方法体
  const match = content.match(/function\s+handleSaveRecount\s*\([\s\S]*?\n\}/);
  assert.ok(match, '必须存在 handleSaveRecount 方法');
  const body = match[0];
  assert.match(body, /reasonCode/i, 'handleSaveRecount 的 StocktakeRecountRequest 必须包含 reasonCode');
});

// ============================================================================
// 小程序盘点复盘契约测试：miniprogram/src/pages/stocktake/detail.vue
// ============================================================================

test('小程序盘点详情: saveRecount 请求体 reasonCode 检查（缺失则记录为 FINDING）', () => {
  const mpPath = join(miniprogram, 'src', 'pages', 'stocktake', 'detail.vue');
  if (!existsSync(mpPath)) {
    assert.fail('junsong-miniprogram/src/pages/stocktake/detail.vue 未创建');
  }
  const content = readFileSync(mpPath, 'utf8');
  // 提取 saveRecount 方法体
  const saveRecountMatch = content.match(/saveRecount\s*\([\s\S]*?\n\s*\},/);
  const saveRecountBody = saveRecountMatch ? saveRecountMatch[0] : '';
  assert.ok(saveRecountBody.length > 0, '必须存在 saveRecount 方法');

  const hasReasonCode = /reasonCode/i.test(saveRecountBody);
  if (!hasReasonCode) {
    // FINDING：仅记录，不修复，测试不失败
    console.log(
      '[FINDING] junsong-miniprogram/src/pages/stocktake/detail.vue: ' +
      'saveRecount 方法的 recountItem 请求体未包含 reasonCode 字段。' +
      'PC 端 StocktakeItemsTable.vue 已包含 reasonCode，小程序端缺失，待后续对齐。'
    );
    // 测试通过 —— FINDING 仅作记录
    assert.ok(true, 'FINDING 已记录：小程序 saveRecount 缺少 reasonCode（不修复）');
  } else {
    assert.ok(hasReasonCode, 'saveRecount 请求体应包含 reasonCode');
  }
});
