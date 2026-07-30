package com.junsong.common.core.idempotency;

import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.web.domain.AjaxResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 幂等切面单元测试。
 *
 * 覆盖：
 * - 缺键（required=true 拒绝；required=false 放行）
 * - 超长键拒绝
 * - 新建成功占位 → 执行业务 → 标记成功
 * - 业务抛异常 → 标记失败 → 重新抛出
 * - 重复请求（PROCESSING）→ 拒绝
 * - 重复请求（SUCCEEDED）→ 返回原结果引用
 * - 重复请求（FAILED）→ 拒绝（要求新键重试）
 * - 相同键不同指纹 → 冲突
 * - 高风险接口 DB 不可用 → 失败关闭
 * - 低风险接口 DB 不可用 + failOpen=false → 失败关闭
 * - 低风险接口 DB 不可用 + failOpen=true → 放行
 */
class IdempotencyAspectTest {

    private IdempotencyAspect aspect;
    private IdempotencyRecordService recordService;
    private IdempotencyKeyResolver keyResolver;
    private ApplicationContext applicationContext;

    /** 默认测试 scene，与 mock 中的 scene 参数保持一致 */
    private static final String TEST_SCENE = "test:scene";

    @BeforeEach
    void setup() {
        aspect = new IdempotencyAspect();
        recordService = mock(IdempotencyRecordService.class);
        keyResolver = mock(IdempotencyKeyResolver.class);
        applicationContext = mock(ApplicationContext.class);

        org.springframework.test.util.ReflectionTestUtils.setField(aspect, "idempotencyRecordService", recordService);
        org.springframework.test.util.ReflectionTestUtils.setField(aspect, "defaultKeyResolver", keyResolver);
        org.springframework.test.util.ReflectionTestUtils.setField(aspect, "fastPath", null);
        org.springframework.test.util.ReflectionTestUtils.setField(aspect, "applicationContext", applicationContext);

        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        IdempotencyResultStore.getAndClear();
    }

    /**
     * 创建自定义 Idempotent 注解实例（绕过注解必须挂在方法上的限制）。
     */
    private Idempotent idempotent(String scene, boolean required, boolean highRisk, boolean failOpen) {
        return new Idempotent() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Idempotent.class; }
            @Override public String scene() { return scene; }
            @Override public boolean required() { return required; }
            @Override public int ttlSeconds() { return 86400; }
            @Override public IdempotencyResultMode resultMode() { return IdempotencyResultMode.REFERENCE; }
            @Override public String keyResolver() { return ""; }
            @Override public boolean highRisk() { return highRisk; }
            @Override public boolean failOpen() { return failOpen; }
            @Override public IdempotencyRetryPolicy retryPolicy() { return IdempotencyRetryPolicy.REQUIRE_NEW_KEY; }
        };
    }

    private ProceedingJoinPoint mockJoinPoint(Object body, Object proceedResult) throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        try {
            Method realMethod = IdempotencyAspectTest.class.getDeclaredMethod("dummyAnnotatedMethod");
            when(signature.getMethod()).thenReturn(realMethod);
        } catch (NoSuchMethodException ignored) {
        }
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(body != null ? new Object[]{body} : new Object[0]);
        when(point.proceed()).thenReturn(proceedResult);
        return point;
    }

    @Idempotent(scene = "test:dummy")
    void dummyAnnotatedMethod() {}

    // =========================================================================
    // 缺键处理
    // =========================================================================

    @Test
    void missingKeyWithRequiredTrueThrows() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn(null);

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("缺少幂等键"));
        verify(point, never()).proceed();
    }

    @Test
    void missingKeyWithRequiredFalseSkipsIdempotency() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, false, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn(null);

        AjaxResult expectedResult = AjaxResult.success("ok");
        ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);

        Object result = aspect.around(point, ann);
        assertEquals(expectedResult, result);
        verify(recordService, never()).acquire(any(), any(), any(), any(), anyInt());
    }

    // =========================================================================
    // 键长度校验
    // =========================================================================

    @Test
    void overlongKeyThrows() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        String longKey = "x".repeat(129);
        when(keyResolver.resolve(any(), any())).thenReturn(longKey);

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("128"));
        verify(point, never()).proceed();
    }

    @Test
    void emptyKeyTreatedAsMissing() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("");

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("缺少幂等键"));
    }

    // =========================================================================
    // 新建成功占位 → 执行业务 → 标记成功
    // =========================================================================

    @Test
    void newRecordExecutesBusinessAndMarksSucceeded() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-success");

        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setRecordId(100L);
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-success"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(true, newRecord));

        AjaxResult expectedResult = AjaxResult.success();
        ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);

        Object result = aspect.around(point, ann);
        assertEquals(expectedResult, result);

        verify(recordService).markSucceeded(eq(100L), isNull(), isNull(), isNull());
        verify(recordService, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void newRecordWithResourceRefStoresReference() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-ref");

        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setRecordId(200L);
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-ref"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(true, newRecord));

        AjaxResult expectedResult = AjaxResult.success();
        ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);

        // 模拟业务层设置资源引用
        doAnswer(invocation -> {
            IdempotencyResultStore.record("fin_sale_record", 999L);
            return expectedResult;
        }).when(point).proceed();

        Object result = aspect.around(point, ann);
        assertEquals(expectedResult, result);

        verify(recordService).markSucceeded(eq(200L), eq("fin_sale_record"), eq("999"), isNull());
    }

    // =========================================================================
    // 业务抛异常 → 标记失败 → 重新抛出
    // =========================================================================

    @Test
    void businessExceptionMarksFailedAndRethrows() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-fail");

        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setRecordId(300L);
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-fail"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(true, newRecord));

        ProceedingJoinPoint point = mockJoinPoint(null, null);
        when(point.proceed()).thenThrow(new ServiceException("业务校验失败"));

        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertEquals("业务校验失败", ex.getMessage());

        verify(recordService).markFailed(eq(300L), contains("业务校验失败"));
        verify(recordService, never()).markSucceeded(anyLong(), any(), any(), any());
    }

    // =========================================================================
    // 重复请求处理
    // =========================================================================

    @Test
    void duplicateProcessingRequestThrows409() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-dup");

        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setRecordId(400L);
        existing.setStatus("PROCESSING");
        existing.setFingerprint(IdempotencyFingerprint.compute(null));
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-dup"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(false, existing));

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("处理中"));
        verify(point, never()).proceed();
    }

    @Test
    void duplicateSucceededRequestReturnsReplayResponse() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-succeeded");

        // 指纹需匹配（body=null → fingerprint="null"）
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setRecordId(500L);
        existing.setStatus("SUCCEEDED");
        existing.setFingerprint("null");
        existing.setResourceType("fin_sale_record");
        existing.setResourceId("888");
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-succeeded"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(false, existing));

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        Object result = aspect.around(point, ann);

        assertInstanceOf(AjaxResult.class, result);
        AjaxResult ajax = (AjaxResult) result;
        assertEquals(200, ajax.get(AjaxResult.CODE_TAG));
        assertEquals("fin_sale_record", ajax.get("resourceType"));
        assertEquals("888", ajax.get("resourceId"));
        assertEquals(true, ajax.get("idempotentReplay"));
        verify(point, never()).proceed();
    }

    @Test
    void duplicateFailedRequestThrowsWithRetryHint() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-failed");

        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setRecordId(600L);
        existing.setStatus("FAILED");
        existing.setFingerprint("null");
        existing.setErrorSummary("数据库连接超时");
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-failed"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(false, existing));

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("数据库连接超时"));
        assertTrue(ex.getMessage().contains("新幂等键"));
        verify(point, never()).proceed();
    }

    // =========================================================================
    // 相同键不同指纹 → 冲突
    // =========================================================================

    @Test
    void sameKeyDifferentFingerprintThrowsConflict() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-conflict");

        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setRecordId(700L);
        existing.setStatus("SUCCEEDED");
        existing.setFingerprint("different-fingerprint-aaa");
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq("key-conflict"), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(false, existing));

        // body=null → fingerprint="null"，与 existing 的 "different-fingerprint-aaa" 不同
        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("冲突"));
        verify(point, never()).proceed();
    }

    // =========================================================================
    // DB 不可用处理
    // =========================================================================

    @Test
    void highRiskDbUnavailableFailsClosed() throws Throwable {
        Idempotent ann = idempotent("finance:post", true, true, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-hr");

        org.springframework.dao.DataAccessResourceFailureException dbEx =
                new org.springframework.dao.DataAccessResourceFailureException("DB down");
        when(recordService.acquire(any(), any(), any(), any(), anyInt())).thenThrow(dbEx);

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("不可用") || ex.getMessage().contains("稍后重试"));
        verify(point, never()).proceed();
    }

    @Test
    void lowRiskFailOpenTrueDbUnavailableProceeds() throws Throwable {
        Idempotent ann = idempotent("notice:read", true, false, true);
        when(keyResolver.resolve(any(), any())).thenReturn("key-fo");

        org.springframework.dao.DataAccessResourceFailureException dbEx =
                new org.springframework.dao.DataAccessResourceFailureException("DB down");
        when(recordService.acquire(any(), any(), any(), any(), anyInt())).thenThrow(dbEx);

        AjaxResult expectedResult = AjaxResult.success();
        ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
        Object result = aspect.around(point, ann);
        assertEquals(expectedResult, result);
        verify(point).proceed();
    }

    @Test
    void lowRiskFailOpenFalseDbUnavailableFailsClosed() throws Throwable {
        Idempotent ann = idempotent("notice:read", true, false, false);
        when(keyResolver.resolve(any(), any())).thenReturn("key-fo-false");

        org.springframework.dao.DataAccessResourceFailureException dbEx =
                new org.springframework.dao.DataAccessResourceFailureException("DB down");
        when(recordService.acquire(any(), any(), any(), any(), anyInt())).thenThrow(dbEx);

        ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(point, ann));
        assertTrue(ex.getMessage().contains("不可用") || ex.getMessage().contains("稍后重试"));
        verify(point, never()).proceed();
    }

    // =========================================================================
    // 键长度边界
    // =========================================================================

    @Test
    void keyOfExactly128CharsIsAccepted() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE, true, false, false);
        String exactKey = "x".repeat(128);
        when(keyResolver.resolve(any(), any())).thenReturn(exactKey);

        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setRecordId(800L);
        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq(exactKey), anyString(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(true, newRecord));

        AjaxResult expectedResult = AjaxResult.success();
        ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);

        Object result = aspect.around(point, ann);
        assertEquals(expectedResult, result);
        verify(recordService).markSucceeded(eq(800L), isNull(), isNull(), isNull());
    }
}
