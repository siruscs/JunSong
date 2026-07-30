package com.junsong.common.core.idempotency;

/**
 * 幂等快速路径接口（可选）。
 *
 * 由 common-redis 模块实现（RedisIdempotencyFastPath）。
 * 当 Redis 在 classpath 时启用，否则跳过。
 * Redis 只作短期快速判断层，不作为最终正确性来源。
 *
 * @author junsong
 */
public interface IdempotencyFastPath {

    /**
     * 尝试标记"处理中"。
     * 使用 SET NX（仅当键不存在时设置），TTL 30 秒。
     *
     * @return true=新建成功或 Redis 异常（回退到 MySQL）；false=已存在（可能重复）
     */
    boolean tryMarkProcessing(Long tenantId, String scene, String idempotencyKey);

    /**
     * 标记成功。
     */
    void markSucceeded(Long tenantId, String scene, String idempotencyKey);

    /**
     * 标记失败（允许重试，删除标记）。
     */
    void markFailed(Long tenantId, String scene, String idempotencyKey);

    /**
     * 查询当前状态（仅用于加速查询，不作为最终判断）。
     */
    String getStatus(Long tenantId, String scene, String idempotencyKey);
}
