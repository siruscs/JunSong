import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');
const financeModule = join(repoRoot, 'junsong-modules', 'junsong-finance');
const pcUi = join(repoRoot, 'junsong-ui-v3');
const miniprogram = join(repoRoot, 'junsong-miniprogram');

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

function readMain(name) {
  return readFileSync(join(financeModule, 'src', 'main', name), 'utf8');
}

function readTest(name) {
  return readFileSync(join(financeModule, 'src', 'test', name), 'utf8');
}

function readUi(rel) {
  return readFileSync(join(pcUi, rel), 'utf8');
}

function readMp(rel) {
  return readFileSync(join(miniprogram, rel), 'utf8');
}

function fileExists(rel) {
  return existsSync(join(repoRoot, rel));
}

// ============================================================================
// Task 1: 失败验收契约 —— 以下断言在 Task 1 阶段应全部 FAIL
// 因为 schema、workflow 服务、UI、小程序页面均未创建。
// 随 Task 2-13 完成，这些断言将逐步 PASS。
// ============================================================================

// ---- Task 2: 三张新表与可重复迁移 ----

test('Task2: sql/finance_stocktake_closure.sql 存在且以 SET NAMES utf8mb4 开头', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

test('Task2: 创建 finance_stocktake 头表，含 tenant_id 与 (tenant_id, take_no) 唯一键', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /CREATE TABLE[\s\S]*?finance_stocktake/i);
  assert.match(sql, /tenant_id\s+BIGINT\s+NOT\s+NULL/i);
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stocktake_tenant_no\s*\(\s*tenant_id\s*,\s*take_no\s*\)/i
  );
});

test('Task2: finance_stocktake 头表包含 status 与 version 字段', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /status\s+VARCHAR\s*\(\s*24\s*\)/i);
  assert.match(sql, /version\s+INT\s+NOT\s+NULL\s+DEFAULT\s+0/i);
});

test('Task2: 创建 finance_stocktake_item 行表，含 (stocktake_id, product_id) 唯一键', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /CREATE TABLE[\s\S]*?finance_stocktake_item/i);
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stocktake_product\s*\(\s*stocktake_id\s*,\s*product_id\s*\)/i
  );
});

test('Task2: finance_stocktake_item 行表包含 count_idempotency_key 与 (tenant_id, count_idempotency_key) 唯一键', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /count_idempotency_key\s+VARCHAR\s*\(\s*96\s*\)/i);
  assert.match(
    sql,
    /UNIQUE KEY\s+uk_stocktake_count_key\s*\(\s*tenant_id\s*,\s*count_idempotency_key\s*\)/i
  );
});

test('Task2: finance_stocktake_item 行表包含 unit_cost / variance_amount DECIMAL(18,2)', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /unit_cost\s+DECIMAL\s*\(\s*18\s*,\s*2\s*\)/i);
  assert.match(sql, /variance_amount\s+DECIMAL\s*\(\s*18\s*,\s*2\s*\)/i);
});

test('Task2: 创建 finance_stocktake_history 历史表', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /CREATE TABLE[\s\S]*?finance_stocktake_history/i);
});

test('Task2: SQL 是幂等的 —— INSERT 使用 WHERE NOT EXISTS 守卫', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  // 字典与权限 INSERT 必须有幂等守卫
  assert.match(sql, /INSERT[\s\S]*?WHERE NOT EXISTS/i);
});

test('Task2: SQL 非破坏 —— 不得 DROP TABLE / TRUNCATE / DELETE FROM', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.doesNotMatch(sql, /DROP\s+TABLE/i);
  assert.doesNotMatch(sql, /TRUNCATE/i);
  assert.doesNotMatch(sql, /DELETE\s+FROM/i);
});

test('Task2: SQL 显式验证输出 HEX(menu_name) 用于中文编码核对', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  assert.match(sql, /HEX\s*\(\s*menu_name\s*\)/i);
});

// ---- Task 2: 10 个分离权限 ----

const REQUIRED_PERMISSIONS = [
  'finance:stocktake:list',
  'finance:stocktake:query',
  'finance:stocktake:add',
  'finance:stocktake:assign',
  'finance:stocktake:count',
  'finance:stocktake:submit',
  'finance:stocktake:recount',
  'finance:stocktake:approve',
  'finance:stocktake:post',
  'finance:stocktake:reverse',
  'finance:stocktake:export'
];

for (const perm of REQUIRED_PERMISSIONS) {
  test(`Task2: SQL 授权新权限码 ${perm}`, () => {
    const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
    if (!existsSync(path)) {
      assert.fail('sql/finance_stocktake_closure.sql 未创建');
    }
    const sql = readSql('finance_stocktake_closure.sql');
    assert.match(sql, new RegExp(perm.replace(/:/g, '\\:')));
  });
}

// ---- Task 2: 损耗原因字典种子 ----

test('Task2: SQL 种子损耗原因字典（EXPIRED/DAMAGED/THEFT/WEIGHING/OPERATION/MISSING_TRANSACTION/OTHER）', () => {
  const path = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (!existsSync(path)) {
    assert.fail('sql/finance_stocktake_closure.sql 未创建');
  }
  const sql = readSql('finance_stocktake_closure.sql');
  for (const code of ['EXPIRED', 'DAMAGED', 'THEFT', 'WEIGHING', 'OPERATION', 'MISSING_TRANSACTION', 'OTHER']) {
    assert.match(sql, new RegExp(code));
  }
});

// ---- Task 2: Mapper XML 与域名类 ----

test('Task2: FinStocktakeMapper.xml 存在且所有写入语句包含 tenant_id', () => {
  const path = join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'FinStocktakeMapper.xml');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeMapper.xml 未创建');
  }
  const xml = readFileSync(path, 'utf8');
  assert.match(xml, /tenant_id/i);
});

test('Task2: FinStocktakeMapper.xml 列表查询包含授权部门过滤', () => {
  const path = join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'FinStocktakeMapper.xml');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeMapper.xml 未创建');
  }
  const xml = readFileSync(path, 'utf8');
  assert.match(xml, /dept_id/i);
  assert.match(xml, /foreach/i);
});

test('Task2: FinStocktakeMapper.xml 锁查询按 dept_id, product_id 确定排序', () => {
  const path = join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'FinStocktakeMapper.xml');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeMapper.xml 未创建');
  }
  const xml = readFileSync(path, 'utf8');
  assert.match(xml, /ORDER BY\s+dept_id\s*,\s*product_id/i);
});

test('Task2: FinStocktakeMapper.xml 不含物理 DELETE', () => {
  const path = join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'FinStocktakeMapper.xml');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeMapper.xml 未创建');
  }
  const xml = readFileSync(path, 'utf8');
  assert.doesNotMatch(xml, /<delete/i);
});

test('Task2: 域名类 FinStocktake.java 存在', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'FinStocktake.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktake.java 未创建');
  }
});

test('Task2: 域名类 FinStocktakeItem.java 存在', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'FinStocktakeItem.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeItem.java 未创建');
  }
});

test('Task2: 域名类 FinStocktakeHistory.java 存在', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'FinStocktakeHistory.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeHistory.java 未创建');
  }
});

// ---- Task 3: 服务接口与控制器 ----

test('Task3: IFinStocktakeService.java 存在且定义 create/assign/list/detail 方法', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'IFinStocktakeService.java');
  if (!existsSync(path)) {
    assert.fail('IFinStocktakeService.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /create/i);
  assert.match(content, /assign/i);
  assert.match(content, /list|page/i);
});

test('Task3: FinStocktakeController.java 暴露 /stocktakes 端点', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'controller', 'FinStocktakeController.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeController.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /\/stocktakes/i);
});

// ---- Task 4: 盲盘与幂等 ----

test('Task4: 计数 VO StocktakeCountRequest.java 含 idempotencyKey 与 version', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'vo', 'StocktakeCountRequest.java');
  if (!existsSync(path)) {
    assert.fail('StocktakeCountRequest.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /idempotencyKey/i);
  assert.match(content, /version/i);
});

// ---- Task 5: 提交/复盘/审批 ----

test('Task5: StocktakeApprovalRequest.java 存在', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'vo', 'StocktakeApprovalRequest.java');
  if (!existsSync(path)) {
    assert.fail('StocktakeApprovalRequest.java 未创建');
  }
});

test('Task5: StocktakeReverseRequest.java 含 idempotencyKey 与 reason', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'domain', 'vo', 'StocktakeReverseRequest.java');
  if (!existsSync(path)) {
    assert.fail('StocktakeReverseRequest.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /idempotencyKey/i);
  assert.match(content, /reason/i);
});

// ---- Task 6: 数量与移动平均成本原子过账 ----

test('Task6: IStockCostService 定义 applyStocktakeLoss / applyStocktakeGain / reverseStocktakeAdjustment', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'IStockCostService.java');
  const content = readFileSync(path, 'utf8');
  assert.match(content, /applyStocktakeLoss/i);
  assert.match(content, /applyStocktakeGain/i);
  assert.match(content, /reverseStocktakeAdjustment/i);
});

test('Task6: IStockCostService.applyStocktakeLoss 接受 sourceLedgerId 参数（成本可追溯）', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'IStockCostService.java');
  const content = readFileSync(path, 'utf8');
  assert.match(content, /applyStocktakeLoss[\s\S]*?sourceLedgerId/i);
});

test('Task6: FinStocktakeServiceImpl 调用 IStockCostService（损耗成本联动）', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'impl', 'FinStocktakeServiceImpl.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeServiceImpl.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /IStockCostService|stockCostService/i);
  assert.match(content, /applyStocktakeLoss|applyStocktakeGain/i);
});

test('Task6: FinStocktakeServiceImpl 过账在一个事务内（@Transactional）', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'impl', 'FinStocktakeServiceImpl.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeServiceImpl.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /@Transactional/i);
});

test('Task6: FinStockLedgerMapper 新增冻结后 movement 汇总查询', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'mapper', 'FinStockLedgerMapper.java');
  const content = readFileSync(path, 'utf8');
  // 新增方法名包含 freeze 或 movement 之一
  assert.match(content, /freeze|movementAfterFreeze|sumMovementAfterFreeze/i);
});

// ---- Task 7: 整单冲销 ----

test('Task7: FinStocktakeServiceImpl 实现整单冲销 reverse 方法', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'impl', 'FinStocktakeServiceImpl.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeServiceImpl.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /reverse/i);
});

// ---- Task 8: 旧接口收口 ----

test('Task8: 旧 StockTakeController 不得保留直接改库存的通道', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'controller', 'StockTakeController.java');
  const content = readFileSync(path, 'utf8');
  // 旧 POST /stockTake 必须被移除或改为创建 DRAFT 任务（不直接过账）
  // 这里检查：不再有直接调用 recordStockTake 返回 ledgerId 的过账行为
  // 允许两种收口方式：
  // 1. 路由被移除（@PostMapping 不存在或类被删除）
  // 2. 改为创建 DRAFT 任务（返回 taskId 而非 ledgerId）
  const hasDirectPost = /@PostMapping[\s\S]*?recordStockTake[\s\S]*?return\s+AjaxResult\.success\s*\(\s*ledgerId\s*\)/i.test(content);
  if (hasDirectPost) {
    assert.fail('旧 StockTakeController 仍保留直接过账通道 POST /stockTake');
  }
});

test('Task8: StockTakeServiceImpl.recordStockTake 不得直接写库存流水和结存（必须走工作流）', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'impl', 'StockTakeServiceImpl.java');
  const content = readFileSync(path, 'utf8');
  // 旧实现直接调用 finStockLedgerMapper.insertFinStockLedger 和 updatePositionQuantity
  // 收口后必须移除这些直接写库的调用，或整个方法被移除
  const hasDirectMutation = /finStockLedgerMapper\.(insertFinStockLedger|updatePositionQuantity)/i.test(content);
  if (hasDirectMutation) {
    assert.fail('StockTakeServiceImpl.recordStockTake 仍直接写库存流水/结存，未走工作流');
  }
});

// ---- Task 9: PC 盘点工作台 ----

test('Task9: PC API junsong-ui-v3/src/api/finance/stocktake.ts 存在', () => {
  const path = join(pcUi, 'src', 'api', 'finance', 'stocktake.ts');
  if (!existsSync(path)) {
    assert.fail('junsong-ui-v3/src/api/finance/stocktake.ts 未创建');
  }
});

test('Task9: PC 页面 junsong-ui-v3/src/views/finance/stocktake/index.vue 存在', () => {
  const path = join(pcUi, 'src', 'views', 'finance', 'stocktake', 'index.vue');
  if (!existsSync(path)) {
    assert.fail('junsong-ui-v3/src/views/finance/stocktake/index.vue 未创建');
  }
});

test('Task9: PC 详情页 junsong-ui-v3/src/views/finance/stocktake/detail.vue 存在', () => {
  const path = join(pcUi, 'src', 'views', 'finance', 'stocktake', 'detail.vue');
  if (!existsSync(path)) {
    assert.fail('junsong-ui-v3/src/views/finance/stocktake/detail.vue 未创建');
  }
});

test('Task9: PC 行组件 junsong-ui-v3/src/views/finance/stocktake/components/StocktakeItemsTable.vue 存在', () => {
  const path = join(pcUi, 'src', 'views', 'finance', 'stocktake', 'components', 'StocktakeItemsTable.vue');
  if (!existsSync(path)) {
    assert.fail('StocktakeItemsTable.vue 未创建');
  }
});

test('Task9: PC stocktake API 使用 v-hasPermi 校验权限', () => {
  const indexPath = join(pcUi, 'src', 'views', 'finance', 'stocktake', 'index.vue');
  if (!existsSync(indexPath)) {
    assert.fail('PC 盘点工作台 index.vue 未创建');
  }
  const content = readFileSync(indexPath, 'utf8');
  assert.match(content, /v-hasPermi/i);
  assert.match(content, /finance:stocktake/i);
});

// ---- Task 10: 小程序盘点 ----

test('Task10: 小程序 API junsong-miniprogram/src/api/stocktake.js 存在且定义新工作流方法', () => {
  // macOS 默认大小写不敏感，stocktake.js 可能匹配到 legacy stockTake.js
  // 必须检查文件内容是否包含新工作流 API（/stocktakes 端点、createStocktake 等）
  const dirPath = join(miniprogram, 'src', 'api');
  const dirExists = existsSync(dirPath);
  if (!dirExists) {
    assert.fail('junsong-miniprogram/src/api/ 目录不存在');
  }
  // 读取目录，查找小写 stocktake.js（精确匹配文件名）
  const files = readdirSync(dirPath);
  const hasLowercase = files.includes('stocktake.js');
  if (!hasLowercase) {
    assert.fail('junsong-miniprogram/src/api/stocktake.js（小写）未创建；当前仅有 legacy stockTake.js');
  }
  const content = readFileSync(join(dirPath, 'stocktake.js'), 'utf8');
  // 新工作流 API 必须调用 /stocktakes 端点（复数），而非 legacy /stockTake
  assert.match(content, /\/stocktakes/i);
});

test('Task10: 小程序页面 junsong-miniprogram/src/pages/stocktake/index.vue 存在', () => {
  const path = join(miniprogram, 'src', 'pages', 'stocktake', 'index.vue');
  if (!existsSync(path)) {
    assert.fail('junsong-miniprogram/src/pages/stocktake/index.vue 未创建');
  }
});

test('Task10: 小程序详情页 junsong-miniprogram/src/pages/stocktake/detail.vue 存在', () => {
  const path = join(miniprogram, 'src', 'pages', 'stocktake', 'detail.vue');
  if (!existsSync(path)) {
    assert.fail('junsong-miniprogram/src/pages/stocktake/detail.vue 未创建');
  }
});

test('Task10: 小程序 pages.json 注册 stocktake 页面', () => {
  const path = join(miniprogram, 'src', 'pages.json');
  const content = readFileSync(path, 'utf8');
  assert.match(content, /pages\/stocktake\/index/i);
  assert.match(content, /pages\/stocktake\/detail/i);
});

test('Task10: 小程序契约测试 test/stocktake-contract.test.mjs 存在', () => {
  const path = join(miniprogram, 'test', 'stocktake-contract.test.mjs');
  if (!existsSync(path)) {
    assert.fail('junsong-miniprogram/test/stocktake-contract.test.mjs 未创建');
  }
});

// ---- Task 11: 损耗分析与智能底座 ----

test('Task11: PC 库存报表 stock.vue 包含盘点入口或损耗指标', () => {
  const path = join(pcUi, 'src', 'views', 'finance', 'report', 'stock.vue');
  const content = readFileSync(path, 'utf8');
  // 添加盘点入口或损耗指标（loss/stocktake 之一）
  assert.match(content, /stocktake|盘点|loss|损耗/i);
});

test('Task11: 后端提供损耗分析查询（Mapper 或 Service 含 loss analytics）', () => {
  // 检查 StockReportMapper.xml 或新 Mapper 包含损耗分析查询
  const stockReportMapperPath = join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'StockReportMapper.xml');
  if (existsSync(stockReportMapperPath)) {
    const content = readFileSync(stockReportMapperPath, 'utf8');
    // 损耗分析查询应包含 loss 或 variance 或 stocktake 相关聚合
    const hasLossAnalytics = /loss|variance|stocktake/i.test(content);
    if (!hasLossAnalytics) {
      // 也可能在新的 Mapper 中
      const stocktakeMapperPath = join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'FinStocktakeMapper.xml');
      if (existsSync(stocktakeMapperPath)) {
        const stContent = readFileSync(stocktakeMapperPath, 'utf8');
        assert.match(stContent, /loss|variance/i);
      } else {
        assert.fail('既未在 StockReportMapper.xml 也未在 FinStocktakeMapper.xml 找到损耗分析查询');
      }
    }
  } else {
    assert.fail('StockReportMapper.xml 不存在');
  }
});

test('Task11: 智能化特征数据查询存在（daily feature dataset）', () => {
  // 检查是否有特征数据查询（sales7d, stockQuantity, daysOfSupply, featureVersion 等）
  const candidates = [
    join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'FinStocktakeMapper.xml'),
    join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'StockReportMapper.xml'),
    join(financeModule, 'src', 'main', 'resources', 'mapper', 'finance', 'PredictiveOpsMapper.xml')
  ];
  let found = false;
  for (const p of candidates) {
    if (existsSync(p)) {
      const content = readFileSync(p, 'utf8');
      if (/featureVersion|feature_version|sales7d|daysOfSupply|days_of_supply/i.test(content)) {
        found = true;
        break;
      }
    }
  }
  if (!found) {
    assert.fail('未找到智能化特征数据查询（featureVersion/sales7d/daysOfSupply 等）');
  }
});

// ---- 全局安全边界 ----

test('安全: SQL 不得使用 Nacos V1 端点（/nacos/v1/）', () => {
  // 检查所有新的 SQL 和 Java 文件不得引用 /nacos/v1/
  const sqlPath = join(repoRoot, 'sql', 'finance_stocktake_closure.sql');
  if (existsSync(sqlPath)) {
    const sql = readFileSync(sqlPath, 'utf8');
    assert.doesNotMatch(sql, /\/nacos\/v1\//i);
  }
});

test('安全: FinStocktakeController 使用 @RequiresPermissions 注解', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'controller', 'FinStocktakeController.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeController.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /@RequiresPermissions/i);
  assert.match(content, /finance:stocktake/i);
});

test('安全: FinStocktakeController 操作权限与查询权限分离', () => {
  const path = join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'controller', 'FinStocktakeController.java');
  if (!existsSync(path)) {
    assert.fail('FinStocktakeController.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  // 操作权限（add/post/reverse）与查询权限（list/query）必须不同
  assert.match(content, /finance:stocktake:list/i);
  assert.match(content, /finance:stocktake:post/i);
  assert.match(content, /finance:stocktake:reverse/i);
});

// ---- Task 13: 验收报告 ----

test('Task13: 验收报告 docs/superpowers/reports/2026-07-25-inventory-stocktake-acceptance.md 存在', () => {
  const path = join(repoRoot, 'docs', 'superpowers', 'reports', '2026-07-25-inventory-stocktake-acceptance.md');
  if (!existsSync(path)) {
    assert.fail('验收报告未创建');
  }
});
