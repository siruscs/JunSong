SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 成本核算 / 回本 / 分润结转上线后只读验证脚本
-- 用途：导入 migration_20260605_accounting_period_profit.sql 及菜单 SQL 后执行，检查关键对象和权限是否齐全。

-- 1. 核心表检查
SELECT
  table_name,
  table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'fin_dept_profit_config',
    'fin_accounting_period',
    'fin_investor',
    'fin_invest_record',
    'fin_profit_share_record',
    'fin_profit_share_detail'
  )
ORDER BY table_name;

-- 2. 旧业务表周期字段检查
SELECT
  table_name,
  column_name,
  column_type,
  column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND column_name = 'period_id'
  AND table_name IN (
    'fin_expense',
    'fin_advance',
    'fin_purchase',
    'fin_sale_record',
    'fin_sale_payment',
    'fin_investor_payment'
  )
ORDER BY table_name;

-- 3. 分润自动返款字段检查
SELECT
  table_name,
  column_name,
  column_type,
  column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fin_investor_payment'
  AND column_name IN ('share_id', 'share_detail_id', 'investor_id', 'source_type', 'payment_status', 'invest_ratio')
ORDER BY ordinal_position;

-- 4. 财务菜单检查
SELECT
  menu_id,
  menu_name,
  parent_id,
  order_num,
  path,
  component,
  perms
FROM sys_menu
WHERE path IN ('accountingPeriod', 'deptProfitConfig', 'profitShare', 'investor', 'investRecord')
   OR perms IN (
    'finance:accountingPeriod:list',
    'finance:accountingPeriod:query',
    'finance:accountingPeriod:add',
    'finance:accountingPeriod:edit',
    'finance:accountingPeriod:remove',
    'finance:profitShare:list',
    'finance:profitShare:query',
    'finance:profitShare:add',
    'finance:profitShare:edit',
    'finance:profitShare:remove',
    'finance:deptProfitConfig:list',
    'finance:deptProfitConfig:query',
    'finance:deptProfitConfig:add',
    'finance:deptProfitConfig:edit',
    'finance:deptProfitConfig:remove',
    'finance:investor:list',
    'finance:investor:query',
    'finance:investor:add',
    'finance:investor:edit',
    'finance:investor:remove',
    'finance:investRecord:list',
    'finance:investRecord:query',
    'finance:investRecord:add',
    'finance:investRecord:edit',
    'finance:investRecord:remove'
   )
ORDER BY parent_id, order_num, menu_id;

-- 5. 分润配置缺失检查：有财务流水但未配置店长分润比例的机构
SELECT DISTINCT x.dept_id
FROM (
  SELECT dept_id FROM fin_expense WHERE del_flag = '0'
  UNION
  SELECT dept_id FROM fin_purchase WHERE del_flag = '0'
  UNION
  SELECT dept_id FROM fin_sale_payment WHERE del_flag = '0'
  UNION
  SELECT dept_id FROM fin_advance WHERE del_flag = '0'
) x
LEFT JOIN fin_dept_profit_config c ON c.dept_id = x.dept_id AND c.del_flag = '0' AND c.status = '0'
WHERE c.config_id IS NULL
ORDER BY x.dept_id;

-- 6. 已回本待结转但未闭合周期检查：正常结果应为空；如有数据，重新执行回本检查或结转会自动修正。
SELECT
  period_id,
  dept_id,
  period_no,
  status,
  break_even_time,
  end_time,
  net_profit
FROM fin_accounting_period
WHERE del_flag = '0'
  AND status = '1'
  AND end_time IS NULL
ORDER BY dept_id, start_time;

-- 7. 自动分润返款链路检查
SELECT
  s.share_id,
  s.share_no,
  s.period_id,
  s.dept_id,
  s.net_profit,
  COUNT(d.detail_id) AS detail_count,
  SUM(CASE WHEN d.receiver_type = '2' THEN 1 ELSE 0 END) AS investor_detail_count,
  SUM(CASE WHEN p.payment_id IS NOT NULL THEN 1 ELSE 0 END) AS auto_payment_count
FROM fin_profit_share_record s
LEFT JOIN fin_profit_share_detail d ON d.share_id = s.share_id
LEFT JOIN fin_investor_payment p ON p.payment_id = d.payment_id AND p.source_type = '1' AND p.del_flag = '0'
WHERE s.del_flag = '0'
GROUP BY s.share_id, s.share_no, s.period_id, s.dept_id, s.net_profit
ORDER BY s.share_time DESC;
