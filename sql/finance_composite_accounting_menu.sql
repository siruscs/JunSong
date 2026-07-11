SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =============================================================
-- 复合核算菜单与权限点
-- 更新日期: 2026-07-07
-- =============================================================

-- 财务管理根菜单
SET @financeRootId = (SELECT menu_id FROM sys_menu WHERE menu_name = '财务管理' AND parent_id = 0 LIMIT 1);

-- 清理旧菜单(含子菜单和角色关联)
SET @oldMenuId = (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'compositeAccounting' LIMIT 1);
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE menu_id = @oldMenuId OR parent_id = @oldMenuId
);
DELETE FROM sys_menu WHERE menu_id = @oldMenuId OR parent_id = @oldMenuId;

-- 取当前最大 menu_id 作为起点
SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

-- 主菜单(C) + 8 个权限点(F)
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '复合核算', @financeRootId, 20, 'compositeAccounting', 'finance/compositeAccounting/index', 1, 0, 'C', '0', '0', 'finance:compositeAccounting:list', 'build', 'admin', NOW(), '', NULL, '复合核算/多店统一核算菜单'),
(@maxMenuId + 2, '复合池查询', @maxMenuId + 1, 1, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 3, '复合池新增', @maxMenuId + 1, 2, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 4, '复合池修改', @maxMenuId + 1, 3, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 5, '复合池删除', @maxMenuId + 1, 4, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 6, '周期纳入', @maxMenuId + 1, 5, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:include', '#', 'admin', NOW(), '', NULL, '回本后手动纳入周期'),
(@maxMenuId + 7, '确认回本', @maxMenuId + 1, 6, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:confirm', '#', 'admin', NOW(), '', NULL, '财务确认整体回本'),
(@maxMenuId + 8, '关闭复合池', @maxMenuId + 1, 7, '', '', 1, 0, 'F', '0', '0', 'finance:compositeAccounting:close', '#', 'admin', NOW(), '', NULL, '关闭复合核算池');

-- 授权给超管角色(role_id=1)
INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN @maxMenuId + 1 AND @maxMenuId + 8;
