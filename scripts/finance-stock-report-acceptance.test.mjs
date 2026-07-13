import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'

// =====================================================================
// Task 9 验收测试：菜单、导出权限、真实数据验收和发布门禁
// 全量验证库存报表第一期实现：SQL 幂等、后端契约、前端契约、报告完整性。
// =====================================================================

const ROOT = new URL('../', import.meta.url)

function read(rel) {
  const abs = new URL(rel, ROOT)
  assert.equal(existsSync(abs), true, `${rel} must exist`)
  return readFileSync(abs, 'utf8')
}

function exists(rel) {
  return existsSync(new URL(rel, ROOT))
}

// =====================================================================
// 1. SQL 菜单与权限契约
// =====================================================================

const sql = read('sql/finance_stock_report_menu.sql')
const sqlNormalized = sql.replace(/\s+/g, ' ')

test('SQL 文件以 SET NAMES utf8mb4 开始', () => {
  assert.match(sql, /^SET NAMES utf8mb4;/)
})

test('SQL 是幂等的：所有 INSERT 使用 WHERE NOT EXISTS 守卫', () => {
  // 统计 INSERT 语句和 WHERE NOT EXISTS 守卫数量
  const insertCount = (sqlNormalized.match(/INSERT\s+INTO/gi) || []).length
  const notExistsCount = (sqlNormalized.match(/WHERE\s+NOT\s+EXISTS/gi) || []).length
  assert.ok(
    insertCount > 0 && notExistsCount >= insertCount,
    `INSERT 数 ${insertCount} 必须 <= WHERE NOT EXISTS 数 ${notExistsCount}`,
  )
})

test('SQL 非破坏：不得 DROP TABLE / TRUNCATE / DELETE FROM', () => {
  assert.doesNotMatch(sql, /DROP\s+TABLE|TRUNCATE|DELETE\s+FROM/i)
})

test('SQL 保留既有 finance:report:stock 权限码', () => {
  // 既有 2155 菜单的权限码不能被破坏
  assert.match(sqlNormalized, /finance:report:stock\b/)
})

test('SQL 新增 finance:report:stock:export 权限码', () => {
  assert.match(sqlNormalized, /finance:report:stock:export/)
})

test('SQL 新增 finance:stock:reconciliation 权限码', () => {
  assert.match(sqlNormalized, /finance:stock:reconciliation/)
})

test('SQL 授权给超级管理员 role_id=1', () => {
  assert.match(sqlNormalized, /SELECT\s+1,\s*2156/i)
  assert.match(sqlNormalized, /SELECT\s+1,\s*2157/i)
})

test('SQL 授权给财务角色 role_id=100', () => {
  assert.match(sqlNormalized, /SELECT\s+100,\s*2156/i)
  assert.match(sqlNormalized, /SELECT\s+100,\s*2157/i)
})

test('SQL 显式验证输出 HEX(menu_name) 用于中文编码核对', () => {
  assert.match(sqlNormalized, /HEX\s*\(\s*menu_name\s*\)/i)
})

test('SQL 在菜单 2155 下挂载按钮权限（parent_id=2155）', () => {
  assert.match(sqlNormalized, /parent_id.*2155|,\s*2155\s*,/i)
})

// =====================================================================
// 2. 后端 StockReportMapper.xml 契约
// =====================================================================

const stockReportXml = read(
  'junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockReportMapper.xml',
)

test('StockReportMapper.xml 显式声明 tenant_id 隔离', () => {
  // AuthorizedStockBase 必须以 tenant_id = #{tenantId} 起步
  assert.match(
    stockReportXml,
    /tenant_id\s*=\s*#\{tenantId\}/i,
    'AuthorizedStockBase 必须显式 tenant_id = #{tenantId}',
  )
})

test('StockReportMapper.xml 通过 change_type 显式分类采购/销售流水', () => {
  // 采购入库分类：PURCHASE_IN / PURCHASE_REVERSE
  assert.match(stockReportXml, /'PURCHASE_IN'/)
  assert.match(stockReportXml, /'PURCHASE_REVERSE'/)
  // 销售出库分类：SALE_OUT / SALE_REVERSE
  assert.match(stockReportXml, /'SALE_OUT'/)
  assert.match(stockReportXml, /'SALE_REVERSE'/)
})

test('StockReportMapper.xml 使用半开日期区间保护索引', () => {
  // 区间内：create_time >= startDate AND create_time < DATE_ADD(endDate, INTERVAL 1 DAY)
  assert.match(stockReportXml, /create_time\s*&gt;=\s*#\{query\.startDate\}/i)
  assert.match(
    stockReportXml,
    /create_time\s*&lt;\s*DATE_ADD\s*\(\s*#\{query\.endDate\}\s*,\s*INTERVAL\s+1\s+DAY\s*\)/i,
  )
})

test('StockReportMapper.xml 期初取 startDate 之前最近快照，回退到流水累加', () => {
  // 期初快照：snapshot_date < startDate
  assert.match(stockReportXml, /snapshot_date\s*&lt;\s*#\{query\.startDate\}/i)
  // 期初流水回退：create_time < startDate
  assert.match(stockReportXml, /create_time\s*&lt;\s*#\{query\.startDate\}/i)
})

test('StockReportMapper.xml 期末与 endDate 当日快照做对账', () => {
  assert.match(stockReportXml, /snapshot_date\s*=\s*#\{query\.endDate\}/i)
  assert.match(stockReportXml, /'ANOMALY'/)
})

test('StockReportMapper.xml 提供 summary/page/ledger 三组查询', () => {
  assert.match(stockReportXml, /<select id="selectStockReportSummary"/)
  assert.match(stockReportXml, /<select id="selectStockReportItems"/)
  assert.match(stockReportXml, /<select id="countStockReportItems"/)
  assert.match(stockReportXml, /<select id="selectStockLedgerRows"/)
})

test('StockReportMapper.xml 流水下钻按 create_time ASC, ledger_id ASC 确定排序', () => {
  const block = stockReportXml.match(/<select id="selectStockLedgerRows"[\s\S]*?<\/select>/)?.[0] ?? ''
  assert.match(block, /ORDER BY create_time ASC, ledger_id ASC/i)
})

// =====================================================================
// 3. 后端 FinanceReportController 契约
// =====================================================================

const controller = read(
  'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReportController.java',
)

test('Controller 暴露 6 个库存相关端点', () => {
  const stockEndpoints = controller.match(/@PostMapping\s*\(\s*"\/stock[^"]*"\s*\)/g) || []
  assert.equal(stockEndpoints.length, 6, `期望 6 个库存端点，实际 ${stockEndpoints.length}`)
})

test('Controller 库存查看端点使用 finance:report:stock 权限', () => {
  // /stock、/stock/summary、/stock/page、/stock/ledger/page 共 4 个端点
  const stockPermBlocks = controller.match(
    /@RequiresPermissions\s*\(\s*"finance:report:stock"\s*\)\s*@PostMapping\s*\(\s*"\/stock[^"]*"\s*\)/g,
  ) || []
  assert.ok(stockPermBlocks.length >= 4, `期望至少 4 个 finance:report:stock 端点，实际 ${stockPermBlocks.length}`)
})

test('Controller 导出端点使用 finance:report:stock:export 权限', () => {
  assert.match(
    controller,
    /@RequiresPermissions\s*\(\s*"finance:report:stock:export"\s*\)\s*@PostMapping\s*\(\s*"\/stock\/export"\s*\)/,
  )
})

test('Controller 对账端点使用 finance:stock:reconciliation 权限', () => {
  assert.match(
    controller,
    /@RequiresPermissions\s*\(\s*"finance:stock:reconciliation"\s*\)\s*@PostMapping\s*\(\s*"\/stock\/reconciliation"\s*\)/,
  )
})

test('Controller 端点列表完整', () => {
  const expected = [
    '/stock',
    '/stock/summary',
    '/stock/page',
    '/stock/ledger/page',
    '/stock/export',
    '/stock/reconciliation',
  ]
  for (const path of expected) {
    assert.match(
      controller,
      new RegExp(`@PostMapping\\s*\\(\\s*"\\${path}"\\s*\\)`),
      `缺少端点 ${path}`,
    )
  }
})

// =====================================================================
// 4. 后端 StockHealthMapper.xml 契约
// =====================================================================

const healthXml = read(
  'junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockHealthMapper.xml',
)

test('StockHealthMapper.xml 所有查询显式 scope tenant_id', () => {
  // 每个查询都需要 scope tenant_id（参数 #{tenantId}）
  const selects = healthXml.match(/<select[^>]*id="[^"]+"[\s\S]*?<\/select>/g) || []
  assert.ok(selects.length > 0, '至少应有一个查询')
  for (const sel of selects) {
    assert.match(
      sel,
      /tenant_id\s*=\s*#\{tenantId\}/i,
      `查询 ${sel.match(/id="([^"]+)"/)?.[1]} 缺少 tenant_id = #{tenantId}`,
    )
  }
})

test('StockHealthMapper.xml 提供四类对账异常查询', () => {
  const expectedIds = [
    'findPositionsWithoutLedger',
    'findLedgerPositionMismatch',
    'findSnapshotEquationMismatch',
    'findLatestSnapshotMismatch',
  ]
  for (const id of expectedIds) {
    assert.match(healthXml, new RegExp(`<select[^>]*id="${id}"`), `缺少对账查询 ${id}`)
  }
})

test('StockHealthMapper.xml 是只读的：不得包含 INSERT/UPDATE/DELETE', () => {
  assert.doesNotMatch(healthXml, /<insert|<update|<delete/i)
})

test('StockHealthMapper.xml 支持授权部门集合过滤', () => {
  // 通过 deptIds foreach 进行授权门店过滤
  assert.match(healthXml, /deptIds\s*!=\s*null/i)
  assert.match(healthXml, /<foreach/i)
})

// =====================================================================
// 5. 前端 stock.vue 契约
// =====================================================================

const stockVue = read('junsong-ui-v3/src/views/finance/report/stock.vue')

test('stock.vue 不再展示"暂未开放"暂停页', () => {
  assert.doesNotMatch(stockVue, /暂未开放/)
})

test('stock.vue 导出按钮使用 v-hasPermi 校验 finance:report:stock:export', () => {
  assert.match(stockVue, /v-hasPermi.*finance:report:stock:export/s)
})

test('stock.vue 调用库存报表 API', () => {
  assert.match(stockVue, /getStockReport|getStockReportSummary|getStockReportPage/)
})

// =====================================================================
// 6. 前端 stockreport.ts 契约
// =====================================================================

const stockReportApi = read('junsong-ui-v3/src/api/finance/stockreport.ts')

test('stockreport.ts 定义 6 个 API 函数', () => {
  const expected = [
    'getStockReport',
    'getStockReportSummary',
    'getStockReportPage',
    'getStockLedgerPage',
    'exportStockReport',
    'getStockReconciliation',
  ]
  for (const fn of expected) {
    assert.match(
      stockReportApi,
      new RegExp(`export function ${fn}\\b`),
      `缺少 API 函数 ${fn}`,
    )
  }
})

test('stockreport.ts export 使用 blob 响应类型', () => {
  assert.match(stockReportApi, /responseType:\s*'blob'/)
})

// =====================================================================
// 7. 前端 StockLedgerDrawer.vue 契约
// =====================================================================

test('StockLedgerDrawer.vue 存在', () => {
  assert.equal(
    exists('junsong-ui-v3/src/views/finance/report/components/StockLedgerDrawer.vue'),
    true,
  )
})

const ledgerDrawer = read('junsong-ui-v3/src/views/finance/report/components/StockLedgerDrawer.vue')

test('StockLedgerDrawer.vue 调用 getStockLedgerPage', () => {
  assert.match(ledgerDrawer, /getStockLedgerPage/)
})

test('StockLedgerDrawer.vue 渲染流水下钻所需列', () => {
  const expectedCols = [
    '变动时间',
    '变动类型',
    '变动前数量',
    '变动数量',
    '变动后数量',
    '来源类型',
    '来源单号',
    '操作人',
    '备注',
  ]
  for (const col of expectedCols) {
    assert.match(ledgerDrawer, new RegExp(col), `缺少列 ${col}`)
  }
})

// =====================================================================
// 8. 完成报告完整性
// =====================================================================

test('Task 1-9 完成报告均存在', () => {
  for (let i = 1; i <= 9; i++) {
    const path = `docs/superpowers/reports/2026-07-12-stock-report-task-${i}-completion.md`
    assert.equal(exists(path), true, `缺少 ${path}`)
  }
})

test('Task 9 完成报告存在', () => {
  const path = 'docs/superpowers/reports/2026-07-12-stock-report-task-9-completion.md'
  assert.equal(exists(path), true, `缺少 ${path}`)
})

// =====================================================================
// 9. 设计与计划文档存在
// =====================================================================

test('设计规格文档存在', () => {
  assert.equal(
    exists('docs/superpowers/specs/2026-07-12-stock-report-design.md'),
    true,
  )
})

test('实施计划文档存在', () => {
  assert.equal(
    exists('docs/superpowers/plans/2026-07-12-stock-report-implementation.md'),
    true,
  )
})

test('运维文档存在', () => {
  assert.equal(
    exists('docs/finance-stock-report-operations.md'),
    true,
  )
})

// =====================================================================
// 10. 财务/安全边界综合断言
// =====================================================================

test('所有库存相关 Mapper 都使用 #{} 参数化查询（防 SQL 注入）', () => {
  // 不应出现 ${} 字符串拼接的 tenant_id / dept_id 条件
  assert.doesNotMatch(
    stockReportXml,
    /\$\{\s*(tenantId|deptId|productId|startDate|endDate)\s*\}/,
  )
  assert.doesNotMatch(
    healthXml,
    /\$\{\s*(tenantId|deptId|productId)\s*\}/,
  )
})

test('StockReportMapper.xml 期末计算使用期初+采购净入库-销售净出库恒等式', () => {
  // 期末 = 期初 + 采购净入库 - 销售净出库
  assert.match(stockReportXml, /purchase_net_in/i)
  assert.match(stockReportXml, /sale_net_out/i)
  // 期末表达式必须包含三个分量
  const closingExpr = stockReportXml.match(
    /COALESCE\(snap\.quantity, led_open\.opening_quantity, 0\)\s*\+\s*COALESCE\(rng\.purchase_net_in, 0\)\s*-\s*COALESCE\(rng\.sale_net_out, 0\)\s*AS closingQuantity/,
  )
  assert.ok(closingExpr, '期末计算表达式不符合期初+采购净入库-销售净出库恒等式')
})

test('StockReportMapper.xml 库存状态分类完整（负库存/零库存/低库存/滞销/正常）', () => {
  assert.match(stockReportXml, /'NEGATIVE_STOCK'/)
  assert.match(stockReportXml, /'ZERO_STOCK'/)
  assert.match(stockReportXml, /'LOW_STOCK'/)
  assert.match(stockReportXml, /'STALE'/)
  assert.match(stockReportXml, /'NORMAL'/)
})

test('StockHealthMapper.xml 异常代码与设计规格一致', () => {
  assert.match(healthXml, /'POSITION_WITHOUT_LEDGER'/)
  assert.match(healthXml, /'LEDGER_POSITION_MISMATCH'/)
  assert.match(healthXml, /'SNAPSHOT_EQUATION_MISMATCH'/)
  assert.match(healthXml, /'LATEST_SNAPSHOT_MISMATCH'/)
})
