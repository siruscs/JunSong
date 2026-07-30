package com.junsong.common.core.idempotency;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.idempotency.mapper.IdempotencyRecordMapper;

/**
 * 幂等记录服务实现。
 *
 * 基于 MySQL 唯一索引做原子占位（INSERT IGNORE），捕获冲突后读取已有记录。
 * 不依赖 SELECT 后再 INSERT，避免并发窗口。
 *
 * <p>仅在有 IdempotencyRecordMapper（即有数据源）的模块才注册，
 * 网关等无数据源模块自动跳过。</p>
 *
 * @author junsong
 */
@Service
public class IdempotencyRecordServiceImpl implements IdempotencyRecordService {

    @Autowired
    private IdempotencyRecordMapper idempotencyRecordMapper;

    @Override
    public AcquireResult acquire(Long tenantId, String scene, String idempotencyKey,
                                  String fingerprint, int ttlSeconds) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setTenantId(tenantId);
        record.setScene(scene);
        record.setIdempotencyKey(idempotencyKey);
        record.setStatus(IdempotencyStatus.PROCESSING.name());
        record.setFingerprint(fingerprint);
        Date expireTime = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);
        record.setExpireTime(expireTime);

        int affected = idempotencyRecordMapper.insertIfAbsent(record);
        if (affected == 1) {
            // 新建成功
            return new AcquireResult(true, record);
        }

        // 唯一键冲突，读取已有记录
        IdempotencyRecord existing = idempotencyRecordMapper.selectByUniqueKey(
                tenantId, scene, idempotencyKey);
        return new AcquireResult(false, existing);
    }

    @Override
    public int markSucceeded(Long recordId, String resourceType, String resourceId, String resultSummary) {
        int affected = idempotencyRecordMapper.updateStatus(
                recordId,
                IdempotencyStatus.PROCESSING.name(),
                IdempotencyStatus.SUCCEEDED.name(),
                null);
        if (affected == 1 && resourceType != null) {
            idempotencyRecordMapper.updateResult(recordId, resourceType, resourceId, resultSummary);
        }
        return affected;
    }

    @Override
    public int markFailed(Long recordId, String errorSummary) {
        return idempotencyRecordMapper.updateStatus(
                recordId,
                IdempotencyStatus.PROCESSING.name(),
                IdempotencyStatus.FAILED.name(),
                truncate(errorSummary, 500));
    }

    @Override
    public boolean reacquire(Long recordId, String newFingerprint, int ttlSeconds) {
        java.util.Date expireTime = new java.util.Date(System.currentTimeMillis() + ttlSeconds * 1000L);
        int affected = idempotencyRecordMapper.reacquire(recordId, newFingerprint, expireTime);
        return affected == 1;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
