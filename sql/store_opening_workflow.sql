SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_store_opening` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '申请单号',
  `store_name` VARCHAR(128) NOT NULL COMMENT '门店名称',
  `store_short_name` VARCHAR(64) DEFAULT NULL COMMENT '门店简称',
  `store_type` VARCHAR(32) DEFAULT NULL COMMENT '门店类型',
  `expected_opening_date` DATE DEFAULT NULL COMMENT '预计开业日期',
  `region_name` VARCHAR(64) DEFAULT NULL COMMENT '所属区域',
  `region_leader` VARCHAR(64) DEFAULT NULL COMMENT '区域负责人',
  `general_manager` VARCHAR(64) DEFAULT NULL COMMENT '总经理账号',
  `store_manager_name` VARCHAR(64) DEFAULT NULL COMMENT '门店负责人',
  `province` VARCHAR(64) DEFAULT NULL COMMENT '省',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '市',
  `district` VARCHAR(64) DEFAULT NULL COMMENT '区',
  `address_detail` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `site_area` DECIMAL(10,2) DEFAULT NULL COMMENT '场地面积',
  `site_mode` VARCHAR(16) DEFAULT NULL COMMENT '场地类型',
  `planned_staff_count` INT DEFAULT NULL COMMENT '预计员工人数',
  `initial_investment_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '预计首期投入',
  `estimated_monthly_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '预计月营收',
  `opening_reason` VARCHAR(1000) DEFAULT NULL COMMENT '开业说明',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_definition_key` VARCHAR(64) DEFAULT NULL COMMENT '流程定义Key',
  `workflow_status` VARCHAR(64) NOT NULL DEFAULT 'DRAFT' COMMENT '流程状态',
  `current_task_name` VARCHAR(128) DEFAULT NULL COMMENT '当前节点名称',
  `submitter` VARCHAR(64) DEFAULT NULL COMMENT '提交人',
  `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_opening_order_no` (`order_no`),
  KEY `idx_store_opening_status` (`workflow_status`),
  KEY `idx_store_opening_process_instance` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店开业申请';

SET @systemRootId = (SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1);
SET @storeOpeningMenuId = (
    SELECT menu_id FROM sys_menu WHERE parent_id = @systemRootId AND perms = 'system:storeOpening:list' LIMIT 1
);

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业申请', @systemRootId, 11, 'storeOpening', 'system/storeOpening/index', 1, 0, 'C', '0', '0', 'system:storeOpening:list', 'tree', 'admin', NOW(), '', NULL, '门店开业申请菜单'
WHERE @systemRootId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @systemRootId AND perms = 'system:storeOpening:list'
  );

SET @storeOpeningMenuId = (
    SELECT menu_id FROM sys_menu WHERE parent_id = @systemRootId AND perms = 'system:storeOpening:list' LIMIT 1
);

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业查询', @storeOpeningMenuId, 1, '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:query', '#', 'admin', NOW(), '', NULL, ''
WHERE @storeOpeningMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @storeOpeningMenuId AND perms = 'system:storeOpening:query'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业新增', @storeOpeningMenuId, 2, '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:add', '#', 'admin', NOW(), '', NULL, ''
WHERE @storeOpeningMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @storeOpeningMenuId AND perms = 'system:storeOpening:add'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业修改', @storeOpeningMenuId, 3, '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE @storeOpeningMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @storeOpeningMenuId AND perms = 'system:storeOpening:edit'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业删除', @storeOpeningMenuId, 4, '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE @storeOpeningMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @storeOpeningMenuId AND perms = 'system:storeOpening:remove'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业提交审批', @storeOpeningMenuId, 5, '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:submit', '#', 'admin', NOW(), '', NULL, ''
WHERE @storeOpeningMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @storeOpeningMenuId AND perms = 'system:storeOpening:submit'
  );

INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门店开业撤回', @storeOpeningMenuId, 6, '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:withdraw', '#', 'admin', NOW(), '', NULL, ''
WHERE @storeOpeningMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @storeOpeningMenuId AND perms = 'system:storeOpening:withdraw'
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN (
    'system:storeOpening:list',
    'system:storeOpening:query',
    'system:storeOpening:add',
    'system:storeOpening:edit',
    'system:storeOpening:remove',
    'system:storeOpening:submit',
    'system:storeOpening:withdraw'
)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
