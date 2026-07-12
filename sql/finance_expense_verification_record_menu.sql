SET NAMES utf8mb4;
-- 核销记录页面菜单及权限（可重复执行）

-- 复用现有财务目录菜单作为父节点
SET @finance_menu_id := (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `menu_id` = 2000
  ORDER BY `menu_id` LIMIT 1
);

-- 新增"核销记录"菜单（类型C，页面组件）
INSERT INTO `sys_menu`
  (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`,
   `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
   `create_by`, `create_time`, `remark`)
SELECT '核销记录', @finance_menu_id, 45, 'verificationRecord', 'finance/verificationRecord/index', '',
        '', 1, 0, 'C', '0', '0',
        'finance:expense:verificationRecord:list', 'documentation',
        'system', NOW(), '费用核销批次审计记录'
WHERE @finance_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `perms` = 'finance:expense:verificationRecord:list'
  );

-- 授予财务相关角色查看核销记录的权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_menu` m ON m.`perms` = 'finance:expense:verificationRecord:list'
WHERE r.`role_key` IN ('finance', 'finance_staff', 'finance_manager');
