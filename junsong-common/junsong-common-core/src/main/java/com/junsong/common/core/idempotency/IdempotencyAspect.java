package com.junsong.common.core.idempotency;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.domain.AjaxResult;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 幂等切面。
 *
 * 拦截 @Idempotent 注解的方法，执行原子幂等校验：
 * 1. 解析幂等键（默认从请求头 X-Idempotency-Key 读取）
 * 2. 计算请求体指纹
 * 3. 原子占位（INSERT IGNORE，MySQL 唯一索引兜底）
 * 4. 业务执行后标记成功/失败
 * 5. 重复请求返回原结果或 409
 *
 * 性能：普通写请求只做一次原子 INSERT，不增加显著开销。
 * 异常：幂等记录库不可用时，高风险接口失败关闭，低风险接口按配置处理。
 *
 * @author junsong
 */
@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);

    @Autowired
    private IdempotencyRecordService idempotencyRecordService;

    @Autowired
    private IdempotencyKeyResolver defaultKeyResolver;

    @Autowired(required = false)
    private IdempotencyFastPath fastPath;

    @Autowired
    private ApplicationContext applicationContext;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        // 1. 解析幂等键
        IdempotencyKeyResolver resolver = resolveKeyResolver(idempotent);
        String idempotencyKey = resolver.resolve(point, idempotent);

        // 2. 缺键处理
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            if (idempotent.required()) {
                throw new ServiceException("缺少幂等键，请刷新页面后重试");
            }
            // required=false 时跳过幂等，直接执行业务
            return point.proceed();
        }

        // 3. 键长度校验
        if (idempotencyKey.length() > 128) {
            throw new ServiceException("幂等键长度不能超过 128 字符");
        }

        // 4. 租户上下文
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止幂等操作");
        }

        // 5. 计算请求体指纹
        // 注意：必须正确识别 @RequestBody 参数，排除 @RequestHeader 等非业务参数
        // 详见 IdempotencyFingerprintExtractor
        String fingerprint = IdempotencyFingerprintExtractor.extract(point);

        // 5.1 暴露幂等键给业务层（用于填充业务表 idempotency_key 列实现 DB 唯一索引兜底）
        IdempotencyResultStore.currentKey(idempotencyKey);
        try {
            return doIdempotentFlow(point, idempotent, idempotencyKey, tenantId, fingerprint);
        } finally {
            IdempotencyResultStore.clearKey();
        }
    }

    /**
     * 幂等校验主流程（由 around 调用，确保 currentKey 在 finally 中被清理）。
     */
    private Object doIdempotentFlow(ProceedingJoinPoint point, Idempotent idempotent,
                                     String idempotencyKey, Long tenantId, String fingerprint) throws Throwable {

        // 6. Redis 快速路径（可选）：命中则跳过数据库查询
        if (fastPath != null) {
            String cachedStatus = fastPath.getStatus(tenantId, idempotent.scene(), idempotencyKey);
            if ("PROCESSING".equals(cachedStatus)) {
                throw new ServiceException("请求处理中，请稍后重试");
            }
            if ("SUCCEEDED".equals(cachedStatus)) {
                // Redis 命中，但仍需 MySQL 确认（Redis 不作为最终正确性来源）
                // 这里不返回，继续走 MySQL 流程
            }
        }

        // 7. 原子占位（捕获数据库异常，按 highRisk 配置处理）
        IdempotencyRecordService.AcquireResult acquireResult;
        try {
            acquireResult = idempotencyRecordService.acquire(
                    tenantId, idempotent.scene(), idempotencyKey,
                    fingerprint, idempotent.ttlSeconds());
        } catch (DataAccessException e) {
            log.error("幂等记录库不可用: scene={}, key={}", idempotent.scene(), idempotencyKey, e);
            if (idempotent.highRisk() || !idempotent.failOpen()) {
                throw new ServiceException("幂等校验服务暂时不可用，请稍后重试");
            }
            // 低风险 + failOpen=true → 放行
            log.warn("低风险接口 failOpen 放行: scene={}", idempotent.scene());
            return point.proceed();
        }

        // 8. 新建成功 → 执行业务
        if (acquireResult.isSuccess()) {
            Long recordId = acquireResult.getExisting().getRecordId();
            try {
                Object result = point.proceed();
                // 标记成功 + 写入业务结果引用
                IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
                if (ref != null) {
                    idempotencyRecordService.markSucceeded(recordId,
                            ref.getResourceType(), ref.getResourceId(), ref.getResultSummary());
                } else if (idempotent.resultMode() == IdempotencyResultMode.FULL) {
                    writeFullResult(recordId, result);
                } else {
                    idempotencyRecordService.markSucceeded(recordId, null, null, null);
                }
                // Redis 快速路径标记成功
                if (fastPath != null) {
                    fastPath.markSucceeded(tenantId, idempotent.scene(), idempotencyKey);
                }
                return result;
            } catch (Throwable ex) {
                // 标记失败，允许重试
                IdempotencyResultStore.getAndClear(); // 清理 ThreadLocal
                String errorSummary = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                idempotencyRecordService.markFailed(recordId, errorSummary);
                // Redis 快速路径标记失败（允许重试）
                if (fastPath != null) {
                    fastPath.markFailed(tenantId, idempotent.scene(), idempotencyKey);
                }
                throw ex;
            }
        }

        // 8. 已存在记录 → 重复请求处理
        IdempotencyRecord existing = acquireResult.getExisting();
        if (existing == null) {
            // 极端情况：INSERT IGNORE 返回 0 但 SELECT 也读不到（可能事务隔离）
            // 高风险接口失败关闭，低风险接口按配置
            if (idempotent.highRisk() || !idempotent.failOpen()) {
                throw new ServiceException("幂等校验异常，请稍后重试");
            }
            return point.proceed();
        }

        // 8.1 相同键不同指纹 → 冲突
        if (existing.getFingerprint() != null
                && !existing.getFingerprint().equals(fingerprint)) {
            log.warn("幂等键冲突: scene={}, key={}, existingFp={}, newFp={}",
                    idempotent.scene(), idempotencyKey,
                    existing.getFingerprint(), fingerprint);
            throw new ServiceException("幂等键冲突，请使用新键重试");
        }

        // 8.2 按状态处理
        IdempotencyStatus status = IdempotencyStatus.fromString(existing.getStatus());
        if (status == IdempotencyStatus.PROCESSING) {
            // 处理中 → 409
            throw new ServiceException("请求处理中，请稍后重试");
        }
        if (status == IdempotencyStatus.SUCCEEDED) {
            // 成功 → 返回原结果引用
            return buildIdempotentResponse(existing, ((MethodSignature) point.getSignature()).getReturnType());
        }
        if (status == IdempotencyStatus.FAILED) {
            // 失败状态处理：根据 retryPolicy 决定行为
            // - REQUIRE_NEW_KEY：抛出原失败原因，要求新键重试（保守策略，默认）
            // - ALLOW_SAME_KEY：尝试 CAS 占位（FAILED → PROCESSING），成功后重新执行业务
            //
            // 设计要点：
            // 1. 业务明确失败（参数校验/业务规则违反）：业务一定未执行，重试安全
            // 2. 响应丢失但业务可能已成功（网络超时/5xx 系统异常）：
            //    - 如果业务实际已成功 → 状态应为 SUCCEEDED，不会进入此分支
            //    - 如果业务未执行 → 状态为 FAILED，允许同键重试
            //    - 如果业务执行中 → 状态为 PROCESSING，已在上面的分支返回"处理中"
            // 3. SUCCEEDED 状态永远返回原结果，不会重新执行业务
            if (idempotent.retryPolicy() == IdempotencyRetryPolicy.ALLOW_SAME_KEY) {
                // 尝试 CAS 重新占位
                boolean reacquired;
                try {
                    reacquired = idempotencyRecordService.reacquire(
                            existing.getRecordId(), fingerprint, idempotent.ttlSeconds());
                } catch (DataAccessException e) {
                    log.error("幂等记录库不可用（重试占位）: scene={}, key={}",
                            idempotent.scene(), idempotencyKey, e);
                    if (idempotent.highRisk() || !idempotent.failOpen()) {
                        throw new ServiceException("幂等校验服务暂时不可用，请稍后重试");
                    }
                    return point.proceed();
                }

                if (!reacquired) {
                    // 其他线程已占位 → 返回处理中
                    throw new ServiceException("请求处理中，请稍后重试");
                }

                // 占位成功 → 重新执行业务（与首次执行走相同的成功/失败标记流程）
                // 注意：旧 errorSummary 和 resourceRef 已被 reacquire 清空
                log.info("幂等键重试占位成功: scene={}, key={}", idempotent.scene(), idempotencyKey);
                Long recordId = existing.getRecordId();
                try {
                    Object result = point.proceed();
                    IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
                    if (ref != null) {
                        idempotencyRecordService.markSucceeded(recordId,
                                ref.getResourceType(), ref.getResourceId(), ref.getResultSummary());
                    } else if (idempotent.resultMode() == IdempotencyResultMode.FULL) {
                        writeFullResult(recordId, result);
                    } else {
                        idempotencyRecordService.markSucceeded(recordId, null, null, null);
                    }
                    if (fastPath != null) {
                        fastPath.markSucceeded(tenantId, idempotent.scene(), idempotencyKey);
                    }
                    return result;
                } catch (Throwable ex) {
                    IdempotencyResultStore.getAndClear();
                    String errorSummary = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    idempotencyRecordService.markFailed(recordId, errorSummary);
                    if (fastPath != null) {
                        fastPath.markFailed(tenantId, idempotent.scene(), idempotencyKey);
                    }
                    throw ex;
                }
            }

            // REQUIRE_NEW_KEY：抛出原失败原因，要求新键重试
            throw new ServiceException("上次请求失败: " + existing.getErrorSummary()
                    + "，请使用新幂等键重试");
        }

        // 未知状态，失败关闭
        throw new ServiceException("幂等记录状态异常: " + existing.getStatus());
    }

    /**
     * 解析键解析器：优先使用注解指定的，否则用默认的 HeaderKeyResolver。
     */
    private IdempotencyKeyResolver resolveKeyResolver(Idempotent idempotent) {
        String beanName = idempotent.keyResolver();
        if (beanName == null || beanName.isEmpty()) {
            return defaultKeyResolver;
        }
        try {
            return applicationContext.getBean(beanName, IdempotencyKeyResolver.class);
        } catch (Exception e) {
            log.warn("自定义键解析器不存在，回退到默认: {}", beanName);
            return defaultKeyResolver;
        }
    }

    /**
     * FULL 模式写入完整 AjaxResult 的 JSON 摘要。
     */
    private void writeFullResult(Long recordId, Object result) {
        if (result == null) {
            return;
        }
        if (result instanceof AjaxResult) {
            AjaxResult ajax = (AjaxResult) result;
            Object data = ajax.get(AjaxResult.DATA_TAG);
            String dataStr = data != null ? String.valueOf(data) : null;
            String summary = "code=" + ajax.get(AjaxResult.CODE_TAG)
                    + ", msg=" + ajax.get(AjaxResult.MSG_TAG);
            if (summary.length() > 500) {
                summary = summary.substring(0, 500);
            }
            idempotencyRecordService.markSucceeded(recordId, "AjaxResult", dataStr, summary);
        }
    }

    /**
     * 构建幂等重放响应。
     * REFERENCE 模式返回资源引用，FULL 模式返回原结果摘要。
     */
    private Object buildIdempotentResponse(IdempotencyRecord existing, Class<?> returnType) {
        if (R.class.isAssignableFrom(returnType)) {
            java.util.Map<String, Object> replay = new java.util.LinkedHashMap<>();
            if (existing.getResourceType() != null) replay.put("resourceType", existing.getResourceType());
            if (existing.getResourceId() != null) replay.put("resourceId", existing.getResourceId());
            replay.put("idempotentReplay", true);
            if (existing.getResultSummary() != null) replay.put("resultSummary", existing.getResultSummary());
            else replay.put("message", "请求已成功处理，请勿重复提交");
            return R.ok(replay);
        }
        AjaxResult ajax = AjaxResult.success();
        if (existing.getResourceType() != null) {
            ajax.put("resourceType", existing.getResourceType());
        }
        if (existing.getResourceId() != null) {
            ajax.put("resourceId", existing.getResourceId());
        }
        if (existing.getResultSummary() != null) {
            ajax.put("idempotentReplay", true);
            ajax.put("resultSummary", existing.getResultSummary());
        } else {
            ajax.put("idempotentReplay", true);
            ajax.put("message", "请求已成功处理，请勿重复提交");
        }
        return ajax;
    }
}
