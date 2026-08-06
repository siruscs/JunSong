-- 会员购买域发布前只读核对脚本。
-- 不修改数据、不删除数据；请在目标租户和指定核算周期下执行并留存结果。
SET NAMES utf8mb4;

SELECT 'table_presence' AS check_name,
       COUNT(*) AS created_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'mem_identity_policy', 'mem_member_status_history', 'mem_member_account',
    'mem_points_ledger', 'mem_growth_ledger', 'mem_campaign_policy',
    'mem_campaign_policy_package', 'mem_purchase_order', 'mem_purchase_item',
    'mem_purchase_delivery', 'mem_purchase_payment'
  );

SELECT 'purchase_snapshot' AS check_name,
       tenant_id,
       dept_id,
       COUNT(*) AS purchase_orders,
       COALESCE(SUM(total_amount), 0) AS order_amount,
       COALESCE(SUM(paid_amount), 0) AS paid_amount,
       COALESCE(SUM(receivable_amount), 0) AS receivable_amount
FROM mem_purchase_order
WHERE del_flag = '0'
GROUP BY tenant_id, dept_id
ORDER BY tenant_id, dept_id;

SELECT 'delivery_reconciliation' AS check_name,
       i.tenant_id,
       i.dept_id,
       COUNT(*) AS item_count,
       SUM(CASE WHEN i.delivered_quantity <> i.delivered_sale_quantity + i.delivered_gift_quantity THEN 1 ELSE 0 END) AS component_mismatch_count,
       SUM(CASE WHEN i.delivered_quantity > i.total_quantity OR i.remaining_quantity < 0 THEN 1 ELSE 0 END) AS over_delivery_count
FROM mem_purchase_item i
WHERE i.del_flag = '0'
GROUP BY i.tenant_id, i.dept_id
ORDER BY i.tenant_id, i.dept_id;

SELECT 'payment_reconciliation' AS check_name,
       o.tenant_id,
       o.dept_id,
       COUNT(*) AS order_count,
       SUM(CASE WHEN o.paid_amount + o.receivable_amount <> o.total_amount THEN 1 ELSE 0 END) AS amount_mismatch_count
FROM mem_purchase_order o
WHERE o.del_flag = '0'
GROUP BY o.tenant_id, o.dept_id
ORDER BY o.tenant_id, o.dept_id;
