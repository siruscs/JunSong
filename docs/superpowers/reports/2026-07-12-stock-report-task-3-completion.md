# Task 3 完成报告：采购与销售赠品库存口径

日期：2026-07-12

## 任务目标

保证进货单普通数量与赠品数量共同增加实物库存，销售数量与赠品数量共同扣减实物库存；修改只写含赠品目标数量的差额流水，删除按含赠品净额反向恢复。赠品零价不得覆盖同商品正常采购单价，数量合计溢出必须在写流水前失败关闭。

## 实际修改文件

| 文件 | 职责 |
|---|---|
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinPurchaseServiceImpl.java` | 同商品所有明细数量继续共同入库；仅非赠品明细更新本期流水展示用采购单价，避免赠品零价覆盖正常单价。 |
| `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinSaleRecordServiceImpl.java` | 使用 `Math.addExact` 合计销售与赠品数量，溢出时返回安全业务错误；库存 apply/reverse 前校验当前部门所有权。 |
| `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinPurchaseServiceImplTest.java` | 验证普通 10 + 赠品 2 入库、赠品 2→5 只追加 +3、删除反向 -15、赠品不覆盖采购单价。 |
| `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinSaleRecordServiceImplTest.java` | 验证销售 8 + 赠品 2 出库、赠品 2→5 只追加 -3、删除回补 13、整数溢出失败关闭。 |

## 关键设计决定

1. 实物数量以库存流水目标净额为准。采购按同商品全部明细 `quantity` 合并，不按 `isGift` 过滤；销售按 `saleQuantity + giftQuantity` 合并。
2. 修改和删除继续复用 Task 2 的来源净额差异模型，不修改或删除历史流水。
3. 第一期不实现移动加权成本。当前 `unitCost` 仅避免被赠品零价覆盖；同商品多采购价的正式成本口径留在计划 Task 10-12。
4. 销售收入和采购金额计算逻辑未改动；本任务只处理实物库存。现有采购计算明确赠品金额为零，销售金额仍由销售记录金额字段决定。
5. `true/yes/1` 在采购金额计算前统一标准化，避免合法赠品别名进入采购金额；采购总数量及同商品目标数量均使用 `Math.addExact`。
6. 非管理员只能对当前登录部门的采购/销售库存执行 apply/reverse；部门缺失或不一致均失败关闭。

## GitNexus 影响分析

已分别对 `applyPurchaseStockIn`、`reversePurchaseStock`、`applySaleStockOut`、`reverseSaleStock` 执行 upstream impact。GitNexus 因本地 LadybugDB 文件版本 42 与运行时版本 41 不兼容，返回风险 `UNKNOWN`，无法提供图谱计数。

人工调用点复核结果：

- 采购两个方法只由 `FinPurchaseServiceImpl` 的新增、更新和删除流程调用，测试调用集中在 `FinPurchaseServiceImplTest`。
- 销售两个方法只由 `FinSaleRecordServiceImpl` 的新增、更新和删除流程调用，测试调用集中在 `FinSaleRecordServiceImplTest`。
- 未发现跨模块直接调用。人工评估为中等业务风险，影响采购入库、销售出库及其冲销流程。

## TDD 证据

RED 命令：

```bash
cd junsong-modules/junsong-finance
mvn -Dtest=FinPurchaseServiceImplTest,FinSaleRecordServiceImplTest test
```

RED 结果：23 个测试中 2 个预期失败。

- 采购赠品场景期望正常单价 `3.00`，实际被覆盖为 `0.00`。
- 销售数量合计溢出期望安全专用错误，实际得到泛化的“目标数量不能为负数”。

GREEN 命令：

```bash
cd junsong-modules/junsong-finance
mvn -Dtest=FinPurchaseServiceImplTest,FinSaleRecordServiceImplTest,FinStockLedgerServiceImplTest test
```

独立审查首次给出 3 个 Important：采购数量溢出、缺少部门所有权校验、赠品别名标准化晚于金额计算。针对这些问题补充第二轮 RED，4 个测试按预期失败；修复后再次运行 GREEN。

第二次独立复审指出授权范围不能收窄为当前门店，且管理员也必须拒绝空门店。随后补充第三轮 RED：授权其他门店允许、未授权门店拒绝、管理员空门店拒绝；实现通过 `RemoteUserService` 读取授权门店集合，远程失败时仅退化到当前门店，不扩大权限。

最终 GREEN 结果：45 tests，0 failures，0 errors，BUILD SUCCESS。

## 数据库迁移与对账

不涉及数据库结构或数据迁移。本任务使用 Task 1/2 已完成的租户安全库存表和差额流水模型。

## 边界自查

- 权限：未新增接口，沿用采购/销售 Controller 既有权限。
- 租户：调用 Task 2 租户化库存接口，租户来自 `TenantContext.getTenantId()`。
- 部门：apply/reverse 显式校验管理员或当前登录部门；越权部门测试证明写流水前失败关闭。
- 并发：沿用 `(tenantId, deptId, productId)` 当前结存行锁和同事务差额对账。
- 财务：赠品计入实物数量但不改变采购金额或销售收入；正式成本计价不在第一期提前实现。
- 原子性：库存流水与业务单据写入仍位于原事务边界，异常回滚。

## 已知限制与后续风险

- 同商品多条非赠品明细使用不同单价时，第一期流水 `unitCost` 仍不是移动加权成本；Task 10-12 必须建立正式成本层。
- GitNexus 图谱不可用，需要升级/重建兼容索引后补做 `detect_changes`。
- 采购与销售 Service 暂时各自保留授权部门测试覆盖字段并复制授权加载逻辑；独立审查定级为 Minor，可在后续统一授权组件任务中消除，不阻断本任务。

## 独立审查结论

首次审查：`CHANGES_REQUIRED`，3 个 Important；已全部修复。

第二次审查：`CHANGES_REQUIRED`，1 个 Important；已修复授权部门集合和管理员空部门问题。

最终复审：未发现 Critical/Important，结论 `APPROVED`。仅记录授权加载逻辑重复和测试覆盖字段两个 Minor。

## 提交与 staged 影响

提交 SHA 在主复核、暂存检查和提交后补入。预期 staged 范围仅为上述 4 个 Java 文件和本报告；预期影响采购入库、采购冲销、销售出库、销售冲销及对应测试，不影响报表查询或快照任务。
