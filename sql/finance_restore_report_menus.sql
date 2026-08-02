SET NAMES utf8mb4;

-- 恢复已存在但被隐藏的财务报表菜单；不创建重复菜单，不触碰废弃成本核算菜单。
START TRANSACTION;

UPDATE sys_menu
   SET visible = '0', status = '0'
 WHERE perms IN (
   'finance:report:expense',
   'finance:report:profitShare',
   'finance:report:sale',
   'finance:report:profit',
   'finance:report:stock'
 );

-- 门店经营分析路由已存在时同步恢复菜单；不存在时按当前财务目录补建。
SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @storeReportId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:report:store' LIMIT 1);
SET @newMenuId := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);

UPDATE sys_menu
   SET visible = '0', status = '0'
 WHERE perms = 'finance:report:store';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @newMenuId, '门店经营分析', @financeRootId, 16, 'storeReport', 'finance/report/store', '', '0', '0', 'C', '0', '0', 'finance:report:store', 'shop', 'admin', NOW(), '', NULL, '门店经营分析菜单'
 WHERE @financeRootId IS NOT NULL AND @storeReportId IS NULL;

COMMIT;

SELECT menu_name, path, visible, status, perms
  FROM sys_menu
 WHERE perms LIKE 'finance:report:%'
 ORDER BY parent_id, order_num;
