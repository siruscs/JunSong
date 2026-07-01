-- =====================================================================
-- TRAE-R3-05: 预警通知去重数据库兜底
--
-- 目标：在 (user_id, type, biz_id) 上建立唯一键，防止并发场景下
--       "先查再发" 产生重复通知。
--
-- 执行顺序：
--   1. 将 biz_id 为空字符串的记录改为 NULL（使唯一键不影响无 bizId 的系统通知）
--   2. 修改列默认值为 NULL
--   3. 清理重复数据（每组 user_id+type+biz_id 仅保留 id 最小的一条）
--   4. 添加唯一键 uk_notification_user_type_biz
--
-- 注意：此脚本需在生产执行前由 DBA 复核。
-- =====================================================================

-- 1. 将 biz_id 为空字符串的记录改为 NULL（NULL 不受唯一键约束）
UPDATE sys_notification SET biz_id = NULL WHERE biz_id = '';

-- 2. 修改列默认值为 NULL，避免后续插入空字符串
ALTER TABLE sys_notification MODIFY COLUMN biz_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务ID（如用户ID）';

-- 3. 清理重复数据：同一 user_id+type+biz_id 保留 id 最小的一条
DELETE n1 FROM sys_notification n1
INNER JOIN (
    SELECT user_id, type, biz_id, MIN(id) AS min_id
    FROM sys_notification
    WHERE biz_id IS NOT NULL
    GROUP BY user_id, type, biz_id
    HAVING COUNT(*) > 1
) dup ON n1.user_id = dup.user_id
        AND n1.type = dup.type
        AND n1.biz_id = dup.biz_id
        AND n1.id > dup.min_id;

-- 4. 添加唯一键
ALTER TABLE sys_notification
    ADD UNIQUE KEY uk_notification_user_type_biz (user_id, type, biz_id);
