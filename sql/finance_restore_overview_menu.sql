SET NAMES utf8mb4;

START TRANSACTION;

SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @overviewMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:overview:list' LIMIT 1);
SET @nextMenuId := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @nextMenuId, '财务概览', @financeRootId, 0, 'overview', 'finance/overview/index', '', '0', '0', 'C', '0', '0', 'finance:overview:list', 'chart', 'admin', NOW(), '', NULL, '核算周期经营概览'
 WHERE @financeRootId IS NOT NULL
   AND @overviewMenuId IS NULL
   AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'overview' AND parent_id = @financeRootId);

SET @overviewMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:overview:list' LIMIT 1);

UPDATE sys_menu
   SET component = 'finance/overview/index', visible = '0', status = '0'
 WHERE perms = 'finance:overview:list'
    OR (path = 'overview' AND parent_id = @financeRootId);

COMMIT;

SELECT menu_id, parent_id, menu_name, path, component, visible, status, perms
  FROM sys_menu
 WHERE perms = 'finance:overview:list';
