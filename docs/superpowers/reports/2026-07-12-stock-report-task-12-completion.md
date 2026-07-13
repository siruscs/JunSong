# Task 12 完成报告：库存价值报表

## 概述
实现基于移动加权平均法的库存价值报表，提供金额、毛利和期间控制功能。

## 交付物
- `StockValueReportVO.java` / `StockValueReportItemVO.java`：价值报表 VO
- `StockReportMapper.java` 修改：新增 `selectStockValueSummary`、`selectStockValueItems`、`existsCostLayerForTenant`、`countStockProductsWithoutCostLayer`
- `FinanceReportServiceImpl.java` 修改：`getStockValueReport`、`createCostAdjustment`、`resolvePeriodStatus`
- `StockCostServiceImpl.java` 修改：`applyCostAdjustment` 成本调整
- `FinanceReportController.java` 修改：`POST /report/stock/value`（读）、`POST /report/stock/cost-adjustment`（写）
- `stock.vue` 修改：标签页切换（数量/价值）、costReady 门禁、期间状态指示器
- `sql/finance_stock_cost_adjust_menu.sql`：`finance:stock:costAdjust` 菜单权限注册

## costReady 门禁
1. `existsCostLayerForTenant`：检查是否存在至少一条成本层记录
2. `countStockProductsWithoutCostLayer`：检查授权范围内是否有库存流水但缺少成本层的商品
3. `costReady = hasAnyCostLayer && missingCount == 0`
4. `costReady=false` 时：所有金额字段为零、items 为空，禁止用零值伪装未完成成本

## 价值恒等式
```
期初金额 + 入库金额 - 销售成本 + 调整金额 = 期末金额
毛利 = 销售收入 - 销售成本
毛利率 = 毛利 / 销售收入 * 100
```

## 期间控制
- `ACTIVE`（'0'）：允许成本调整
- `LOCKED`（'1'）：禁止成本调整回写
- `CARRIED_FORWARD`（'2'）：禁止成本调整回写

## Finding 1-3 修复
1. **source_ledger_id 追溯**：成本服务调用传入 `ledgerId`（`fin_stock_ledger.ledger_id`）而非 `referenceId`；销售出库改为先写流水获取 ID，再固化成本并回填 `unit_cost`
2. **销售冲销 fail-closed**：找不到原 SALE_OUT 固化成本时抛出 `ServiceException`，防止数量回补但成本金额未回补
3. **costReady 覆盖校验**：从"存在任意成本层"改为"授权范围内所有有库存流水的商品都有成本层"

## 测试
- `StockValueReportServiceTest.java`：13 项单元测试
  - costReady 门禁（无成本层/全覆盖/部分初始化）
  - 价值恒等式
  - 毛利计算
  - 赠品入库/销售规则
  - 期间控制（LOCKED/CARRIED_FORWARD 拒绝调整）
  - 租户隔离
- 焦点测试套件：72 tests, 0 failures
- 前端构建：成功

## 提交
- `c6f3ab91` feat(finance): complete valued stock reporting
- Finding 1-5 修复提交（本次）

## 安全约束
- 租户隔离：所有查询按 `tenant_id + dept_id` 过滤
- 期间控制：LOCKED/CARRIED_FORWARD 期间拒绝成本调整回写
- 权限分离：读权限 `finance:report:stock`，写权限 `finance:stock:costAdjust`
- fail-closed：缺租户上下文、缺原固化成本、缺成本层覆盖时均拒绝操作
