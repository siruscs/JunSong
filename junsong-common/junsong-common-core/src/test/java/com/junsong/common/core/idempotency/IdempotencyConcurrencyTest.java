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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 幂等切面 10 线程并发测试。
 *
 * 验证场景：
 * 1. 10 线程相同键相同请求 → 只有 1 个线程执行业务，其余被拒绝
 * 2. 10 线程相同键但 acquire 返回不同结果（模拟唯一索引竞争）→ 只有 1 个成功
 * 3. 跨租户相同键 → 两个租户各自独立执行（不互相阻断）
 * 4. 不同键相同请求 → 10 个线程全部执行
 *
 * 关键：模拟真实 MySQL 唯一索引行为——并发 acquire 时只有一个返回 success=true。
 *
 * @author junsong
 */
class IdempotencyConcurrencyTest {

    private IdempotencyAspect aspect;
    private IdempotencyRecordService recordService;
    private IdempotencyKeyResolver keyResolver;
    private ApplicationContext applicationContext;

    private static final String TEST_SCENE = "sale:create";

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

    private Idempotent idempotent(String scene) {
        return new Idempotent() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Idempotent.class; }
            @Override public String scene() { return scene; }
            @Override public boolean required() { return true; }
            @Override public int ttlSeconds() { return 2592000; }
            @Override public IdempotencyResultMode resultMode() { return IdempotencyResultMode.REFERENCE; }
            @Override public String keyResolver() { return ""; }
            @Override public boolean highRisk() { return true; }
            @Override public boolean failOpen() { return false; }
            @Override public IdempotencyRetryPolicy retryPolicy() { return IdempotencyRetryPolicy.REQUIRE_NEW_KEY; }
        };
    }

    private ProceedingJoinPoint mockJoinPoint(Object body, Object proceedResult) throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        try {
            Method realMethod = IdempotencyConcurrencyTest.class.getDeclaredMethod("dummyAnnotatedMethod");
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

    /**
     * 场景 1：10 线程相同键相同请求 → 只有 1 个线程执行业务。
     *
     * 模拟真实 MySQL 唯一索引：第一个 acquire 返回 success=true，
     * 其余 9 个返回 success=false + existing.status=PROCESSING。
     */
    @Test
    void tenThreadsSameKeyOnlyOneExecutes() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE);
        String key = "concurrent-key-001";
        when(keyResolver.resolve(any(), any())).thenReturn(key);

        // 模拟唯一索引竞争：用 AtomicInteger 保证只有一个线程拿到 success=true
        AtomicInteger winnerCounter = new AtomicInteger(0);
        IdempotencyRecord processingRecord = new IdempotencyRecord();
        processingRecord.setRecordId(1000L);
        processingRecord.setStatus("PROCESSING");
        processingRecord.setFingerprint(IdempotencyFingerprint.compute(null));

        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq(key), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    // 只有第一个线程返回 success=true
                    if (winnerCounter.compareAndSet(0, 1)) {
                        IdempotencyRecord newRecord = new IdempotencyRecord();
                        newRecord.setRecordId(1000L);
                        return new IdempotencyRecordService.AcquireResult(true, newRecord);
                    }
                    // 其余线程返回 PROCESSING 已有记录
                    return new IdempotencyRecordService.AcquireResult(false, processingRecord);
                });

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger businessExecCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
                    // 计数业务执行次数
                    doAnswer(inv -> {
                        businessExecCount.incrementAndGet();
                        return AjaxResult.success();
                    }).when(point).proceed();

                    Object result = aspect.around(point, ann);
                    successCount.incrementAndGet();
                } catch (ServiceException e) {
                    if (e.getMessage().contains("处理中")) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Throwable e) {
                    // 忽略其他异常
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(finishGate.await(10, TimeUnit.SECONDS), "10 线程应在 10 秒内完成");
        pool.shutdown();

        assertEquals(1, businessExecCount.get(), "业务方法只能被执行 1 次");
        assertEquals(1, successCount.get(), "只有 1 个线程成功返回");
        assertEquals(9, rejectedCount.get(), "9 个线程应被拒绝（处理中）");
    }

    /**
     * 场景 2：10 线程相同键，但第一个线程成功后标记 SUCCEEDED，
     * 后续线程拿到 SUCCEEDED 状态 → 返回重放响应。
     */
    @Test
    void tenThreadsSameKeyFirstSucceedsOthersGetReplay() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE);
        String key = "concurrent-key-002";
        when(keyResolver.resolve(any(), any())).thenReturn(key);

        AtomicInteger winnerCounter = new AtomicInteger(0);
        IdempotencyRecord succeededRecord = new IdempotencyRecord();
        succeededRecord.setRecordId(2000L);
        succeededRecord.setStatus("SUCCEEDED");
        succeededRecord.setFingerprint(IdempotencyFingerprint.compute(null));
        succeededRecord.setResourceType("fin_sale_record");
        succeededRecord.setResourceId("500");

        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq(key), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    if (winnerCounter.compareAndSet(0, 1)) {
                        IdempotencyRecord newRecord = new IdempotencyRecord();
                        newRecord.setRecordId(2000L);
                        return new IdempotencyRecordService.AcquireResult(true, newRecord);
                    }
                    return new IdempotencyRecordService.AcquireResult(false, succeededRecord);
                });

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threadCount);
        AtomicInteger businessExecCount = new AtomicInteger(0);
        AtomicInteger replayCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
                    doAnswer(inv -> {
                        businessExecCount.incrementAndGet();
                        return AjaxResult.success();
                    }).when(point).proceed();

                    Object result = aspect.around(point, ann);
                    if (result instanceof AjaxResult) {
                        AjaxResult ajax = (AjaxResult) result;
                        if (Boolean.TRUE.equals(ajax.get("idempotentReplay"))) {
                            replayCount.incrementAndGet();
                        }
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(finishGate.await(10, TimeUnit.SECONDS), "10 线程应在 10 秒内完成");
        pool.shutdown();

        assertEquals(1, businessExecCount.get(), "业务方法只能被执行 1 次");
        assertEquals(9, replayCount.get(), "9 个线程应收到重放响应");
        assertEquals(0, errorCount.get(), "不应有异常");
    }

    /**
     * 场景 3：跨租户相同键 → 两个租户各自独立执行。
     * 租户 1 和租户 2 使用相同的幂等键，但都能成功执行。
     */
    @Test
    void crossTenantSameKeyIndependentExecution() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE);
        String key = "cross-tenant-key-001";

        // 租户 1 的 mock
        when(keyResolver.resolve(any(), any())).thenReturn(key);

        // 按租户 ID 返回不同的 acquire 结果
        when(recordService.acquire(anyLong(), eq(TEST_SCENE), eq(key), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    Long tenantId = (Long) inv.getArgument(0);
                    IdempotencyRecord newRecord = new IdempotencyRecord();
                    newRecord.setRecordId(tenantId * 10000L); // 租户 1 → 10000, 租户 2 → 20000
                    return new IdempotencyRecordService.AcquireResult(true, newRecord);
                });

        AtomicInteger businessExecCount = new AtomicInteger(0);
        AtomicInteger tenant1Success = new AtomicInteger(0);
        AtomicInteger tenant2Success = new AtomicInteger(0);

        Runnable tenant1Task = () -> {
            TenantContext.setTenantId(1L);
            try {
                ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
                doAnswer(inv -> {
                    businessExecCount.incrementAndGet();
                    return AjaxResult.success();
                }).when(point).proceed();
                aspect.around(point, ann);
                tenant1Success.incrementAndGet();
            } catch (Throwable ignored) {
            }
        };

        Runnable tenant2Task = () -> {
            TenantContext.setTenantId(2L);
            try {
                ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
                doAnswer(inv -> {
                    businessExecCount.incrementAndGet();
                    return AjaxResult.success();
                }).when(point).proceed();
                aspect.around(point, ann);
                tenant2Success.incrementAndGet();
            } catch (Throwable ignored) {
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(2);

        pool.submit(() -> { try { startGate.await(); tenant1Task.run(); } catch (InterruptedException ignored) {} finally { finishGate.countDown(); } });
        pool.submit(() -> { try { startGate.await(); tenant2Task.run(); } catch (InterruptedException ignored) {} finally { finishGate.countDown(); } });

        startGate.countDown();
        assertTrue(finishGate.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(2, businessExecCount.get(), "两个租户应各自执行业务 1 次");
        assertEquals(1, tenant1Success.get(), "租户 1 应成功");
        assertEquals(1, tenant2Success.get(), "租户 2 应成功");
    }

    /**
     * 场景 4：10 线程不同键相同请求 → 全部执行。
     */
    @Test
    void tenThreadsDifferentKeysAllExecute() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE);

        // 每个线程使用不同的键
        when(keyResolver.resolve(any(), any())).thenAnswer(inv -> {
            String threadName = Thread.currentThread().getName();
            return "key-" + threadName;
        });

        when(recordService.acquire(anyLong(), eq(TEST_SCENE), anyString(), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    String key = (String) inv.getArgument(2);
                    IdempotencyRecord newRecord = new IdempotencyRecord();
                    newRecord.setRecordId(System.currentTimeMillis());
                    return new IdempotencyRecordService.AcquireResult(true, newRecord);
                });

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threadCount);
        AtomicInteger businessExecCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ProceedingJoinPoint point = mockJoinPoint(null, AjaxResult.success());
                    doAnswer(inv -> {
                        businessExecCount.incrementAndGet();
                        return AjaxResult.success();
                    }).when(point).proceed();
                    aspect.around(point, ann);
                } catch (Throwable ignored) {
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(finishGate.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(10, businessExecCount.get(), "10 个不同键应全部执行业务");
    }

    /**
     * 场景 5：相同键不同参数 → 冲突。
     * 第一个线程用 body A 成功，第二个线程用 body B（不同指纹）→ 冲突。
     */
    @Test
    void sameKeyDifferentBodyThrowsConflict() throws Throwable {
        Idempotent ann = idempotent(TEST_SCENE);
        String key = "conflict-key-001";
        when(keyResolver.resolve(any(), any())).thenReturn(key);

        // body A 的指纹
        String bodyA = "{\"amount\":100}";
        String bodyBFingerprint = IdempotencyFingerprint.compute("{\"amount\":200}");

        AtomicInteger winnerCounter = new AtomicInteger(0);
        IdempotencyRecord succeededRecord = new IdempotencyRecord();
        succeededRecord.setRecordId(3000L);
        succeededRecord.setStatus("SUCCEEDED");
        succeededRecord.setFingerprint(IdempotencyFingerprint.compute(bodyA));
        succeededRecord.setResourceType("fin_sale_record");
        succeededRecord.setResourceId("777");

        when(recordService.acquire(eq(1L), eq(TEST_SCENE), eq(key), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    if (winnerCounter.compareAndSet(0, 1)) {
                        IdempotencyRecord newRecord = new IdempotencyRecord();
                        newRecord.setRecordId(3000L);
                        return new IdempotencyRecordService.AcquireResult(true, newRecord);
                    }
                    return new IdempotencyRecordService.AcquireResult(false, succeededRecord);
                });

        // 第一个线程：body A → 成功
        ProceedingJoinPoint pointA = mockJoinPoint(bodyA, AjaxResult.success());
        Object resultA = aspect.around(pointA, ann);
        assertNotNull(resultA);

        // 第二个线程：body B → 冲突
        ProceedingJoinPoint pointB = mockJoinPoint("{\"amount\":200}", AjaxResult.success());
        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(pointB, ann));
        assertTrue(ex.getMessage().contains("冲突"), "不同参数相同键应返回冲突");
    }
}
