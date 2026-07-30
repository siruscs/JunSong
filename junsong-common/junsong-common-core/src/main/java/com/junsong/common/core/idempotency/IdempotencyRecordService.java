package com.junsong.common.core.idempotency;

/**
 * 幂等记录服务接口。
 *
 * 提供原子占位、状态流转、结果写入能力，由 AOP 切面调用。
 *
 * @author junsong
 */
public interface IdempotencyRecordService {

    /**
     * 原子占位：尝试创建 PROCESSING 记录。
     * 唯一键冲突时返回已存在记录（用于判断重复请求）。
     *
     * @param tenantId 租户ID
     * @param scene 幂等场景
     * @param idempotencyKey 幂等键
     * @param fingerprint 请求体指纹
     * @param ttlSeconds 保留期（秒）
     * @return 占位结果：success=true 表示新建成功；success=false 表示已存在，existing 为已有记录
     */
    AcquireResult acquire(Long tenantId, String scene, String idempotencyKey,
                          String fingerprint, int ttlSeconds);

    /**
     * 标记成功：PROCESSING → SUCCEEDED，写入业务结果引用。
     */
    int markSucceeded(Long recordId, String resourceType, String resourceId, String resultSummary);

    /**
     * 标记失败：PROCESSING → FAILED，写入错误摘要。
     * 失败后允许安全重试（键可被复用）。
     */
    int markFailed(Long recordId, String errorSummary);

    /**
     * 重新占位：FAILED → PROCESSING，用于同键安全重试场景。
     *
     * 仅当 retryPolicy=ALLOW_SAME_KEY 时由切面调用。
     * CAS 操作：并发安全，多线程同时重试同一 FAILED 记录时只有第一个成功。
     *
     * @param recordId 已存在的 FAILED 记录ID
     * @param newFingerprint 新请求体指纹
     * @param ttlSeconds 新的保留期（秒）
     * @return true=占位成功，可执行业务；false=状态已变化（其他线程已占位）
     */
    boolean reacquire(Long recordId, String newFingerprint, int ttlSeconds);

    /**
     * 占位结果。
     */
    class AcquireResult {
        private final boolean success;
        private final IdempotencyRecord existing;

        public AcquireResult(boolean success, IdempotencyRecord existing) {
            this.success = success;
            this.existing = existing;
        }

        public boolean isSuccess() { return success; }
        public IdempotencyRecord getExisting() { return existing; }
    }
}
