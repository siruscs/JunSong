SET NAMES utf8mb4;

-- ============================================================
-- 修复商品名称错别字：【太】→【肽】
--
-- 背景：维护商品时误将"肽"写成"太"，需修正商品主表及所有保存了商品名快照的业务表。
--
-- ⚠️ 重要：不是所有"太"都是错别字（如"太太口服液"中的"太"是正确的）。
--   本脚本采用"安全替换"策略：
--   1. 第 1 步：先查询所有包含"太"但不含"肽"的商品，人工确认哪些是错别字
--   2. 第 2 步：按确认后的 product_id 列表精确更新（避免误改正确商品名）
--
-- 执行方式：mysql --default-character-set=utf8mb4 < 本文件
-- ============================================================


-- ##########################################################
-- 第 1 步：诊断查询 —— 列出所有包含"太"的商品（供人工确认）
-- ##########################################################

-- 1.1 商品主表：含"太"的商品列表
SELECT '=== fin_product 中含「太」的商品 ===' AS info;
SELECT
    product_id,
    product_code,
    product_name,
    CASE
        WHEN product_name NOT LIKE '%肽%' AND product_name LIKE '%太%' THEN '疑似错别字（含太不含肽）'
        WHEN product_name LIKE '%太%' AND product_name LIKE '%肽%' THEN '含太也含肽（需人工判断）'
    END AS diagnosis
FROM fin_product
WHERE product_name LIKE '%太%'
  AND del_flag = '0'
ORDER BY product_id;

-- 1.2 统计各业务表含"太"的记录数（快照字段）
SELECT '=== 各业务表含「太」记录数统计 ===' AS info;
SELECT 'fin_product'              AS table_name, COUNT(*) AS cnt FROM fin_product              WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'fin_purchase_detail',           COUNT(*) FROM fin_purchase_detail      WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'fin_sale_record',               COUNT(*) FROM fin_sale_record          WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'fin_stock_ledger',              COUNT(*) FROM fin_stock_ledger         WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'fin_stock_snapshot',            COUNT(*) FROM fin_stock_snapshot       WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'fin_stock_init_item',           COUNT(*) FROM fin_stock_init_item      WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'finance_stocktake_item',        COUNT(*) FROM finance_stocktake_item   WHERE product_name        LIKE '%太%'
UNION ALL
SELECT 'mem_purchase_item',             COUNT(*) FROM mem_purchase_item        WHERE product_name_snapshot LIKE '%太%'
UNION ALL
SELECT 'mem_purchase_return_item',      COUNT(*) FROM mem_purchase_return_item WHERE product_name_snapshot LIKE '%太%';


-- ##########################################################
-- 第 2 步：精确更新
--
-- 【执行前必读】
--   1. 先执行第 1 步，查看 1.1 查询结果
--   2. 确认哪些 product_id 的商品名是"错把肽写成太"
--   3. 将下面的 @wrong_product_ids 替换为实际需要修正的 product_id 列表
--      （格式：逗号分隔，如 '1,2,3'）
--   4. 确认无误后取消下面 UPDATE 语句的注释执行
--
--   如果确认所有含"太"的商品都是错别字（不含正确的"太太"等），
--   可以把 @wrong_product_ids 设为 'ALL'，脚本会按 product_name LIKE '%太%' 更新。
-- ##########################################################

-- 设置需要修正的商品 ID 列表（执行前务必修改为实际值）
-- 示例：SET @wrong_product_ids = '123,456,789';
-- 如果全部含"太"的都要改：SET @wrong_product_ids = 'ALL';
SET @wrong_product_ids = 'ALL';  -- ← 执行前修改此处


-- ============================================================
-- 2.1 更新商品主表 fin_product
-- ============================================================
-- UPDATE fin_product
-- SET product_name = REPLACE(product_name, '太', '肽')
-- WHERE del_flag = '0'
--   AND product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('fin_product 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.2 更新进货明细 fin_purchase_detail
--    按商品主表修正后的 product_id 关联更新快照名
-- ============================================================
-- UPDATE fin_purchase_detail d
-- JOIN fin_product p ON d.product_id = p.product_id
-- SET d.product_name = REPLACE(d.product_name, '太', '肽')
-- WHERE d.product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(d.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('fin_purchase_detail 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.3 更新销售记录 fin_sale_record
-- ============================================================
-- UPDATE fin_sale_record r
-- SET r.product_name = REPLACE(r.product_name, '太', '肽')
-- WHERE r.product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(r.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('fin_sale_record 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.4 更新库存流水 fin_stock_ledger
-- ============================================================
-- UPDATE fin_stock_ledger l
-- SET l.product_name = REPLACE(l.product_name, '太', '肽')
-- WHERE l.product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(l.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('fin_stock_ledger 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.5 更新库存快照 fin_stock_snapshot
-- ============================================================
-- UPDATE fin_stock_snapshot s
-- SET s.product_name = REPLACE(s.product_name, '太', '肽')
-- WHERE s.product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(s.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('fin_stock_snapshot 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.6 更新库存初始化明细 fin_stock_init_item
-- ============================================================
-- UPDATE fin_stock_init_item i
-- SET i.product_name = REPLACE(i.product_name, '太', '肽')
-- WHERE i.product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(i.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('fin_stock_init_item 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.7 更新盘点明细 finance_stocktake_item
-- ============================================================
-- UPDATE finance_stocktake_item si
-- SET si.product_name = REPLACE(si.product_name, '太', '肽')
-- WHERE si.product_name LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(si.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('finance_stocktake_item 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.8 更新会员购买明细 mem_purchase_item（字段名：product_name_snapshot）
-- ============================================================
-- UPDATE mem_purchase_item mi
-- SET mi.product_name_snapshot = REPLACE(mi.product_name_snapshot, '太', '肽')
-- WHERE mi.product_name_snapshot LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(mi.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('mem_purchase_item 更新行数: ', ROW_COUNT()) AS update_result;


-- ============================================================
-- 2.9 更新会员退货明细 mem_purchase_return_item（字段名：product_name_snapshot）
-- ============================================================
-- UPDATE mem_purchase_return_item ri
-- SET ri.product_name_snapshot = REPLACE(ri.product_name_snapshot, '太', '肽')
-- WHERE ri.product_name_snapshot LIKE '%太%'
--   AND ( @wrong_product_ids = 'ALL'
--         OR FIND_IN_SET(ri.product_id, @wrong_product_ids) );
--
-- SELECT CONCAT('mem_purchase_return_item 更新行数: ', ROW_COUNT()) AS update_result;


-- ##########################################################
-- 第 3 步：验证更新结果
-- ##########################################################

-- 3.1 验证：各表不再包含"太"（已全部改为"肽"）
-- SELECT '=== 验证：更新后各表含「太」记录数（应为0或仅剩正确含太的） ===' AS info;
-- SELECT 'fin_product'              AS table_name, COUNT(*) AS remaining FROM fin_product              WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'fin_purchase_detail',           COUNT(*) FROM fin_purchase_detail      WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'fin_sale_record',               COUNT(*) FROM fin_sale_record          WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'fin_stock_ledger',              COUNT(*) FROM fin_stock_ledger         WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'fin_stock_snapshot',            COUNT(*) FROM fin_stock_snapshot       WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'fin_stock_init_item',           COUNT(*) FROM fin_stock_init_item      WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'finance_stocktake_item',        COUNT(*) FROM finance_stocktake_item   WHERE product_name        LIKE '%太%'
-- UNION ALL
-- SELECT 'mem_purchase_item',             COUNT(*) FROM mem_purchase_item        WHERE product_name_snapshot LIKE '%太%'
-- UNION ALL
-- SELECT 'mem_purchase_return_item',      COUNT(*) FROM mem_purchase_return_item WHERE product_name_snapshot LIKE '%太%';

-- 3.2 验证：更新后含"肽"的商品列表
-- SELECT '=== 验证：更新后含「肽」的商品 ===' AS info;
-- SELECT product_id, product_code, product_name FROM fin_product WHERE product_name LIKE '%肽%' AND del_flag = '0' ORDER BY product_id;
