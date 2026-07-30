SET NAMES utf8mb4;

-- ============================================================
-- 全系统幂等记录表（可重复执行、租户隔离、非破坏）
-- 作用域：tenant_id + scene + idempotency_key 唯一
-- 状态机：PROCESSING → SUCCEEDED / FAILED
-- 性能：MySQL 唯一索引兜底，Redis 仅作可选加速层
-- ============================================================

-- ---------- 1. 幂等记录主表 ----------
CREATE TABLE IF NOT EXISTS sys_idempotency_record (
    record_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    tenant_id       BIGINT       NOT NULL                COMMENT '租户ID',
    scene           VARCHAR(64)  NOT NULL                COMMENT '幂等场景标识（如 sale:create）',
    idempotency_key VARCHAR(128) NOT NULL                COMMENT '幂等键（来自 X-Idempotency-Key 请求头）',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PROCESSING' COMMENT '状态 PROCESSING/SUCCEEDED/FAILED',
    fingerprint     VARCHAR(128)          DEFAULT NULL   COMMENT '请求体稳定指纹（字段排序+规范化后 SHA-256）',
    resource_type   VARCHAR(64)           DEFAULT NULL   COMMENT '业务资源类型（如 fin_sale_record）',
    resource_id     VARCHAR(64)           DEFAULT NULL   COMMENT '业务资源ID（如 saleId）',
    result_summary  VARCHAR(500)          DEFAULT NULL   COMMENT '结果摘要（小型响应才存，大响应只存引用）',
    error_summary   VARCHAR(500)          DEFAULT NULL   COMMENT '失败摘要（FAILED 状态时填充）',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    expire_time     DATETIME              DEFAULT NULL   COMMENT '过期时间（用于归档清理）',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_idempotency_tenant_scene_key (tenant_id, scene, idempotency_key),
    KEY idx_idempotency_status (status),
    KEY idx_idempotency_expire (expire_time),
    KEY idx_idempotency_tenant_scene (tenant_id, scene, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全系统幂等记录表';

-- ---------- 2. 对账输出 ----------
SELECT 'sys_idempotency_record 表创建' AS reconciliation_type,
       (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_idempotency_record') AS expected_1;

SELECT 'sys_idempotency_record 唯一索引核验' AS reconciliation_type,
       COUNT(*) AS unique_index_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_idempotency_record'
    AND NON_UNIQUE = 0;

SELECT 'sys_idempotency_record 索引核验' AS reconciliation_type,
       COUNT(*) AS index_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_idempotency_record';

SELECT 'sys_idempotency_record 状态分布' AS reconciliation_type,
       IFNULL(status, 'NULL') AS status, COUNT(*) AS cnt
    FROM sys_idempotency_record GROUP BY status;

-- ---------- 3. HEX 编码验证 ----------
SELECT 'sys_idempotency_record 表名 HEX 验证' AS reconciliation_type,
       HEX('sys_idempotency_record') AS table_name_hex;

-- 回滚说明（不执行）：
-- DROP TABLE IF EXISTS sys_idempotency_record;
