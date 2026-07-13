# Task 10 完成报告：库存成本层 Schema

## 概述
建立移动加权平均法的底层数据结构，为第二期库存价值报表提供成本计价基础。

## 交付物
- `fin_stock_cost_layer` 表：租户+门店+商品维度的成本层，记录 `avg_unit_cost`、`stock_quantity`、`stock_amount`、`version`（乐观锁）
- `fin_stock_cost_ledger` 表：成本变动流水，记录 `source_type`、`source_ledger_id`（关联 `fin_stock_ledger.ledger_id`）、`cost_change_type`、`quantity`、`unit_cost`、`amount`、`period_id`、`adjust_reason`
- `FinStockCostLayer.java` / `FinStockCostLedger.java` 领域对象
- `FinStockCostLayerMapper.java` + XML：`insertCostLayerIfAbsent`、`selectCostLayerForUpdate`、`updateCostLayer`（乐观锁）、`insertCostLedger`
- `sql/finance_stock_cost_layer.sql`：可重复执行的 DDL

## 成本变动类型约定
| cost_change_type | 说明 | amount 符号 |
|---|---|---|
| COST_IN | 采购入库 | + |
| COST_REVERSE_IN | 销售冲销回补 | + |
| COST_OUT | 销售出库 | - |
| COST_REVERSE_OUT | 采购冲销 | - |
| COST_ADJUST | 成本调整 | 有符号 |

## 测试
- `scripts/finance-stock-cost-layer.test.mjs`：18 项契约测试通过（DDL 可重复执行、字段约束、索引、乐观锁版本字段）

## 提交
- `efc2baec6` feat(finance): add stock cost layer schema

## 安全约束
- 所有读写按 `tenant_id + dept_id + product_id` 隔离
- `source_ledger_id` 关联 `fin_stock_ledger.ledger_id`，保证成本流水可追溯到具体库存流水
- 乐观锁 `version` 字段防止并发更新丢失
