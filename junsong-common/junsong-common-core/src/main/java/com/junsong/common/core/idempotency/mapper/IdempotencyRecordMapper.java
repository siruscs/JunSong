package com.junsong.common.core.idempotency.mapper;

import org.apache.ibatis.annotations.Param;
import com.junsong.common.core.idempotency.IdempotencyRecord;

/**
 * 幂等记录 Mapper 接口。
 *
 * 关键 SQL：
 * - insertIfAbsent：原子占位，唯一键冲突返回 0
 * - selectByUniqueKey：读取已有记录
 * - updateStatus：状态流转（PROCESSING → SUCCEEDED / FAILED）
 * - updateResult：写入业务结果引用
 *
 * <p>不使用 @Mapper 注解，避免被 MybatisAutoConfiguration 的 AutoConfiguredMapperScannerRegistrar
 * 无条件扫描（网关无 SqlSessionFactory 会导致启动失败）。
 * 由业务模块的 @EnableCustomConfig -> @MapperScan("com.junsong.**.mapper") 扫描注册。</p>
 *
 * @author junsong
 */
public interface IdempotencyRecordMapper {

    /**
     * 原子插入幂等记录（占位）。
     * 唯一键 uk_idempotency_tenant_scene_key 冲突时返回 0。
     */
    int insertIfAbsent(IdempotencyRecord record);

    /**
     * 按租户+场景+键查询幂等记录。
     */
    IdempotencyRecord selectByUniqueKey(@Param("tenantId") Long tenantId,
                                        @Param("scene") String scene,
                                        @Param("idempotencyKey") String idempotencyKey);

    /**
     * 更新状态（PROCESSING → SUCCEEDED / FAILED）。
     * 谓词：record_id + status=PROCESSING
     */
    int updateStatus(@Param("recordId") Long recordId,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus,
                     @Param("errorSummary") String errorSummary);

    /**
     * 重新占位（FAILED → PROCESSING），用于同键安全重试场景。
     *
     * 谓词：record_id + status='FAILED'
     * 操作：status='PROCESSING'，清空 error_summary 和旧业务结果引用，刷新指纹和过期时间。
     *
     * 这是 CAS 操作，并发安全：多个线程同时重试同一 FAILED 记录时，
     * 只有第一个线程能成功，其他线程返回 0（看到 PROCESSING 后会被切面拦截为"请求处理中"）。
     *
     * @param recordId 记录ID
     * @param newFingerprint 新请求体指纹（同键不同参数会被前置校验拦截，此处仅刷新时间戳）
     * @param expireTime 新的过期时间
     * @return 1=占位成功；0=状态已变化（其他线程已占位或已被清理）
     */
    int reacquire(@Param("recordId") Long recordId,
                  @Param("newFingerprint") String newFingerprint,
                  @Param("expireTime") java.util.Date expireTime);

    /**
     * 写入业务结果引用。
     */
    int updateResult(@Param("recordId") Long recordId,
                     @Param("resourceType") String resourceType,
                     @Param("resourceId") String resourceId,
                     @Param("resultSummary") String resultSummary);

    /**
     * 归档过期记录（仅归档 SUCCEEDED/FAILED，PROCESSING 需人工核查）。
     */
    int archiveExpired(@Param("batchSize") int batchSize);
}
