SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =============================================================
-- 复合核算 PROD 增量迁移脚本
-- 日期: 2026-07-07
-- 用途: 修复已部署到 PROD 的 4 张表(P0/P1 审查问题)
--   1. [P1#6] fin_composite_period_item 增加全局唯一约束(有效记录维度)
--   2. [P2#7] fin_composite_accounting_pool.status 注释补充草稿状态(4)
-- 注意: 此脚本幂等,可重复执行
-- =============================================================

-- -------------------------------------------------------------
-- 1. fin_composite_period_item 增加生成列 + 全局唯一约束
--    业务规则:同一周期在有效状态(status='0' and del_flag='0')下
--    只能被一个复合池纳入。撤销/删除后 active_period_key 为 NULL,允许重新纳入。
-- -------------------------------------------------------------
-- 1.1 添加生成列(若不存在)
SET @colExists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fin_composite_period_item' AND column_name = 'active_period_key');
SET @sql = IF(@colExists = 0,
  'ALTER TABLE `fin_composite_period_item` ADD COLUMN `active_period_key` bigint GENERATED ALWAYS AS (CASE WHEN `status` = ''0'' AND `del_flag` = ''0'' THEN `period_id` ELSE NULL END) VIRTUAL',
  'SELECT ''active_period_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 添加全局唯一索引(若不存在)
SET @idxExists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'fin_composite_period_item' AND index_name = 'uk_active_period');
SET @sql = IF(@idxExists = 0,
  'ALTER TABLE `fin_composite_period_item` ADD UNIQUE KEY `uk_active_period` (`active_period_key`)',
  'SELECT ''uk_active_period already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------
-- 2. fin_composite_accounting_pool.status 注释补充草稿状态(4)
--    仅修改注释,不影响数据
-- -------------------------------------------------------------
ALTER TABLE `fin_composite_accounting_pool` MODIFY COLUMN `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0进行中 1已达回本 2已确认回本 3已关闭 4草稿)';
