-- =============================================
-- sys_dept 门店坐标化升级（门店开业流程空间查重前置依赖）
-- 日期：2026-06-23
-- 说明：为部门表新增经纬度与门店类型标识，支持周边门店空间查重
-- =============================================

ALTER TABLE `sys_dept`
  ADD COLUMN `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度' AFTER `detail_address`,
  ADD COLUMN `latitude`  DECIMAL(10,7) DEFAULT NULL COMMENT '纬度' AFTER `longitude`,
  ADD COLUMN `dept_type` CHAR(1) DEFAULT '0' COMMENT '部门类型(0普通部门 1门店)' AFTER `latitude`;

-- 空间查询优化索引：门店类型 + 经纬度（包围盒预筛用）
ALTER TABLE `sys_dept` ADD INDEX `idx_dept_geo` (`dept_type`, `longitude`, `latitude`);
