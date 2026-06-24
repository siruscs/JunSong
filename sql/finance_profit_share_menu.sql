SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 财务管理 - 店面分润配置 / 分润记录菜单
SET @financeRootId = (SELECT menu_id FROM sys_menu WHERE menu_name = '财务管理' AND parent_id = 0 LIMIT 1);
SET @oldConfigMenuId = (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'deptProfitConfig' LIMIT 1);
SET @oldShareMenuId = (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'profitShare' LIMIT 1);

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE menu_id IN (@oldConfigMenuId, @oldShareMenuId)
       OR parent_id IN (@oldConfigMenuId, @oldShareMenuId)
);
DELETE FROM sys_menu WHERE menu_id IN (@oldConfigMenuId, @oldShareMenuId) OR parent_id IN (@oldConfigMenuId, @oldShareMenuId);

SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '分润配置', @financeRootId, 10, 'deptProfitConfig', 'finance/deptProfitConfig/index', 1, 0, 'C', '0', '0', 'finance:deptProfitConfig:list', 'edit', 'admin', NOW(), '', NULL, '店面分润配置菜单'),
(@maxMenuId + 2, '配置查询', @maxMenuId + 1, 1, '', '', 1, 0, 'F', '0', '0', 'finance:deptProfitConfig:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 3, '配置新增', @maxMenuId + 1, 2, '', '', 1, 0, 'F', '0', '0', 'finance:deptProfitConfig:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 4, '配置修改', @maxMenuId + 1, 3, '', '', 1, 0, 'F', '0', '0', 'finance:deptProfitConfig:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 5, '配置删除', @maxMenuId + 1, 4, '', '', 1, 0, 'F', '0', '0', 'finance:deptProfitConfig:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 6, '分润记录', @financeRootId, 11, 'profitShare', 'finance/profitShare/index', 1, 0, 'C', '0', '0', 'finance:profitShare:list', 'money', 'admin', NOW(), '', NULL, '分润记录菜单'),
(@maxMenuId + 7, '分润查询', @maxMenuId + 6, 1, '', '', 1, 0, 'F', '0', '0', 'finance:profitShare:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 8, '分润结转', @maxMenuId + 6, 2, '', '', 1, 0, 'F', '0', '0', 'finance:profitShare:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 9, '分润修改', @maxMenuId + 6, 3, '', '', 1, 0, 'F', '0', '0', 'finance:profitShare:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 10, '分润作废', @maxMenuId + 6, 4, '', '', 1, 0, 'F', '0', '0', 'finance:profitShare:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN @maxMenuId + 1 AND @maxMenuId + 10;
