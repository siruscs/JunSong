# 库存报表分期实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 先交付严格按租户和授权门店隔离、包含进货与销售赠品实物数量、可下钻和可对账的经营库存报表，再独立交付移动加权成本与毛利核对。

**Architecture:** `fin_stock_ledger` 是不可变变动事实，`fin_stock_position` 是当前结存，`fin_stock_snapshot` 是日终事实。第一期先修正三表的租户键、写入锁、快照和对账，再由专用库存报表 Mapper 查询；第二期增加成本层，禁止用当前商品价格回算历史成本。

**Tech Stack:** Java 17、Spring Boot 4、MyBatis 4、MySQL、Vue 3、TypeScript、Element Plus、JUnit 5、Node test。

**Authoritative spec:** `docs/superpowers/specs/2026-07-12-stock-report-design.md`

---

## 全局执行协议

每个任务由一个新的执行 Agent 完成。Agent 开始前必须读取根 `AGENTS.md`、本计划和设计规格，检查 `git status --short`，保留现有 RabbitMQ、证书、构建产物和其他用户改动。

修改任何现有函数、类或方法前必须执行 GitNexus：

```text
impact({target: "准确符号名", direction: "upstream", includeTests: true})
```

报告直接调用者、受影响流程和风险等级。HIGH/CRITICAL 必须先警告用户并取得确认。每个任务先写失败测试并观察失败，再实现最小改动。

每个任务完成后必须创建：

```text
docs/superpowers/reports/2026-07-12-stock-report-task-<N>-completion.md
```

报告按规格第 11 节填写。报告和代码一起暂存后运行：

```text
detect_changes({scope: "staged"})
git diff --cached --name-only
git diff --cached --check
```

执行 Agent 不得推送，不得宣告最终完成。主复核 Agent 独立复跑测试并给出 `APPROVED`、`CHANGES_REQUIRED` 或 `BLOCKED`。只有 `APPROVED` 后才进入下一任务。

---

## 第一期：经营库存报表

### Task 1：建立租户安全的库存表结构与迁移检查

**Files:**
- Create: `sql/finance_stock_report_foundation.sql`
- Create: `scripts/finance-stock-report-foundation.test.mjs`
- Modify: `sql/stock_ledger_foundation.sql`
- Modify: `sql/stock_position.sql`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-1-completion.md`

- [ ] **Step 1：写迁移契约失败测试**

在 Node 测试中读取 SQL 并断言：文件以 `SET NAMES utf8mb4;` 开始；三表都有 `tenant_id BIGINT NOT NULL`；position 唯一键为 `(tenant_id, dept_id, product_id)`；snapshot 唯一键为 `(tenant_id, snapshot_date, dept_id, product_id)`；流水索引包含 `(tenant_id, dept_id, product_id, create_time)` 和来源对账键；脚本输出重复键、零租户、结存无流水、流水与结存差异、快照重复行数量。

```js
assert.match(sql, /UNIQUE KEY uk_stock_position_tenant_dept_product\s*\(tenant_id, dept_id, product_id\)/i)
assert.match(sql, /UNIQUE KEY uk_stock_snapshot_tenant_date_dept_product\s*\(tenant_id, snapshot_date, dept_id, product_id\)/i)
assert.match(sql, /AS position_without_ledger_count/i)
assert.match(sql, /AS ledger_position_mismatch_count/i)
```

- [ ] **Step 2：观察测试失败**

Run: `node --test scripts/finance-stock-report-foundation.test.mjs`

Expected: FAIL，指出租户唯一键或对账输出缺失。

- [ ] **Step 3：实现幂等迁移**

迁移顺序必须是：检查异常数据 → 输出无法推导租户的行 → 仅对能从所属采购/销售单唯一推导租户的流水回填 → 检查重复业务键 → 删除旧唯一键 → 创建新唯一键和索引。不能用 `tenant_id = 1` 覆盖未知存量数据。发现无法推导或重复键时用 `SIGNAL SQLSTATE '45000'` 阻断。

- [ ] **Step 4：运行 SQL 契约测试**

Run: `node --test scripts/finance-stock-report-foundation.test.mjs`

Expected: PASS。

- [ ] **Step 5：在隔离测试库重复执行迁移**

Run: `bin/deploy-sql.sh DEV sql/finance_stock_report_foundation.sql`

Expected: 连续执行两次均成功；对账输出为 0，或明确列出阻断数据且不修改其租户。

- [ ] **Step 6：完成报告、暂存检查并提交**

```bash
git add -f sql/finance_stock_report_foundation.sql scripts/finance-stock-report-foundation.test.mjs docs/superpowers/reports/2026-07-12-stock-report-task-1-completion.md
git add sql/stock_ledger_foundation.sql sql/stock_position.sql
git diff --cached --check
git commit -m "fix(finance): enforce tenant-safe stock keys"
```

### Task 2：将 tenantId 贯穿库存实体、Mapper、行锁和差额对账

**Files:**
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinStockLedger.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinStockSnapshot.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/FinStockPositionView.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinStockLedgerMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinStockLedgerMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IFinStockLedgerService.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinStockLedgerServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinStockLedgerServiceImplTest.java`
- Create: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/mapper/FinStockLedgerMapperContractTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-2-completion.md`

- [ ] **Step 1：对 `reconcilePurchaseStock`、`reconcileSaleStock` 和所有现有 Mapper 方法运行 upstream impact**

Expected: 报告采购、销售、快照任务和测试调用者；HIGH/CRITICAL 时先取得用户确认。

- [ ] **Step 2：写两个租户相同门店商品不串库的失败测试**

测试内存 Mapper 键从 `deptId:productId` 改为 `tenantId:deptId:productId`，建立租户 1 和租户 2 都使用 `deptId=10, productId=100` 的场景，断言两边结存和流水独立。再写 XML 契约测试，断言每个查询和更新显式包含 `tenant_id = #{tenantId}`。

- [ ] **Step 3：观察失败**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=FinStockLedgerServiceImplTest,FinStockLedgerMapperContractTest test`

Expected: FAIL，现有签名或 SQL 缺少 tenantId。

- [ ] **Step 4：修改服务签名**

目标签名：

```java
void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName,
    Long referenceId, String referenceNo, Integer targetQuantity, BigDecimal unitCost, String operator);
void reconcileSaleStock(Long tenantId, Long deptId, Long productId, String productName,
    Long referenceId, String referenceNo, Integer targetQuantity, String operator);
```

`tenantId`、`deptId`、`productId` 任一为空立即失败关闭。insert position、FOR UPDATE、sumRecordedNet、selectRecordedProductIds、update position 和 insert ledger 全部使用同一租户键。更新结存影响行数不等于 1 时抛出安全业务异常并回滚。

- [ ] **Step 5：运行聚焦测试**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=FinStockLedgerServiceImplTest,FinStockLedgerMapperContractTest test`

Expected: PASS。

- [ ] **Step 6：报告、暂存检查并提交**

Commit: `fix(finance): scope stock ledger by tenant`

### Task 3：修正采购和销售写入链路，锁定赠品数量口径

**Files:**
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinPurchaseServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinSaleRecordServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinPurchaseServiceImplTest.java`
- Modify: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinSaleRecordServiceImplTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-3-completion.md`

- [ ] **Step 1：对 `applyPurchaseStockIn`、`reversePurchaseStock`、`applySaleStockOut`、`reverseSaleStock` 运行 upstream impact**

- [ ] **Step 2：写赠品失败测试**

采购场景必须覆盖：同商品普通明细 `quantity=10,isGift=0` 加赠品明细 `quantity=2,isGift=1`，目标入库 12；把赠品从 2 改为 5，只追加 `+3`；删除单据追加 `-15`；赠品金额为 0，但库存不为 0；重复提交不新增流水。

销售场景必须覆盖：`saleQuantity=8,giftQuantity=2` 目标出库 10；赠品改为 5 只追加 `-3`；删除回补 13；销售收入仍只由付费销售金额决定；重复提交不新增流水。

- [ ] **Step 3：观察失败**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=FinPurchaseServiceImplTest,FinSaleRecordServiceImplTest test`

- [ ] **Step 4：从可信上下文取得租户并调用新签名**

使用 `TenantContext.getTenantId()`，并校验业务单据部门属于当前授权范围。采购目标数量为同商品全部明细 `quantity` 合计，不按 `isGift` 过滤；成本字段只从非赠品明细计算，第一期不得把赠品零价覆盖商品正常采购单价。销售目标数量明确计算为：

```java
int targetOut = Math.addExact(saleQuantity, giftQuantity == null ? 0 : giftQuantity);
```

两个数量均校验非负，付费销售数量必须大于 0；整数溢出转为安全业务异常。

- [ ] **Step 5：运行聚焦测试和库存服务回归**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=FinPurchaseServiceImplTest,FinSaleRecordServiceImplTest,FinStockLedgerServiceImplTest test`

Expected: PASS，测试明确断言赠品进入实物库存但不进入采购/销售金额。

- [ ] **Step 6：报告、暂存检查并提交**

Commit: `fix(finance): include gifts in stock movements`

### Task 4：重建租户安全、可追溯的每日快照

**Files:**
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IStockSnapshotService.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StockSnapshotServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinStockLedgerMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinStockLedgerMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/StockSnapshotServiceImplTest.java`
- Create: `scripts/stock-snapshot-rebuild-contract.test.mjs`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-4-completion.md`

- [ ] **Step 1：对 `rebuildDailySnapshot` 及调度调用者运行 impact**

- [ ] **Step 2：写失败测试**

覆盖租户隔离、无流水日沿用上一日结存、跨日补建、当天采购普通 10 + 赠品 2 入库、销售 8 + 赠品 2 出库、冲销分类，以及不能用当前 position 倒填历史所有日期。

- [ ] **Step 3：观察失败**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=StockSnapshotServiceImplTest test`

- [ ] **Step 4：实现按历史流水顺序补建**

接口以 `tenantId, snapshotDate, deptId` 为最小执行单元；批量任务先按 tenantId、deptId、productId 升序。期初取 D-1 期末或 D 日第一笔流水的 beforeQuantity，期末由期初加分类净变动计算；仅当天快照允许与可信当前结存做交叉校验，历史日期不能使用当前结存作为 closing。

- [ ] **Step 5：运行测试**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=StockSnapshotServiceImplTest,FinStockLedgerMapperContractTest test && cd ../.. && node --test scripts/stock-snapshot-rebuild-contract.test.mjs`

- [ ] **Step 6：报告、暂存检查并提交**

Commit: `feat(finance): rebuild tenant-safe stock snapshots`

### Task 5：实现只读库存健康检查与存量对账

**Files:**
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockReconciliationRowVO.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/StockHealthMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockHealthMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StockHealthServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/StockHealthServiceImplTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-5-completion.md`

- [ ] **Step 1：对现有 StockHealth 符号运行 impact**

- [ ] **Step 2：写失败测试**

覆盖 `POSITION_WITHOUT_LEDGER`、`LEDGER_POSITION_MISMATCH`、`SNAPSHOT_EQUATION_MISMATCH`、`LATEST_SNAPSHOT_MISMATCH`，并验证两个租户使用相同部门商品不会互相聚合。

- [ ] **Step 3：观察失败并实现只读查询**

所有查询接收 `tenantId` 和已授权 `deptIds`；返回期望数量、实际数量、差额、异常代码和安全说明。服务不得 UPDATE 或自动修复。

- [ ] **Step 4：运行测试**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=StockHealthServiceImplTest test`

- [ ] **Step 5：报告、暂存检查并提交**

Commit: `feat(finance): add stock reconciliation checks`

### Task 6：实现经营库存报表查询模型和 Mapper

**Files:**
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockReportQuery.java`
- Replace: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockReportVO.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockReportSummaryVO.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockReportItemVO.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockLedgerRowVO.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/StockReportMapper.java`
- Create: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockReportMapper.xml`
- Create: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/mapper/StockReportMapperContractTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-6-completion.md`

- [ ] **Step 1：对现有 `StockReportVO` 和 `getStockReport` 运行 impact**

- [ ] **Step 2：写 Mapper 契约失败测试**

断言 summary、page、ledger 三组 SQL 都含 tenantId 和 deptIds；日期使用半开区间或 DATE 参数而非 `DATE(create_time)` 破坏索引；普通进货和赠品进货均来自 `PURCHASE_IN/PURCHASE_REVERSE` 流水；普通销售和销售赠品均来自 `SALE_OUT/SALE_REVERSE` 流水，不能再次 JOIN 单据数量重复计算。

- [ ] **Step 3：定义查询类型**

`StockReportQuery` 包含 `deptIds,startDate,endDate,keyword,status,pageNum,pageSize`。页大小限定 1..200，日期闭区间最长 366 天。`StockReportItemVO` 按 tenant + dept + product 唯一，包含规格规定的数量、最近出入库时间、无出库天数、状态和对账状态。

- [ ] **Step 4：实现共享 SQL 口径**

用 `<sql id="AuthorizedStockBase">` 统一租户、部门、商品和日期条件；summary 从同一基础聚合派生。数量分类显式使用 change_type，避免简单按正负号把销售冲销误算成采购入库。

- [ ] **Step 5：运行契约测试**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=StockReportMapperContractTest test`

- [ ] **Step 6：报告、暂存检查并提交**

Commit: `feat(finance): add stock report query model`

### Task 7：实现报表服务、权限、分页、导出和对账接口

**Files:**
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReportController.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IFinanceReportService.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceReportServiceImpl.java`
- Create: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinanceStockReportServiceImplTest.java`
- Create: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/controller/FinanceReportControllerContractTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-7-completion.md`

- [ ] **Step 1：对 Controller、接口和 `getStockReport` 运行 impact**

- [ ] **Step 2：写授权失败测试**

覆盖请求部门与授权部门取交集、交集为空失败、tenant 缺失失败、非法日期和页大小失败。Controller 契约断言查看、导出、对账分别使用 `finance:report:stock`、`finance:report:stock:export`、`finance:stock:reconciliation`。

- [ ] **Step 3：观察失败**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=FinanceStockReportServiceImplTest,FinanceReportControllerContractTest test`

- [ ] **Step 4：实现服务边界**

从 `TenantContext.getTenantId()` 获取租户，从既有数据范围服务取得授权部门。禁止只依赖前端隐藏或 MyBatis 租户插件。接口失败返回安全业务信息，不暴露 SQL。导出复用同一 query normalization 方法和 Mapper 口径。

- [ ] **Step 5：运行报表与既有财务回归**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=FinanceStockReportServiceImplTest,FinanceReportControllerContractTest,FinanceReportServiceImplTest test`

- [ ] **Step 6：报告、暂存检查并提交**

Commit: `feat(finance): expose authorized stock reports`

### Task 8：实现 PC 库存报表和流水下钻

**Files:**
- Create: `junsong-ui-v3/src/api/finance/stockreport.ts`
- Replace: `junsong-ui-v3/src/views/finance/report/stock.vue`
- Create: `junsong-ui-v3/src/views/finance/report/components/StockLedgerDrawer.vue`
- Create: `scripts/finance-stock-report-ui.test.mjs`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-8-completion.md`

- [ ] **Step 1：对 `StockReport` 页面和现有报表 API 消费者运行 impact**

- [ ] **Step 2：写 UI 契约失败测试**

断言页面不再出现“暂未开放”；调用 summary/page/ledger；展示期初、采购净入库、销售净出库、期末和异常指标；赠品只通过后端库存流水进入数量，不在前端二次拼算；接口失败不显示零值假数据；导出按钮受独立权限控制。

- [ ] **Step 3：观察失败**

Run: `node --test scripts/finance-stock-report-ui.test.mjs`

- [ ] **Step 4：实现页面**

查询栏包含授权门店、日期、商品和状态。表格按门店 + 商品分页；点击行打开流水抽屉；状态使用明确标签；口径说明写明“进货赠品计入入库数量，销售赠品计入出库数量，但赠品不计采购/销售金额”。错误时清空旧数据并展示服务端安全原因。

- [ ] **Step 5：运行 UI 契约和生产构建**

Run: `node --test scripts/finance-stock-report-ui.test.mjs && cd junsong-ui-v3 && npm run build`

Expected: PASS；构建可有既有依赖警告，但不得有 TypeScript 或打包错误。

- [ ] **Step 6：报告、暂存检查并提交**

Commit: `feat(ui): open stock operation report`

### Task 9：补齐菜单、导出权限、真实数据验收和发布门禁

**Files:**
- Create: `sql/finance_stock_report_menu.sql`
- Create: `scripts/finance-stock-report-acceptance.test.mjs`
- Create: `docs/finance-stock-report-operations.md`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-9-completion.md`

- [ ] **Step 1：写菜单和验收失败测试**

断言 SQL 幂等、utf8mb4、保留 `finance:report:stock` 并新增 export/reconciliation 权限；验收脚本验证租户、授权部门、赠品入库、赠品出库、恒等式、下钻和导出。

- [ ] **Step 2：观察失败并实现 SQL/验收脚本**

Run: `node --test scripts/finance-stock-report-acceptance.test.mjs`

- [ ] **Step 3：部署 SQL 并验证中文与 HEX**

Run: `bin/deploy-sql.sh DEV sql/finance_stock_report_menu.sql`

Expected: 重复执行安全；菜单名称、权限和 `HEX(menu_name)` 正确。

- [ ] **Step 4：构造真实赠品验收数据**

在独立测试门店创建进货普通 10 + 赠品 2、销售 8 + 赠品 2。预期采购净入库 12、销售净出库 10、期末 2；采购赠品金额和销售赠品收入均为 0。修改赠品数量后验证仅生成差额流水，删除后验证反向流水。

- [ ] **Step 5：运行完整第一期回归**

```bash
cd junsong-modules/junsong-finance && mvn test
cd ../../../junsong-ui-v3 && npm run build
cd .. && node --test scripts/finance-stock-report-*.test.mjs scripts/stock-*.test.mjs
```

- [ ] **Step 6：主复核 Agent 执行发布门禁**

主复核必须检查 9 份完成报告、复跑聚焦测试、调用真实 API、对账 SQL、菜单 HEX、暂存影响和独立代码审查。任何 Critical/Important 未解决则输出 `CHANGES_REQUIRED`，不得开放菜单。

- [ ] **Step 7：提交第一期验收**

Commit: `feat(finance): complete stock report phase one`

---

## 第二期：移动加权成本与毛利核对

第二期只能在第一期 Task 9 获得主复核 `APPROVED` 后开始。

### Task 10：设计并迁移库存成本层

**Files:**
- Create: `sql/finance_stock_cost_layer.sql`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinStockCostLayer.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinStockCostLayerMapper.java`
- Create: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinStockCostLayerMapper.xml`
- Create: `scripts/finance-stock-cost-layer.test.mjs`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-10-completion.md`

- [ ] **Step 1：写失败契约测试**

断言成本层按 tenant + dept + product 唯一；成本流水记录来源、原流水、数量、单位成本 6 位、金额 2 位、会计期间、调整原因、操作者和版本；不覆盖第一期库存流水。

- [ ] **Step 2：观察失败、实现幂等迁移并重复执行**

- [ ] **Step 3：运行测试和对账输出**

Run: `node --test scripts/finance-stock-cost-layer.test.mjs`

- [ ] **Step 4：报告、暂存检查并提交**

Commit: `feat(finance): add stock cost layer schema`

### Task 11：实现移动加权成本、销售成本固化和赠品成本

**Files:**
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IStockCostService.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StockCostServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinStockLedgerServiceImpl.java`
- Create: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/StockCostServiceImplTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-11-completion.md`

- [ ] **Step 1：对库存写入链路运行 impact 并取得高风险确认**

- [ ] **Step 2：写移动加权失败测试**

覆盖普通采购与赠品同单：普通 10 件单价 20，赠品 2 件金额 0，入库总数量 12、总金额 200、新平均成本 `16.666667`；销售 8 + 赠品 2 共出库 10，销售成本按 10 件固化，赠品也消耗成本但不产生收入。覆盖舍入、负库存策略、销售冲销按原成本回补、采购冲销和重复请求。

- [ ] **Step 3：观察失败并实现**

所有成本读写与 position 行使用相同租户键和确定锁顺序。`BigDecimal` 中间单位成本 scale 6，最终金额 scale 2，均 `HALF_UP`。销售成本取出库瞬间固化成本，禁止读取当前商品采购价。

- [ ] **Step 4：运行测试**

Run: `cd junsong-modules/junsong-finance && mvn -Dtest=StockCostServiceImplTest,FinStockLedgerServiceImplTest,FinPurchaseServiceImplTest,FinSaleRecordServiceImplTest test`

- [ ] **Step 5：报告、暂存检查并提交**

Commit: `feat(finance): calculate moving average stock cost`

### Task 12：增加财务库存报表与期间控制

**Files:**
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockValueReportVO.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/StockReportMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/StockReportMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceReportServiceImpl.java`
- Modify: `junsong-ui-v3/src/views/finance/report/stock.vue`
- Create: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/StockValueReportServiceTest.java`
- Report: `docs/superpowers/reports/2026-07-12-stock-report-task-12-completion.md`

- [ ] **Step 1：运行 impact 并写失败测试**

覆盖期初金额 + 入库金额 - 销售成本 + 调整 = 期末金额；销售收入 - 销售成本 = 毛利；赠品入库影响数量和平均成本，销售赠品影响销售成本但不影响收入；LOCKED/已结转期间拒绝回写。

- [ ] **Step 2：实现查询与期间控制**

ACTIVE 期间允许有权限调整；锁定期间差异只能在当前 ACTIVE 期间生成有原因的调整流水。前端只有服务端返回 `costReady=true` 时展示金额和毛利，禁止用零值伪装未完成成本。

- [ ] **Step 3：运行第二期回归和构建**

```bash
cd junsong-modules/junsong-finance && mvn test
cd ../../../junsong-ui-v3 && npm run build
```

- [ ] **Step 4：执行独立审查和真实对账**

抽取至少三个包含赠品、修改、冲销和跨期间场景，与采购金额、销售收入、库存成本流水及会计期间逐笔核对。

- [ ] **Step 5：主复核 Agent 审核报告并提交**

Commit: `feat(finance): complete valued stock reporting`

---

## 最终完成条件

- 第一、二期各自独立发布，第一期不得提前展示库存金额和毛利。
- 12 份完成报告齐全，每份均有证据而非结论性描述。
- 进货普通数量和赠品数量都进入库存增加；销售数量和赠品数量都进入库存核减。
- 赠品不进入采购金额或销售收入，但第二期必须消耗或摊薄真实库存成本。
- 所有业务读写按当前租户和授权部门失败关闭。
- 流水、结存、快照、报表及成本恒等式全部通过。
- 主复核 Agent 对最终版本给出 `APPROVED`，独立审查无 Critical/Important。
