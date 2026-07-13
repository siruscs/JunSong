# Task 7 完成报告：报表服务、权限、分页、导出和对账接口

## 任务编号和目标

Task 7：实现报表服务、权限、分页、导出和对账接口。将库存报表从"暂不开放"状态开放为可查询、可分页、可导出、可对账的完整接口。

## 实际修改文件及其职责

| 文件 | 类型 | 职责 |
|------|------|------|
| `controller/FinanceReportController.java` | 修改 | 6 个库存端点：stock/summary/page/ledger/page/export/reconciliation，权限码分离 |
| `service/IFinanceReportService.java` | 修改 | 移除旧方法，新增 6 个库存报表方法 |
| `service/impl/FinanceReportServiceImpl.java` | 修改 | 实现 6 个方法 + applyStockDataScope + validateStockReportRequest |
| `test/.../FinanceStockReportServiceImplTest.java` | 新建 | 12 个测试覆盖权限、校验、数据正确性 |
| `test/.../FinanceReportControllerContractTest.java` | 新建 | 8 个契约测试断言权限码 |

## 关键设计决定

1. **applyStockDataScope**：新建独立的数据权限方法，交集为空时 fail-closed 抛 ServiceException（不同于 applyDataScope 回退到授权部门）。
2. **validateStockReportRequest**：包级可见静态方法，校验租户/参数/pageSize(1-200)/日期区间(≤366天)，将 IllegalArgumentException 转为安全 ServiceException。
3. **导出**：创建查询副本设置 pageSize=200，避免修改原始查询对象。
4. **流水下钻**：服务层做内存分页（Mapper 不支持分页参数）。
5. **对账**：委托给 IStockHealthService.reconcileStock。
6. **可测试性**：校验逻辑抽取为包级可见方法，绕过 TenantContext 默认值问题。

## 已执行的测试命令和结果

| 命令 | 结果 |
|------|------|
| `mvn -Dtest=FinanceStockReportServiceImplTest,FinanceReportControllerContractTest test` | 20 测试通过 |
| `mvn -Dtest=FinanceReportServiceImplTest test` | 15 测试通过（回归） |
| `mvn compile` | BUILD SUCCESS |

## 数据库迁移

不涉及。

## 权限、租户、部门、并发和财务边界自查

- [x] 权限：stock=finance:report:stock, export=finance:report:stock:export, reconciliation=finance:stock:reconciliation
- [x] 租户：从 TenantContext.getTenantId() 获取，null 时 fail-closed
- [x] 部门：交集为空时 fail-closed
- [x] 并发：只读查询，无并发风险
- [x] 财务边界：不修改数据，返回安全业务消息

## 已知限制和后续风险

1. 流水下钻在服务层做内存分页，大数据量时需改为 Mapper 分页。
2. 导出目前返回最多 200 条，超大导出需后续支持流式导出。
3. SecurityUtils.isAdmin() 在单元测试中难以模拟，通过包级可见方法绕过。
