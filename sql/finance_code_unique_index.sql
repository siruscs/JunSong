-- =====================================================================
-- 财务模块编码唯一性加固
-- 1. 为 product_code、supplier_code 加唯一索引（先清重复再建）
-- 2. 补全 countToday 系列查询的 SQL 体（已在 Mapper XML 中修复）
-- 可重复执行
-- =====================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------
-- 1. fin_product.product_code 唯一索引
-- -----------------------------------------------------------------

-- 先检查是否有重复的 product_code（排除空值）
-- 如果有重复，先保留最新的一条，其余标记删除
UPDATE fin_product a
INNER JOIN (
    SELECT product_code, MAX(product_id) AS keep_id
    FROM fin_product
    WHERE del_flag = '0' AND product_code IS NOT NULL AND product_code != ''
    GROUP BY product_code
    HAVING COUNT(1) > 1
) b ON a.product_code = b.product_code AND a.product_id != b.keep_id
SET a.del_flag = '2'
WHERE a.del_flag = '0';

-- 创建唯一索引（幂等）
SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fin_product'
      AND INDEX_NAME = 'uk_product_code'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_product ADD UNIQUE INDEX uk_product_code (product_code)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------
-- 2. fin_supplier.supplier_code 唯一索引
-- -----------------------------------------------------------------

UPDATE fin_supplier a
INNER JOIN (
    SELECT supplier_code, MAX(supplier_id) AS keep_id
    FROM fin_supplier
    WHERE del_flag = '0' AND supplier_code IS NOT NULL AND supplier_code != ''
    GROUP BY supplier_code
    HAVING COUNT(1) > 1
) b ON a.supplier_code = b.supplier_code AND a.supplier_id != b.keep_id
SET a.del_flag = '2'
WHERE a.del_flag = '0';

SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fin_supplier'
      AND INDEX_NAME = 'uk_supplier_code'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_supplier ADD UNIQUE INDEX uk_supplier_code (supplier_code)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
