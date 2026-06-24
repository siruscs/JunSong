SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 财务管理 - 核算周期/回本状态菜单
SET @financeRootId = (SELECT menu_id FROM sys_menu WHERE menu_name = '财务管理' AND parent_id = 0 LIMIT 1);
SET @oldMenuId = (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'accountingPeriod' LIMIT 1);

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE menu_id = @oldMenuId OR parent_id = @oldMenuId
);
DELETE FROM sys_menu WHERE menu_id = @oldMenuId OR parent_id = @oldMenuId;

SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '核算周期', @financeRootId, 9, 'accountingPeriod', 'finance/accountingPeriod/index', 1, 0, 'C', '0', '0', 'finance:accountingPeriod:list', 'chart', 'admin', NOW(), '', NULL, '核算周期与回本状态菜单'),
(@maxMenuId + 2, '周期查询', @maxMenuId + 1, 1, '', '', 1, 0, 'F', '0', '0', 'finance:accountingPeriod:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 3, '周期新增', @maxMenuId + 1, 2, '', '', 1, 0, 'F', '0', '0', 'finance:accountingPeriod:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 4, '周期修改', @maxMenuId + 1, 3, '', '', 1, 0, 'F', '0', '0', 'finance:accountingPeriod:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 5, '周期删除', @maxMenuId + 1, 4, '', '', 1, 0, 'F', '0', '0', 'finance:accountingPeriod:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 6, '分润结转', @maxMenuId + 1, 5, '', '', 1, 0, 'F', '0', '0', 'finance:profitShare:add', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN @maxMenuId + 1 AND @maxMenuId + 6;
