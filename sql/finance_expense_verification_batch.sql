-- 费用核销批次、明细及独立权限（可重复执行）

CREATE TABLE IF NOT EXISTS `fin_expense_verify_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL,
  `request_id` varchar(64) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `dept_id` bigint NOT NULL,
  `total_expense_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `total_advance_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `difference_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `status` varchar(16) NOT NULL DEFAULT 'VERIFIED',
  `source_type` varchar(16) NOT NULL DEFAULT 'NORMAL',
  `verify_by` varchar(64) NOT NULL,
  `verify_time` datetime NOT NULL,
  `reverse_by` varchar(64) DEFAULT NULL,
  `reverse_time` datetime DEFAULT NULL,
  `reverse_reason` varchar(500) DEFAULT NULL,
  `reverse_request_id` varchar(64) DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_verify_batch_no` (`tenant_id`, `batch_no`),
  UNIQUE KEY `uk_verify_batch_request` (`tenant_id`, `request_id`),
  UNIQUE KEY `uk_verify_reverse_request` (`tenant_id`, `reverse_request_id`),
  KEY `idx_verify_batch_dept_status` (`tenant_id`, `dept_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用核销批次';

CREATE TABLE IF NOT EXISTS `fin_expense_verify_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `expense_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `dept_id` bigint NOT NULL,
  `expense_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `original_status` varchar(16) DEFAULT NULL,
  `original_advance_id` bigint DEFAULT NULL,
  `period_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `uk_verify_expense` (`batch_id`, `expense_id`),
  KEY `idx_verify_expense_id` (`tenant_id`, `expense_id`),
  KEY `idx_verify_expense_dept` (`tenant_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用核销费用明细';

CREATE TABLE IF NOT EXISTS `fin_advance_verify_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `advance_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `dept_id` bigint NOT NULL,
  `advance_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `original_status` varchar(16) DEFAULT NULL,
  `period_id` bigint DEFAULT NULL,
  `relation_type` varchar(16) NOT NULL DEFAULT 'SOURCE',
  `generated_flag` char(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `uk_verify_advance` (`batch_id`, `advance_id`, `relation_type`),
  KEY `idx_verify_advance_id` (`tenant_id`, `advance_id`),
  KEY `idx_verify_advance_dept` (`tenant_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用核销借支明细';

-- 复用现有“费用记录”菜单作为按钮父节点；找不到父菜单时不写入。
SET @expense_menu_id := (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `perms` = 'finance:expense:list'
  ORDER BY `menu_id` LIMIT 1
);

-- 优先原位升级旧“费用核销”按钮，保留 menu_id 及已有 sys_role_menu 授权关系。
UPDATE `sys_menu`
SET `perms` = 'finance:expense:verify',
    `update_by` = 'system',
    `update_time` = NOW(),
    `remark` = '费用单笔及批量核销权限'
WHERE `menu_name` = '费用核销'
  AND `perms` = 'finance:expense:edit'
  AND `parent_id` = @expense_menu_id
  AND NOT EXISTS (
    SELECT 1 FROM (
      SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'finance:expense:verify'
    ) current_verify_menu
  );

INSERT INTO `sys_menu`
  (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`,
   `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
   `create_by`, `create_time`, `remark`)
SELECT '费用核销', @expense_menu_id, 10, '', '', '', '', 1, 0, 'F', '0', '0',
       'finance:expense:verify', '#', 'system', NOW(), '费用单笔及批量核销权限'
WHERE @expense_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @expense_menu_id
      AND `menu_name` = '费用核销'
      AND `perms` IN ('finance:expense:edit', 'finance:expense:verify')
  );

INSERT INTO `sys_menu`
  (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`,
   `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
   `create_by`, `create_time`, `remark`)
SELECT '费用反核销', @expense_menu_id, 11, '', '', '', '', 1, 0, 'F', '0', '0',
       'finance:expense:unverify', '#', 'system', NOW(), '费用整批反核销权限'
WHERE @expense_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'finance:expense:unverify');

-- 仅按明确角色标识授权，避免依赖环境中的固定角色 ID。
-- 原位升级会保留旧角色关联；先精确清理该核销按钮上的非财务角色授权。
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE m.`perms` = 'finance:expense:verify'
  AND m.`parent_id` = @expense_menu_id
  AND r.`role_key` NOT IN ('finance', 'finance_staff', 'finance_manager');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_menu` m ON m.`perms` = 'finance:expense:verify'
WHERE r.`role_key` IN ('finance', 'finance_staff', 'finance_manager');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_menu` m ON m.`perms` = 'finance:expense:unverify'
WHERE r.`role_key` IN ('finance_manager');
