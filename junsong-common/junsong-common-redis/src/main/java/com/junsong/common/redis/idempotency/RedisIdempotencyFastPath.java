package com.junsong.common.redis.idempotency;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.junsong.common.core.idempotency.IdempotencyFastPath;

/**
 * 幂等 Redis 快速路径（可选加速层）。
 *
 * 设计原则：
 * - Redis 只作短期快速判断层（TTL ≤ 30 秒），不作为最终正确性来源
 * - Redis 不可用时自动回退到 MySQL 原子校验
 * - 高风险接口不得因 Redis 异常绕过幂等
 * - 使用 SET NX（仅当键不存在时设置）
 *
 * 性能：
 * - 命中 Redis 时避免一次数据库 INSERT
 * - 未命中时回退到 MySQL 原子占位
 *
 * @author junsong
 */
@Component
public class RedisIdempotencyFastPath implements IdempotencyFastPath {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyFastPath.class);
    private static final String KEY_PREFIX = "idem:";
    private static final long DEFAULT_TTL_SECONDS = 30;

    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 尝试标记"处理中"。
     * 使用 SET NX（仅当键不存在时设置），TTL 30 秒。
     *
     * @return true=新建成功（Redis 标记）；false=已存在（可能重复）
     */
    public boolean tryMarkProcessing(Long tenantId, String scene, String idempotencyKey) {
        return tryMarkProcessing(tenantId, scene, idempotencyKey, DEFAULT_TTL_SECONDS);
    }

    public boolean tryMarkProcessing(Long tenantId, String scene, String idempotencyKey, long ttlSeconds) {
        String redisKey = buildKey(tenantId, scene, idempotencyKey);
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                    redisKey, "PROCESSING", ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // Redis 异常时回退到 MySQL，不阻断流程
            log.warn("Redis 幂等快速路径异常，回退到 MySQL: scene={}, key={}", scene, idempotencyKey, e);
            return true; // 返回 true 让 MySQL 做最终判断
        }
    }

    /**
     * 标记成功。
     * 重复请求命中时由 MySQL 返回原结果，Redis 仅做短期加速。
     */
    public void markSucceeded(Long tenantId, String scene, String idempotencyKey) {
        String redisKey = buildKey(tenantId, scene, idempotencyKey);
        try {
            redisTemplate.opsForValue().set(redisKey, "SUCCEEDED",
                    DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 标记成功异常（忽略）: scene={}, key={}", scene, idempotencyKey, e);
        }
    }

    /**
     * 标记失败（允许重试）。
     * 失败后立即删除 Redis 标记，让客户端可以用新键重试。
     */
    public void markFailed(Long tenantId, String scene, String idempotencyKey) {
        String redisKey = buildKey(tenantId, scene, idempotencyKey);
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis 标记失败异常（忽略）: scene={}, key={}", scene, idempotencyKey, e);
        }
    }

    /**
     * 查询当前状态（仅用于加速查询，不作为最终判断）。
     */
    public String getStatus(Long tenantId, String scene, String idempotencyKey) {
        String redisKey = buildKey(tenantId, scene, idempotencyKey);
        try {
            Object val = redisTemplate.opsForValue().get(redisKey);
            return val != null ? String.valueOf(val) : null;
        } catch (Exception e) {
            log.warn("Redis 查询状态异常（回退到 MySQL）: scene={}, key={}", scene, idempotencyKey, e);
            return null;
        }
    }

    private String buildKey(Long tenantId, String scene, String idempotencyKey) {
        return KEY_PREFIX + tenantId + ":" + scene + ":" + idempotencyKey;
    }
}
