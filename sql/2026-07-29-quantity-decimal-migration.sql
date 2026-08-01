SET NAMES utf8mb4;

-- ==========================================================================
-- 库存/销售/进货数量列 INT → DECIMAL(18,3) 迁移脚本
-- 背景：进货管理与销售管理需要支持小数数量（0.5kg / 1.5 盒等称重/拆分场景）。
-- 原则：
--   1. 非破坏、可重复执行：每次 ALTER 前先查 information_schema 确认当前列类型确实是 INT。
--   2. 精确 3 位小数：DECIMAL(18,3)，足够覆盖"称重到克"、"按 0.001 拆分"的业务精度；
--      仍保留金额 2 位小数（与现行口径一致）。
--   3. 默认值与原 INT 保持一致（DEFAULT 0），避免破坏历史 INSERT 口径。
--   4. 对账输出：执行后打印每列的最终类型，便于 DBA 复核。
-- 建议：先在 DEV 跑。PROD 执行前请先备份 affected 表。
-- ==========================================================================

-- ==========================================================================
-- 辅助存储过程：仅当列为 INT / BIGINT 整型时才改为 DECIMAL(18,3)
-- ==========================================================================
DROP PROCEDURE IF EXISTS alter_int_col_to_decimal3;
DELIMITER //
CREATE PROCEDURE alter_int_col_to_decimal3(
    IN p_table_name  VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_is_nullable VARCHAR(8),   -- 'YES' / 'NO'
    IN p_default     VARCHAR(32)   -- '0' / '' / NULL
)
BEGIN
    DECLARE v_exists   INT DEFAULT 0;
    DECLARE v_curr_type VARCHAR(64) DEFAULT '';
    DECLARE v_nullable  VARCHAR(8)  DEFAULT '';
    DECLARE v_dflt      VARCHAR(64) DEFAULT '';
    DECLARE v_sql       LONGTEXT;

    SELECT COUNT(*) INTO v_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = p_table_name
      AND COLUMN_NAME  = p_column_name;

    IF v_exists = 0 THEN
        SELECT CONCAT('SKIP: column not exists ', p_table_name, '.', p_column_name) AS info;
    ELSE
        SELECT COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT, '<NULL>')
          INTO v_curr_type, v_nullable, v_dflt
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table_name
          AND COLUMN_NAME  = p_column_name;

        -- 只有当前为 INT / BIGINT / SMALLINT / TINYINT 才做转换；已是 DECIMAL 的直接跳过
        IF v_curr_type REGEXP '^(big|small|tiny)?int([(][0-9]+[)])?( unsigned)?$' THEN
            SET v_sql = CONCAT(
                'ALTER TABLE `', p_table_name, '` ',
                'MODIFY COLUMN `', p_column_name, '` DECIMAL(18,3) ',
                IF(p_is_nullable = 'NO', 'NOT NULL', 'NULL'),
                IF(p_default IS NULL OR p_default = '',
                   '',
                   CONCAT(' DEFAULT ', p_default)),
                ' COMMENT ''', p_column_name, ' (支持3位小数)'''
            );
            SELECT CONCAT('EXEC: ', v_sql) AS info;
            SET @_alter_decimal3_sql = v_sql;
            PREPARE stmt FROM @_alter_decimal3_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        ELSE
            SELECT CONCAT('SKIP: already non-INT (', v_curr_type, ') on ',
                          p_table_name, '.', p_column_name) AS info;
        END IF;
    END IF;
END //
DELIMITER ;

-- ==========================================================================
-- Step 1: 进货单表
-- ==========================================================================
CALL alter_int_col_to_decimal3('fin_purchase',          'total_quantity',  'YES', '0');

-- Step 2: 进货单明细表
CALL alter_int_col_to_decimal3('fin_purchase_detail',   'quantity',        'NO',  '0');

-- Step 3: 销售记录表
CALL alter_int_col_to_decimal3('fin_sale_record',       'sale_quantity',   'NO',  '0');
CALL alter_int_col_to_decimal3('fin_sale_record',       'gift_quantity',   'NO',  '0');
CALL alter_int_col_to_decimal3('fin_sale_record',       'total_quantity',  'NO',  '0');

-- Step 4: 库存流水表
CALL alter_int_col_to_decimal3('fin_stock_ledger',      'change_quantity', 'NO',  '0');
CALL alter_int_col_to_decimal3('fin_stock_ledger',      'before_quantity', 'NO',  '0');
CALL alter_int_col_to_decimal3('fin_stock_ledger',      'after_quantity',  'NO',  '0');

-- Step 5: 库存当前结存表
CALL alter_int_col_to_decimal3('fin_stock_position',    'quantity',        'NO',  '0');

-- Step 6: 库存快照表
CALL alter_int_col_to_decimal3('fin_stock_snapshot',    'quantity',        'NO',  '0');
CALL alter_int_col_to_decimal3('fin_stock_snapshot',    'opening_quantity','NO',  '0');
CALL alter_int_col_to_decimal3('fin_stock_snapshot',    'in_quantity',     'NO',  '0');
CALL alter_int_col_to_decimal3('fin_stock_snapshot',    'out_quantity',    'NO',  '0');

-- Step 7: 库存成本层表
CALL alter_int_col_to_decimal3('fin_stock_cost_layer',  'stock_quantity',  'NO',  '0');

-- Step 8: 库存成本流水表
CALL alter_int_col_to_decimal3('fin_stock_cost_ledger', 'quantity',        'NO',  '0');

-- Step 9: 盘点行表（8 个数量列）
CALL alter_int_col_to_decimal3('finance_stocktake_item','expected_quantity',            'NO',  '0');
CALL alter_int_col_to_decimal3('finance_stocktake_item','movement_quantity_after_freeze','NO', '0');
CALL alter_int_col_to_decimal3('finance_stocktake_item','adjusted_expected_quantity',   'YES', NULL);
CALL alter_int_col_to_decimal3('finance_stocktake_item','actual_quantity',              'YES', NULL);
CALL alter_int_col_to_decimal3('finance_stocktake_item','recount_quantity',             'YES', NULL);
CALL alter_int_col_to_decimal3('finance_stocktake_item','final_quantity',               'YES', NULL);
CALL alter_int_col_to_decimal3('finance_stocktake_item','variance_quantity',            'YES', NULL);

-- Step 10: 期初批次行表 quantity 已是 DECIMAL(18,2)，升级到 DECIMAL(18,3) 与其它列对齐
-- （这里复用 helper，直接从 DECIMAL(18,2) → DECIMAL(18,3) 也安全，helper 会 SKIP；
--   所以直接单独写一条 ALTER，判断现在不是 (18,3) 才改。）
SET @_upg_qty_sql = (SELECT IF(
    COLUMN_TYPE = 'decimal(18,2)',
    CONCAT('ALTER TABLE `fin_stock_init_item` MODIFY COLUMN `quantity` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT ''期初数量（3位小数精度）'''),
    'SELECT ''SKIP: fin_stock_init_item.quantity already target type'' AS info'
) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_init_item' AND COLUMN_NAME = 'quantity');
PREPARE stmt FROM @_upg_qty_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DROP PROCEDURE IF EXISTS alter_int_col_to_decimal3;

-- ==========================================================================
-- 对账输出：最终列类型核查
-- ==========================================================================
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    IFNULL(COLUMN_DEFAULT, '<NULL>') AS COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
      (TABLE_NAME = 'fin_purchase'             AND COLUMN_NAME = 'total_quantity')
   OR (TABLE_NAME = 'fin_purchase_detail'      AND COLUMN_NAME = 'quantity')
   OR (TABLE_NAME = 'fin_sale_record'          AND COLUMN_NAME IN ('sale_quantity','gift_quantity','total_quantity'))
   OR (TABLE_NAME = 'fin_stock_ledger'         AND COLUMN_NAME IN ('change_quantity','before_quantity','after_quantity'))
   OR (TABLE_NAME = 'fin_stock_position'       AND COLUMN_NAME = 'quantity')
   OR (TABLE_NAME = 'fin_stock_snapshot'       AND COLUMN_NAME IN ('quantity','opening_quantity','in_quantity','out_quantity'))
   OR (TABLE_NAME = 'fin_stock_cost_layer'     AND COLUMN_NAME = 'stock_quantity')
   OR (TABLE_NAME = 'fin_stock_cost_ledger'    AND COLUMN_NAME = 'quantity')
   OR (TABLE_NAME = 'finance_stocktake_item'   AND COLUMN_NAME IN ('expected_quantity','movement_quantity_after_freeze','adjusted_expected_quantity','actual_quantity','recount_quantity','final_quantity','variance_quantity'))
   OR (TABLE_NAME = 'fin_stock_init_item'      AND COLUMN_NAME = 'quantity')
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- ==========================================================================
-- 对账输出 2：现有数量数据的小数位分布
-- （帮助 DBA 判断历史数据是否已含有隐式小数或被 INT 截断导致的误差）
-- ==========================================================================
SELECT 'fin_sale_record.sale_quantity' AS col,
       COUNT(*)                           AS total_rows,
       SUM(CASE WHEN sale_quantity % 1 <> 0 THEN 1 ELSE 0 END) AS with_decimal,
       MIN(sale_quantity)                 AS min_qty,
       MAX(sale_quantity)                 AS max_qty
FROM fin_sale_record
UNION ALL
SELECT 'fin_purchase_detail.quantity',
       COUNT(*),
       SUM(CASE WHEN quantity % 1 <> 0 THEN 1 ELSE 0 END),
       MIN(quantity), MAX(quantity)
FROM fin_purchase_detail
UNION ALL
SELECT 'fin_stock_position.quantity',
       COUNT(*),
       SUM(CASE WHEN quantity % 1 <> 0 THEN 1 ELSE 0 END),
       MIN(quantity), MAX(quantity)
FROM fin_stock_position;
