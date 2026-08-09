-- =====================================================================
-- Phase 2: 小程序模块分组排序哨兵行（@GROUP@前缀）
-- 执行对象：PROD `junsong-cloud` 库
-- 幂等：INSERT ... WHERE NOT EXISTS (按 uk_module_key 去重保证安全重复执行)
-- =====================================================================

SET NAMES utf8mb4;

-- 在同一张 sys_mp_module_sort 表里，用 module_key 前缀 @GROUP@ 作为分组排序的哨兵行。
-- 分组顺序（默认）：会员服务(10) → 会员运营(20) → 财务管理(30) → 系统管理(40) → 移动办公(50)

INSERT INTO `sys_mp_module_sort` (`module_key`, `group_name`, `sort_order`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT * FROM (
  SELECT '@GROUP@会员服务' AS module_key, NULL AS group_name, 10 AS sort_order, '分组哨兵：控制「会员服务」整体显示顺序' AS remark, 'system' AS create_by, NOW() AS create_time, 'system' AS update_by, NOW() AS update_time
  UNION ALL SELECT '@GROUP@会员运营', NULL, 20, '分组哨兵：控制「会员运营」整体显示顺序', 'system', NOW(), 'system', NOW()
  UNION ALL SELECT '@GROUP@财务管理', NULL, 30, '分组哨兵：控制「财务管理」整体显示顺序', 'system', NOW(), 'system', NOW()
  UNION ALL SELECT '@GROUP@系统管理', NULL, 40, '分组哨兵：控制「系统管理」整体显示顺序', 'system', NOW(), 'system', NOW()
  UNION ALL SELECT '@GROUP@移动办公', NULL, 50, '分组哨兵：控制「移动办公」整体显示顺序', 'system', NOW(), 'system', NOW()
) AS init_vals
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_mp_module_sort` s WHERE s.module_key = init_vals.module_key
);

-- 验证：模块行 vs 分组哨兵行 数量汇总
SELECT
  CASE WHEN module_key LIKE '@GROUP@%' THEN 'group_sentinel' ELSE 'module' END AS row_type,
  COUNT(*) AS cnt,
  MIN(sort_order) AS min_sort,
  MAX(sort_order) AS max_sort
FROM `sys_mp_module_sort`
GROUP BY row_type;
