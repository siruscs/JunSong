SET NAMES utf8mb4;

-- ============================================================
-- 修复乐龄店石斛商品单位错别字：【升】→【斤】
--
-- 背景：乐龄店（dept_id=203）的石斛商品（product_id=35）最初单位误维护为"升"，
--      商品主表后已修正为"斤"，但 fin_purchase_detail 进货明细的历史快照仍为"升"。
--
-- 影响范围：
--   - fin_product（商品主表）：已是"斤"，无需更新
--   - fin_purchase_detail（进货明细）：3 条记录 unit='升' → '斤'
--   - 其他业务表（fin_sale_record/fin_stock_ledger/fin_stock_snapshot 等）：无 unit 字段，不受影响
--
-- 执行方式：mysql --default-character-set=utf8mb4 < 本文件
-- ============================================================

-- 1. 更新前诊断
SELECT '=== 更新前：石斛+升 记录 ===' AS info;
SELECT detail_id, purchase_id, product_id, product_name, unit
FROM fin_purchase_detail
WHERE product_name LIKE '%石斛%' AND unit = '升';

-- 2. 执行更新
START TRANSACTION;

UPDATE fin_purchase_detail
SET unit = '斤'
WHERE product_name LIKE '%石斛%' AND unit = '升';

SELECT CONCAT('fin_purchase_detail 更新行数: ', ROW_COUNT()) AS update_result;

-- 3. 验证：更新后石斛+升 记录应为0
SELECT '=== 验证：更新后石斛+升 记录数（应为0） ===' AS check_result;
SELECT COUNT(*) AS remaining FROM fin_purchase_detail WHERE product_name LIKE '%石斛%' AND unit = '升';

-- 4. 验证：更新后石斛+斤 记录
SELECT '=== 验证：更新后石斛+斤 记录 ===' AS check_result;
SELECT detail_id, purchase_id, product_id, product_name, unit
FROM fin_purchase_detail
WHERE product_name LIKE '%石斛%' AND unit = '斤';

COMMIT;
