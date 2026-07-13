# 库存报表运维手册

> 适用版本：第一期经营库存报表（Task 1-9）
> 设计规格：`docs/superpowers/specs/2026-07-12-stock-report-design.md`
> 实施计划：`docs/superpowers/plans/2026-07-12-stock-report-implementation.md`

## 1. 功能概述

库存报表是财务模块的子能力，提供基于库存事实层的可查询、可下钻、可对账的经营库存视图。第一期不包含库存金额和毛利，相关成本层在第二期 Task 10-12 交付。

### 1.1 报表能力

| 能力 | 端点 | 权限 | 说明 |
|---|---|---|---|
| 库存报表（汇总 + 分页） | `POST /finance/report/stock` | `finance:report:stock` | 一次返回汇总和首页明细 |
| 库存报表汇总 | `POST /finance/report/stock/summary` | `finance:report:stock` | 仅返回汇总卡片数据 |
| 库存报表分页 | `POST /finance/report/stock/page` | `finance:report:stock` | 服务端分页明细 |
| 库存流水下钻 | `POST /finance/report/stock/ledger/page` | `finance:report:stock` | 单商品按时间倒序的变动流水 |
| 库存报表导出 | `POST /finance/report/stock/export` | `finance:report:stock:export` | blob 下载，复用查询条件 |
| 库存对账 | `POST /finance/report/stock/reconciliation` | `finance:stock:reconciliation` | 只读异常清单 |

### 1.2 数量恒等式

```
期初数量 + 采购净入库数量 - 销售净出库数量 + 其他调整净数量 = 期末数量
```

- 期初取 `startDate` 之前最近一次 `fin_stock_snapshot` 的期末数量，无快照时回退为 `startDate` 之前 `fin_stock_ledger` 净累加。
- 采购净入库来自流水 `change_type IN ('PURCHASE_IN', 'PURCHASE_REVERSE')`。
- 销售净出库来自流水 `change_type IN ('SALE_OUT', 'SALE_REVERSE')`。
- 其他调整净数量在第一期恒为 0，前端不展示。

### 1.3 赠品口径

- 进货单按每个商品的全部明细数量入库，不论 `is_gift` 是否为 1。赠品不计采购金额，但 `quantity` 必须计入采购入库数量。
- 销售单按 `sale_quantity + gift_quantity` 出库。赠品不计销售收入，但必须计入销售出库数量。
- 报表的采购净入库和销售净出库直接汇总可信库存流水，**不得再次从业务单据拼算**，避免赠品重复计算或遗漏。

## 2. 数据库表

| 表 | 角色 | 关键约束 |
|---|---|---|
| `fin_stock_ledger` | 不可变库存变动流水 | `tenant_id BIGINT NOT NULL`，索引 `(tenant_id, dept_id, product_id, create_time)` 与 `(tenant_id, dept_id, reference_type, reference_id, product_id)` |
| `fin_stock_position` | 当前结存 | 唯一键 `(tenant_id, dept_id, product_id)` |
| `fin_stock_snapshot` | 日终事实快照 | 唯一键 `(tenant_id, snapshot_date, dept_id, product_id)`，恒等式 `opening + in - out = quantity` |

基础 DDL：
- `sql/stock_ledger_foundation.sql` - 流水/快照基础 DDL
- `sql/stock_position.sql` - 结存基础 DDL

租户安全迁移：
- `sql/finance_stock_report_foundation.sql` - 幂等迁移，含前置校验和对账输出

## 3. API 端点与权限

### 3.1 权限码

| 权限码 | 用途 | menu_id |
|---|---|---|
| `finance:report:stock` | 库存报表查看 | 2155（C 类菜单） |
| `finance:report:stock:export` | 库存报表导出 | 2156（F 类按钮） |
| `finance:stock:reconciliation` | 库存对账 | 2157（F 类按钮） |
| `finance:stock:health` | 库存底座健康检查（R6-E） | 通过 `stock_health_permission.sql` 维护 |

### 3.2 授权矩阵

| 角色 | 查看 | 导出 | 对账 | 健康检查 |
|---|---|---|---|---|
| 超级管理员 role_id=1 | ✓ | ✓ | ✓ | ✓ |
| 财务角色 role_id=100 | ✓ | ✓ | ✓ | 视配置 |
| 门店角色 | 按数据范围 | 按数据范围 | 按数据范围 | 按数据范围 |

### 3.3 后端 Controller

`junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReportController.java`

每个端点都通过 `@RequiresPermissions` 强制后端授权，UI 隐藏仅作辅助。

## 4. 菜单与权限配置

### 4.1 库存报表菜单树

```
财务管理 (2000, M)
└── 库存报表 (2155, C, finance:report:stock)
    ├── 库存报表导出 (2156, F, finance:report:stock:export)
    └── 库存对账 (2157, F, finance:stock:reconciliation)
```

### 4.2 部署 SQL

```bash
bin/deploy-sql.sh DEV sql/finance_stock_report_menu.sql
```

SQL 特性：
- 以 `SET NAMES utf8mb4;` 开头，确保中文 menu_name 正确落库。
- 所有 `INSERT` 使用 `WHERE NOT EXISTS` 守卫，可重复执行。
- 末尾输出 `SELECT menu_id, menu_name, perms, HEX(menu_name) ...` 用于人工核对编码。
- 不含 `DROP TABLE / TRUNCATE / DELETE FROM`，非破坏。

### 4.3 验证菜单编码

部署后必须执行：

```sql
SELECT menu_id, menu_name, perms, HEX(menu_name) FROM sys_menu WHERE menu_id IN (2155, 2156, 2157);
```

`HEX(menu_name)` 应为 utf8mb4 编码的中文（无 `3F` 问号、无乱码）。

## 5. 如何验证

### 5.1 健康检查

库存底座健康检查（R6-E）通过 `finance:stock:health` 权限调用：

```http
GET /finance/stock/health
```

返回租户和授权门店范围内的：
- 流水总数、快照总数
- 负库存商品数
- 结存无流水商品数
- 昨日有结存无快照数
- 当日快照与结存不一致数

### 5.2 对账查询

库存对账接口返回只读异常清单：

```http
POST /finance/report/stock/reconciliation
```

异常代码：

| 代码 | 含义 | 处理建议 |
|---|---|---|
| `POSITION_WITHOUT_LEDGER` | 结存存在但无任何流水记录 | 排查结存来源，必要时人工补录流水 |
| `LEDGER_POSITION_MISMATCH` | 流水累计与结存不一致 | 比对流水和结存，确定哪一方为准 |
| `SNAPSHOT_EQUATION_MISMATCH` | 快照恒等式不成立 | 重算当日快照 |
| `LATEST_SNAPSHOT_MISMATCH` | 最新快照与当前结存不一致 | 重算当日快照或确认今日变动已落流水 |

### 5.3 报表端到端验收

构造测试数据：
1. 在独立测试门店创建进货单：普通 10 件 + 赠品 2 件。
2. 创建销售单：付费 8 件 + 赠品 2 件。
3. 调用 `POST /finance/report/stock` 查询当日。

预期：
- 采购净入库 = 12（含赠品）
- 销售净出库 = 10（含赠品）
- 期末 = 期初 + 12 - 10 = 期初 + 2
- 采购金额仅由非赠品明细计算
- 销售收入仅由付费销售金额决定

修改赠品数量后验证：仅生成差额流水，不重复计算。
删除单据后验证：生成反向流水，库存恢复。

## 6. 已知限制（第一期）

1. **不含库存金额和毛利**：第一期仅交付数量报表。库存成本、销售成本、毛利在第二期 Task 10-12 交付。前端不会用零值伪装未完成成本。
2. **无盘点/调拨/采购退货/销售退货入口**：第一期不能虚构这些变动类型，相关流水只来自采购入库、销售出库及其冲销。
3. **流水下钻分页在服务层内存分页**：大数据量场景需优化为 SQL 分页。
4. **导出限制 200 条**：超过部分需要分批导出。
5. **其他调整净数量恒为 0**：第一期没有真实、受控的调整入口，该字段不在前端展示。
6. **滞销阈值固定 30 天**：阈值使用系统配置，默认 30 天。

## 7. 故障排查

### 7.1 报表显示"暂未开放"

**原因**：UI 仍为暂停页，或前端构建版本未更新。
**解决**：
1. 确认 `junsong-ui-v3/src/views/finance/report/stock.vue` 不含 "暂未开放" 字符串。
2. 重新构建前端：`cd junsong-ui-v3 && npm run build`。
3. 浏览器硬刷新清缓存。

### 7.2 报表查询返回空数据

**原因**：
- 用户未被授予 `finance:report:stock` 权限。
- 用户授权部门集合与请求 deptIds 交集为空。
- 该租户/门店在查询区间内确实无库存流水和快照。

**排查**：
1. 检查 `sys_role_menu` 中当前用户角色是否关联 menu_id=2155。
2. 检查用户授权部门（数据范围服务）。
3. 直接查 `fin_stock_ledger` 确认是否有数据。

### 7.3 期末与快照不一致

**原因**：流水与快照恒等式被破坏，可能因为：
- 当日快照任务未执行或失败。
- 流水被外部直接修改。
- 跨租户串库。

**排查**：
1. 调用 `GET /finance/stock/health` 查看异常计数。
2. 调用 `POST /finance/report/stock/reconciliation` 查看具体异常清单。
3. 重跑快照任务 `rebuildDailySnapshot(tenantId, snapshotDate, deptId)`。
4. 如果是跨租户串库，立即停止服务并排查 `tenant_id` 隔离链路。

### 7.4 导出按钮不可见

**原因**：用户未被授予 `finance:report:stock:export` 权限。
**解决**：
1. 检查 `sys_role_menu` 中当前用户角色是否关联 menu_id=2156。
2. 重新部署 `sql/finance_stock_report_menu.sql`。
3. 用户重新登录刷新权限缓存。

### 7.5 中文菜单名乱码

**原因**：部署 SQL 未设置 utf8mb4 字符集。
**解决**：
1. 确认 SQL 以 `SET NAMES utf8mb4;` 开头。
2. 使用 `bin/deploy-sql.sh` 部署（自动带 `--default-character-set=utf8mb4`）。
3. 验证 `HEX(menu_name)` 是否为 utf8mb4 编码。

### 7.6 流水下钻无数据

**原因**：
- 该商品在该日期区间内确实无流水。
- 流水 `del_flag = '1'` 被软删除。
- `tenant_id` 隔离导致看不到其他租户流水。

**排查**：
```sql
SELECT COUNT(*) FROM fin_stock_ledger
WHERE tenant_id = ?
  AND dept_id = ?
  AND product_id = ?
  AND del_flag = '0'
  AND create_time >= ?
  AND create_time < DATE_ADD(?, INTERVAL 1 DAY);
```

## 8. 相关文件索引

### 8.1 后端

| 文件 | 职责 |
|---|---|
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReportController.java` | 6 个库存端点 + 其他报表端点 |
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceReportServiceImpl.java` | 报表服务：权限校验、部门交集、调用 Mapper |
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StockSnapshotServiceImpl.java` | 每日快照重建服务 |
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StockHealthServiceImpl.java` | 库存健康检查与对账 |
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/task/StockDailySnapshotTask.java` | 每日快照定时任务 |
| `junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockReportMapper.xml` | 库存报表查询 SQL |
| `junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockHealthMapper.xml` | 库存对账查询 SQL |
| `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinStockLedgerMapper.xml` | 库存流水/快照基础 SQL |

### 8.2 前端

| 文件 | 职责 |
|---|---|
| `junsong-ui-v3/src/api/finance/stockreport.ts` | 6 个 API 函数 + TypeScript 接口 |
| `junsong-ui-v3/src/views/finance/report/stock.vue` | 库存报表页面 |
| `junsong-ui-v3/src/views/finance/report/components/StockLedgerDrawer.vue` | 流水下钻抽屉 |

### 8.3 SQL

| 文件 | 职责 |
|---|---|
| `sql/stock_ledger_foundation.sql` | 流水/快照基础 DDL |
| `sql/stock_position.sql` | 结存基础 DDL |
| `sql/finance_stock_report_foundation.sql` | 租户安全迁移 |
| `sql/finance_stock_report_menu.sql` | 菜单与权限补齐（本任务） |
| `sql/stock_health_permission.sql` | 库存健康检查权限（R6-E） |

### 8.4 测试

| 文件 | 职责 |
|---|---|
| `scripts/finance-stock-report-foundation.test.mjs` | Task 1：迁移契约 |
| `scripts/stock-snapshot-rebuild-contract.test.mjs` | Task 4：快照重建契约 |
| `scripts/finance-stock-report-ui.test.mjs` | Task 8：前端契约 |
| `scripts/finance-stock-report-acceptance.test.mjs` | Task 9：全量验收（本任务） |
| `scripts/stock-ledger-health.test.mjs` | 库存底座健康回归 |

### 8.5 完成报告

`docs/superpowers/reports/2026-07-12-stock-report-task-{1..9}-completion.md` 共 9 份，按规格第 11 节填写。
