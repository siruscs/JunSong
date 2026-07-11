SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 财务管理 - 核算周期运维调整起始时间权限
-- 新增按钮权限 finance:accountingPeriod:opsAdjustStartTime，仅授权给管理员角色

-- 1. 找到核算周期菜单
SET @accountingPeriodMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'finance:accountingPeriod:list' AND menu_type = 'C' LIMIT 1);

-- 2. 清理旧的运维调整按钮（如有）
SET @oldBtnId = (SELECT menu_id FROM sys_menu WHERE parent_id = @accountingPeriodMenuId AND perms = 'finance:accountingPeriod:opsAdjustStartTime' LIMIT 1);
DELETE FROM sys_role_menu WHERE menu_id = @oldBtnId;
DELETE FROM sys_menu WHERE menu_id = @oldBtnId;

-- 3. 取当前最大 menu_id
SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

-- 4. 插入运维调整起始时间按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '运维调整起始时间', @accountingPeriodMenuId, 10, '', '', 1, 0, 'F', '0', '0', 'finance:accountingPeriod:opsAdjustStartTime', '#', 'admin', NOW(), '', NULL, '运维调整历史核算周期起始时间，不重新核算金额');

-- 5. 仅授权给管理员角色（role_id=1），不默认授权普通店长或普通财务角色
INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id = @maxMenuId + 1;
