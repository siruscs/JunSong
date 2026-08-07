SET NAMES utf8mb4;

-- 商品、供应商编码允许在同一租户的不同机构复用；同一机构内仍保持唯一。
-- 仅调整唯一性边界，不删除业务数据。执行前先查看重复检查结果。
SET @db = DATABASE();

SELECT 'product duplicate check' AS check_name, tenant_id, dept_id, product_code, COUNT(*) AS row_count
FROM fin_product WHERE del_flag = '0' AND product_code IS NOT NULL AND product_code <> ''
GROUP BY tenant_id, dept_id, product_code HAVING COUNT(*) > 1;
SELECT 'supplier duplicate check' AS check_name, tenant_id, dept_id, supplier_code, COUNT(*) AS row_count
FROM fin_supplier WHERE del_flag = '0' AND supplier_code IS NOT NULL AND supplier_code <> ''
GROUP BY tenant_id, dept_id, supplier_code HAVING COUNT(*) > 1;

SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@db AND table_name='fin_product' AND index_name='uk_product_code'), 'ALTER TABLE fin_product DROP INDEX uk_product_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@db AND table_name='fin_supplier' AND index_name='uk_supplier_code'), 'ALTER TABLE fin_supplier DROP INDEX uk_supplier_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@db AND table_name='fin_product' AND index_name='uk_product_code_scope'), 'SELECT 1', 'ALTER TABLE fin_product ADD UNIQUE INDEX uk_product_code_scope (tenant_id, dept_id, product_code)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@db AND table_name='fin_supplier' AND index_name='uk_supplier_code_scope'), 'SELECT 1', 'ALTER TABLE fin_supplier ADD UNIQUE INDEX uk_supplier_code_scope (tenant_id, dept_id, supplier_code)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SHOW INDEX FROM fin_product WHERE Key_name = 'uk_product_code_scope';
SHOW INDEX FROM fin_supplier WHERE Key_name = 'uk_supplier_code_scope';
