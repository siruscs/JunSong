-- =====================================================================
-- R7-C Task 5.1: 通知去重键 dedup_key
--
-- 目标：为 sys_notification 增加通用去重键 dedup_key，并在 (user_id, dedup_key)
--       上建立唯一索引，支撑工作台高优先级告警等场景的幂等发送。
--
-- 幂等说明：
--   使用 information_schema 检查列/索引是否存在，避免 ADD COLUMN IF NOT EXISTS
--   语法兼容问题（MySQL 8.0.14- / MariaDB 不支持）。
--
-- 与既有 uk_notification_user_type_biz (user_id, type, biz_id) 互补：
--   dedup_key 用于跨 type 的稳定业务去重键（如 workbench 联动场景）。
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 检查并增加 dedup_key 列（位于 link_url 之后）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_notification'
      AND COLUMN_NAME = 'dedup_key'
);

SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE sys_notification ADD COLUMN dedup_key varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''通知去重键（同 user_id + dedup_key 唯一）'' AFTER link_url',
    'DO 0'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 检查并建立唯一索引（user_id + dedup_key）
--    dedup_key 为 NULL 时不参与唯一约束，不影响无去重键的系统通知。
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_notification'
      AND INDEX_NAME = 'uk_sys_notification_dedup_user'
);

SET @ddl2 = IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_sys_notification_dedup_user ON sys_notification (user_id, dedup_key)',
    'DO 0'
);

PREPARE stmt FROM @ddl2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
