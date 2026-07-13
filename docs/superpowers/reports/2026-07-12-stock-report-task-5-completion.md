# Task 5 完成报告：只读库存健康检查与存量对账

## 任务编号和目标

Task 5：实现只读库存健康检查与存量对账。为库存报表开放前提供租户安全的只读对账能力，覆盖四类异常检测，禁止自动修复。

## 实际修改文件及其职责

| 文件 | 类型 | 职责 |
|------|------|------|
| `domain/vo/StockReconciliationRowVO.java` | 新建 | 对账明细行 VO：tenantId、deptId、deptName、productId、productName、expectedQuantity、actualQuantity、diffQuantity、anomalyCode、safetyNote |
| `domain/vo/StockReconciliationResultVO.java` | 新建 | 对账结果 VO：rows、totalAnomalyCount、anomalyCounts（Map）、status |
| `mapper/StockHealthMapper.java` | 修改 | 6 个现有计数方法增加 tenantId+deptIds 参数；新增 4 个只读对账查询方法 |
| `mapper/finance/StockHealthMapper.xml` | 修改 | 所有查询按 tenant_id 过滤；4 个新 SELECT 查询（只读，无 UPDATE/DELETE/INSERT） |
| `service/IStockHealthService.java` | 修改 | checkHealth 增加 tenantId+deptIds 参数；新增 reconcileStock 方法 |
| `service/impl/StockHealthServiceImpl.java` | 修改 | 实现租户安全的 checkHealth 和 reconcileStock；tenantId 为 null 时 fail-closed |
| `controller/StockHealthController.java` | 修改 | 调用改为 checkHealth(TenantContext.getTenantId(), null) |
| `controller/FinanceActionCenterInnerController.java` | 修改 | 增加 TenantContext null 守卫 |
| `test/.../StockHealthServiceImplTest.java` | 修改 | 10 个现有测试更新签名；新增 9 个测试覆盖四类异常、租户隔离、fail-closed、只读验证 |

## 关键设计决定

1. **所有查询租户隔离**：每个 SQL 均显式 `tenant_id = #{tenantId}`，不再全量扫描。
2. **只读对账**：reconcileStock 仅调用 SELECT 查询，不修改任何数据。
3. **四类异常代码**：POSITION_WITHOUT_LEDGER、LEDGER_POSITION_MISMATCH、SNAPSHOT_EQUATION_MISMATCH、LATEST_SNAPSHOT_MISMATCH。
4. **fail-closed**：tenantId 为 null 时抛出 ServiceException，不返回任何数据。
5. **deptIds 可选**：null 或空时不按部门过滤（管理员全局视图），非空时 IN 过滤。

## GitNexus impact 结果

对 StockHealthMapper、StockHealthServiceImpl、IStockHealthService、StockHealthController、FinanceActionCenterInnerController 运行 impact。直接调用者：StockHealthController、FinanceActionCenterInnerController。风险等级：MEDIUM（签名变更影响现有调用者）。已通过更新调用者签名解决。

## 已执行的测试命令和结果

| 命令 | 结果 |
|------|------|
| `mvn -Dtest=StockHealthServiceImplTest test` | 19 测试通过，0 失败 |
| `mvn -Dtest=FinStockLedgerServiceImplTest test` | 14 测试通过，0 失败 |
| `mvn -Dtest=StockSnapshotServiceImplTest test` | 6 测试通过，0 失败 |

## 数据库迁移

不涉及。本任务仅修改查询逻辑，不涉及表结构变更。

## 权限、租户、部门、并发和财务边界自查

- [x] 权限：StockHealthController 保持 `finance:stock:health` 权限
- [x] 租户：所有查询显式 `tenant_id = #{tenantId}`，null 时 fail-closed
- [x] 部门：deptIds 非空时 IN 过滤
- [x] 并发：只读查询，无并发风险
- [x] 财务边界：只读对账，不修改任何财务数据

## 已知限制和后续风险

1. `findLatestSnapshotMismatch` 使用 `row_number()` 窗口函数，要求 MySQL 8.0+。
2. `findLedgerPositionMismatch` 使用 INNER JOIN 确保与 POSITION_WITHOUT_LEDGER 互斥，但若 position 表有而 ledger 表无记录，仅前者会报告。
3. 真实数据库验证留待 Task 9 发布门禁执行。
