SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 财务管理 - 投资人 / 投资来源记录菜单
SET @financeRootId = (SELECT menu_id FROM sys_menu WHERE menu_name = '财务管理' AND parent_id = 0 LIMIT 1);
SET @oldInvestorMenuId = (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'investor' LIMIT 1);
SET @oldInvestRecordMenuId = (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'investRecord' LIMIT 1);

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE menu_id IN (@oldInvestorMenuId, @oldInvestRecordMenuId)
       OR parent_id IN (@oldInvestorMenuId, @oldInvestRecordMenuId)
);
DELETE FROM sys_menu WHERE menu_id IN (@oldInvestorMenuId, @oldInvestRecordMenuId) OR parent_id IN (@oldInvestorMenuId, @oldInvestRecordMenuId);

SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '投资人', @financeRootId, 12, 'investor', 'finance/investor/index', 1, 0, 'C', '0', '0', 'finance:investor:list', 'peoples', 'admin', NOW(), '', NULL, '投资人菜单'),
(@maxMenuId + 2, '投资人查询', @maxMenuId + 1, 1, '', '', 1, 0, 'F', '0', '0', 'finance:investor:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 3, '投资人新增', @maxMenuId + 1, 2, '', '', 1, 0, 'F', '0', '0', 'finance:investor:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 4, '投资人修改', @maxMenuId + 1, 3, '', '', 1, 0, 'F', '0', '0', 'finance:investor:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 5, '投资人删除', @maxMenuId + 1, 4, '', '', 1, 0, 'F', '0', '0', 'finance:investor:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 6, '投资来源记录', @financeRootId, 13, 'investRecord', 'finance/investRecord/index', 1, 0, 'C', '0', '0', 'finance:investRecord:list', 'money', 'admin', NOW(), '', NULL, '投资来源记录菜单'),
(@maxMenuId + 7, '投资来源查询', @maxMenuId + 6, 1, '', '', 1, 0, 'F', '0', '0', 'finance:investRecord:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 8, '投资来源新增', @maxMenuId + 6, 2, '', '', 1, 0, 'F', '0', '0', 'finance:investRecord:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 9, '投资来源修改', @maxMenuId + 6, 3, '', '', 1, 0, 'F', '0', '0', 'finance:investRecord:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 10, '投资来源删除', @maxMenuId + 6, 4, '', '', 1, 0, 'F', '0', '0', 'finance:investRecord:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN @maxMenuId + 1 AND @maxMenuId + 10;
