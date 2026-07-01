-- ============================================================
-- 财务业务编号唯一索引（幂等）
-- 确保 fin_product.product_code 和 fin_supplier.supplier_code 有唯一约束，
-- 使服务层的 DuplicateKeyException 重试机制生效。
-- 其他业务表（fin_expense/fin_advance/fin_sale_record/fin_sale_payment/
-- fin_purchase/fin_investor_payment/fin_cost_accounting）已有唯一索引。
-- ============================================================

-- 1. fin_product.product_code
ALTER TABLE `fin_product` DROP INDEX `idx_product_code`;
ALTER TABLE `fin_product` ADD UNIQUE INDEX `uk_product_code` (`product_code`);

-- 2. fin_supplier.supplier_code
ALTER TABLE `fin_supplier` DROP INDEX `idx_supplier_code`;
ALTER TABLE `fin_supplier` ADD UNIQUE INDEX `uk_supplier_code` (`supplier_code`);
