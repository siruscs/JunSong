# Task 11 完成报告：移动加权平均成本计算

## 概述
实现移动加权平均法的成本计价服务，在采购入库和销售出库时自动更新成本层。

## 交付物
- `IStockCostService.java`：成本计价服务接口（5 个方法）
- `StockCostServiceImpl.java`：成本计价服务实现
- `FinStockLedgerServiceImpl.java` 修改：在采购/销售对账时联动成本服务
- `FinStockLedgerMapper.java` 修改：新增 `selectSaleOutUnitCost` 查询原固化成本

## 核心算法
- **采购入库**：`新平均成本 = (入库前库存金额 + 本次入库金额) / 入库后数量`；赠品计入数量不计金额，摊薄平均成本
- **销售出库**：按出库瞬间平均成本固化销售成本；出库后平均成本不变
- **销售冲销**：按原 SALE_OUT 固化的单位成本反向恢复库存金额，重新计算平均成本
- **采购冲销**：按当前平均成本逆转数量和金额
- BigDecimal：单位成本 scale 6，金额 scale 2，HALF_UP

## 联动方式
- `FinStockLedgerServiceImpl` 通过 `@Autowired(required = false) IStockCostService` 注入成本服务
- Phase 1 测试（未注入成本服务）仍正常通过，保证向后兼容
- `source_ledger_id` 传入 `fin_stock_ledger.ledger_id`（Finding 1 修复后），保证成本流水可追溯

## 测试
- `StockCostServiceImplTest.java`：14 项单元测试
  - 采购入库计算平均成本（含赠品摊薄）
  - 销售出库固化成本
  - 销售冲销按原成本回补
  - 采购冲销逆转
  - 并发安全（乐观锁）
  - 租户+门店+商品隔离
- 焦点测试套件：59 tests, 0 failures
- 全模块回归：507 tests, 0 failures（1 pre-existing unrelated error）

## 提交
- `1585df898` feat(finance): calculate moving average stock cost

## 安全约束
- 租户隔离：所有读写按 `tenant_id + dept_id + product_id`
- 行锁：`SELECT ... FOR UPDATE` 保证并发安全
- 乐观锁：`version` 字段防止更新丢失
- 成本固化：销售出库取出库瞬间成本，禁止用当前采购价回算历史成本
