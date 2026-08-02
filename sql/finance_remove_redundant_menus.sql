SET NAMES utf8mb4;

-- 清理财务管理下明确重复、废弃和无页面菜单。
-- 保留业务菜单数据之外的历史业务数据；如需恢复，可依据本次执行前的 SELECT 结果回滚。
START TRANSACTION;

SELECT menu_id, parent_id, menu_name, path, component, visible, perms
  FROM sys_menu
 WHERE menu_id IN (
   2151,       -- 废弃成本分析报表
   2392,       -- 无页面财务概览统计
   2400,       -- 与财务概览重复的经营总览
   2440,       -- 与财务概览重复的现金流看板
   2438,       -- 空的库存健康检查
   500,        -- 财务目录下重复的操作日志
   3247        -- 重复库存调整菜单
 )
 OR parent_id IN (2400, 500, 3247);

DELETE FROM sys_role_menu
 WHERE menu_id IN (
   SELECT menu_id FROM sys_menu
    WHERE menu_id IN (2151, 2392, 2400, 2440, 2438, 500, 3247)
       OR parent_id IN (2400, 500, 3247)
 );

DELETE FROM sys_menu
 WHERE menu_id IN (2151, 2392, 2400, 2440, 2438, 500, 3247)
    OR parent_id IN (2400, 500, 3247);

COMMIT;

SELECT menu_id, parent_id, menu_name, path, component, visible, perms
  FROM sys_menu
 WHERE parent_id = (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1)
 ORDER BY order_num, menu_id;
