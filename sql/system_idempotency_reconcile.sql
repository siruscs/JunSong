SET NAMES utf8mb4;

-- ============================================================
-- 幂等记录对账与归档脚本（可重复执行、非破坏）
-- 用途：
--   1. 扫描重复幂等键（理论上唯一索引保证不会出现，但需对账）
--   2. 扫描过期未清理的记录
--   3. 提供归档建议（不直接删除，由归档任务执行）
-- ============================================================

-- ---------- 1. 重复幂等键扫描 ----------
-- 唯一索引 uk_idempotency_tenant_scene_key 保证不会出现重复，此处为对账
SELECT '重复幂等键扫描' AS reconciliation_type,
       tenant_id, scene, idempotency_key, COUNT(*) AS cnt
    FROM sys_idempotency_record
    GROUP BY tenant_id, scene, idempotency_key
    HAVING COUNT(*) > 1;

-- 预期：0 行（唯一索引保证）

-- ---------- 2. 过期记录扫描 ----------
-- expire_time 已过期但状态仍为 PROCESSING 的记录（可能死锁）
SELECT '过期 PROCESSING 记录扫描' AS reconciliation_type,
       COUNT(*) AS expired_processing_count
    FROM sys_idempotency_record
    WHERE status = 'PROCESSING'
      AND expire_time IS NOT NULL
      AND expire_time < NOW();

-- ---------- 3. 已完成记录统计 ----------
SELECT '已完成记录统计' AS reconciliation_type,
       tenant_id, scene, COUNT(*) AS succeeded_count
    FROM sys_idempotency_record
    WHERE status = 'SUCCEEDED'
    GROUP BY tenant_id, scene
    ORDER BY succeeded_count DESC
    LIMIT 20;

-- ---------- 4. 失败记录统计 ----------
SELECT '失败记录统计' AS reconciliation_type,
       tenant_id, scene, COUNT(*) AS failed_count
    FROM sys_idempotency_record
    WHERE status = 'FAILED'
    GROUP BY tenant_id, scene
    ORDER BY failed_count DESC
    LIMIT 20;

-- ---------- 5. 归档建议（不执行删除，仅输出建议） ----------
-- 归档策略：
--   - SUCCEEDED 且 expire_time < NOW() - INTERVAL 1 DAY → 可归档
--   - FAILED 且 expire_time < NOW() - INTERVAL 1 DAY → 可归档
--   - PROCESSING 且 expire_time < NOW() - INTERVAL 1 HOUR → 可能死锁，需人工核查
-- 注意：财务高风险接口保留期 ≥ 30 天，由配置决定，不能直接删除审计证据

SELECT '可归档记录建议' AS reconciliation_type,
       status, COUNT(*) AS archivable_count
    FROM sys_idempotency_record
    WHERE expire_time IS NOT NULL
      AND expire_time < NOW() - INTERVAL 1 DAY
      AND status IN ('SUCCEEDED', 'FAILED')
    GROUP BY status;

-- ---------- 6. 租户隔离核验 ----------
-- 每个租户的幂等记录数
SELECT '租户隔离核验' AS reconciliation_type,
       tenant_id, COUNT(*) AS record_count
    FROM sys_idempotency_record
    GROUP BY tenant_id
    ORDER BY record_count DESC
    LIMIT 20;
