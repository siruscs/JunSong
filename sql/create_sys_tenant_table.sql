CREATE TABLE IF NOT EXISTS `sys_tenant` (
  `tenant_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户编号',
  `tenant_name` VARCHAR(30) NOT NULL COMMENT '租户名称',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `domain` VARCHAR(128) DEFAULT NULL COMMENT '域名',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `account_count` INT DEFAULT 0 COMMENT '用户数量',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';