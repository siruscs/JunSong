SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 工作流中心菜单初始化脚本
-- 说明：
-- 1. 仅创建菜单与按钮权限，不执行任何 Nacos 变更
-- 2. 默认授予 admin(角色ID=1)

SET @oldWorkflowRootId = (
    SELECT menu_id FROM sys_menu WHERE menu_name = '工作流中心' AND parent_id = 0 LIMIT 1
);

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE menu_id = @oldWorkflowRootId OR parent_id = @oldWorkflowRootId
);
DELETE FROM sys_menu WHERE menu_id = @oldWorkflowRootId OR parent_id = @oldWorkflowRootId;

SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '工作流中心', 0, 7, 'workflow', NULL, 1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '', NULL, '工作流中心顶级目录'),
(@maxMenuId + 2, '流程定义', @maxMenuId + 1, 1, 'definition', 'workflow/definition/index', 1, 0, 'C', '0', '0', 'workflow:definition:list', 'tree', 'admin', NOW(), '', NULL, '流程定义中心');

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 3, '流程定义查询', @maxMenuId + 2, 1, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 4, '流程定义新增', @maxMenuId + 2, 2, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 5, '流程定义修改', @maxMenuId + 2, 3, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 6, '流程定义发布', @maxMenuId + 2, 4, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:deploy', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 7, '流程定义删除', @maxMenuId + 2, 5, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN @maxMenuId + 1 AND @maxMenuId + 7;
