package com.junsong.common.core.idempotency;

import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.web.domain.AjaxResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;

/**
 * 幂等框架性能基准测试（组件基准，Mock 路径）。
 *
 * 对比启用/禁用幂等框架时的性能差异：
 * - 禁用：直接调用业务方法
 * - 启用（无重复）：每个请求使用不同键，全部走新建路径
 * - 启用（有重复）：相同键重复请求，走重放路径
 *
 * 测量指标：P50/P95/P99 延迟、吞吐量
 *
 * 重要说明：
 * 1. 本测试使用 Mock 的 IdempotencyRecordService，不涉及真实数据库 IO。
 * 2. 因此本测试只能称为"组件基准"，不能作为系统真实性能结论。
 * 3. 真实数据库性能基准见 {@link IdempotencyDatabaseIntegrationTest#realDatabase_performanceBenchmark}，
 *    包含真实 INSERT IGNORE / 唯一键冲突 / UPDATE CAS 等 SQL 操作。
 * 4. 完整系统性能（含 Spring AOP 实际调用、真实 Controller、真实 Service、真实事务、
 *    真实网络请求）需 DEV 环境部署后采集。
 *
 * @author junsong
 */
class IdempotencyPerformanceBenchmark {

    private IdempotencyAspect aspect;
    private IdempotencyRecordService recordService;
    private IdempotencyKeyResolver keyResolver;
    private ApplicationContext applicationContext;

    private static final String TEST_SCENE = "bench:sale";
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCH_ITERATIONS = 10000;
    private static final int CONCURRENT_THREADS = 10;
    private static final int REQUESTS_PER_THREAD = 1000;

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

    private Idempotent idempotent() {
        return new Idempotent() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Idempotent.class; }
            @Override public String scene() { return TEST_SCENE; }
            @Override public boolean required() { return true; }
            @Override public int ttlSeconds() { return 2592000; }
            @Override public IdempotencyResultMode resultMode() { return IdempotencyResultMode.REFERENCE; }
            @Override public String keyResolver() { return ""; }
            @Override public boolean highRisk() { return false; }
            @Override public boolean failOpen() { return false; }
            @Override public IdempotencyRetryPolicy retryPolicy() { return IdempotencyRetryPolicy.REQUIRE_NEW_KEY; }
        };
    }

    private ProceedingJoinPoint mockJoinPoint(Object body, Object proceedResult) throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        try {
            Method realMethod = IdempotencyPerformanceBenchmark.class.getDeclaredMethod("dummyMethod");
            when(signature.getMethod()).thenReturn(realMethod);
        } catch (NoSuchMethodException ignored) {}
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(body != null ? new Object[]{body} : new Object[0]);
        when(point.proceed()).thenReturn(proceedResult);
        return point;
    }

    @Idempotent(scene = "test:dummy")
    void dummyMethod() {}

    /**
     * 计算百分位数。
     */
    private double percentile(List<Long> latencies, double percentile) {
        if (latencies.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index) / 1_000_000.0; // 转换为毫秒
    }

    /**
     * 基准 1：禁用幂等框架（直接调用业务方法）。
     */
    @Test
    void benchmarkWithoutIdempotency() throws Throwable {
        Idempotent ann = idempotent();
        AjaxResult expectedResult = AjaxResult.success();

        // 模拟业务方法延迟（1us）
        when(keyResolver.resolve(any(), any())).thenReturn("bench-key-" + System.nanoTime());
        when(recordService.acquire(any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            IdempotencyRecord rec = new IdempotencyRecord();
            rec.setRecordId(System.nanoTime());
            return new IdempotencyRecordService.AcquireResult(true, rec);
        });

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
            aspect.around(point, ann);
        }

        // 基准测试
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>(BENCH_ITERATIONS));
        long startTime = System.nanoTime();

        for (int i = 0; i < BENCH_ITERATIONS; i++) {
            long reqStart = System.nanoTime();
            when(keyResolver.resolve(any(), any())).thenReturn("bench-key-" + i);
            ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
            aspect.around(point, ann);
            latencies.add(System.nanoTime() - reqStart);
        }

        long totalTime = System.nanoTime() - startTime;

        System.out.println("=== 启用幂等框架（无重复，全部新建）===");
        System.out.printf("迭代次数: %d%n", BENCH_ITERATIONS);
        System.out.printf("总耗时: %.2f ms%n", totalTime / 1_000_000.0);
        System.out.printf("吞吐量: %.0f ops/s%n", BENCH_ITERATIONS * 1_000_000_000.0 / totalTime);
        System.out.printf("P50 延迟: %.4f ms%n", percentile(latencies, 50));
        System.out.printf("P95 延迟: %.4f ms%n", percentile(latencies, 95));
        System.out.printf("P99 延迟: %.4f ms%n", percentile(latencies, 99));
        System.out.printf("平均延迟: %.4f ms%n", latencies.stream().mapToLong(l -> l).average().orElse(0) / 1_000_000.0);
    }

    /**
     * 基准 2：启用幂等框架，重复请求（走重放路径）。
     */
    @Test
    void benchmarkWithIdempotencyReplay() throws Throwable {
        Idempotent ann = idempotent();
        AjaxResult expectedResult = AjaxResult.success();
        String fixedKey = "replay-key-fixed";

        // 模拟重复请求：acquire 返回 SUCCEEDED 已有记录
        IdempotencyRecord succeededRecord = new IdempotencyRecord();
        succeededRecord.setRecordId(9999L);
        succeededRecord.setStatus("SUCCEEDED");
        succeededRecord.setFingerprint(IdempotencyFingerprint.compute(null));
        succeededRecord.setResourceType("fin_sale_record");
        succeededRecord.setResourceId("888");

        when(keyResolver.resolve(any(), any())).thenReturn(fixedKey);
        when(recordService.acquire(any(), any(), eq(fixedKey), any(), anyInt()))
                .thenReturn(new IdempotencyRecordService.AcquireResult(false, succeededRecord));

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
            aspect.around(point, ann);
        }

        // 基准测试
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>(BENCH_ITERATIONS));
        long startTime = System.nanoTime();

        for (int i = 0; i < BENCH_ITERATIONS; i++) {
            long reqStart = System.nanoTime();
            ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
            aspect.around(point, ann);
            latencies.add(System.nanoTime() - reqStart);
        }

        long totalTime = System.nanoTime() - startTime;

        System.out.println("=== 启用幂等框架（重复请求，走重放路径）===");
        System.out.printf("迭代次数: %d%n", BENCH_ITERATIONS);
        System.out.printf("总耗时: %.2f ms%n", totalTime / 1_000_000.0);
        System.out.printf("吞吐量: %.0f ops/s%n", BENCH_ITERATIONS * 1_000_000_000.0 / totalTime);
        System.out.printf("P50 延迟: %.4f ms%n", percentile(latencies, 50));
        System.out.printf("P95 延迟: %.4f ms%n", percentile(latencies, 95));
        System.out.printf("P99 延迟: %.4f ms%n", percentile(latencies, 99));
        System.out.printf("平均延迟: %.4f ms%n", latencies.stream().mapToLong(l -> l).average().orElse(0) / 1_000_000.0);
    }

    /**
     * 基准 3：10 线程并发，启用幂等框架。
     */
    @Test
    void benchmarkConcurrentWithIdempotency() throws Throwable {
        Idempotent ann = idempotent();
        AjaxResult expectedResult = AjaxResult.success();

        AtomicInteger keyCounter = new AtomicInteger(0);
        when(keyResolver.resolve(any(), any())).thenAnswer(inv -> "concurrent-key-" + keyCounter.incrementAndGet());
        when(recordService.acquire(any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            IdempotencyRecord rec = new IdempotencyRecord();
            rec.setRecordId((long) keyCounter.get());
            return new IdempotencyRecordService.AcquireResult(true, rec);
        });

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
            aspect.around(point, ann);
        }

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>(CONCURRENT_THREADS * REQUESTS_PER_THREAD));
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(CONCURRENT_THREADS);
        AtomicLong totalOperations = new AtomicLong(0);

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
                        long reqStart = System.nanoTime();
                        ProceedingJoinPoint point = mockJoinPoint(null, expectedResult);
                        aspect.around(point, ann);
                        latencies.add(System.nanoTime() - reqStart);
                        totalOperations.incrementAndGet();
                    }
                } catch (Throwable ignored) {
                } finally {
                    finishGate.countDown();
                }
            });
        }

        long startTime = System.nanoTime();
        startGate.countDown();
        assertTrue(finishGate.await(60, TimeUnit.SECONDS), "并发测试应在 60 秒内完成");
        long totalTime = System.nanoTime() - startTime;
        pool.shutdown();

        int total = CONCURRENT_THREADS * REQUESTS_PER_THREAD;
        System.out.println("=== 10 线程并发，启用幂等框架 ===");
        System.out.printf("线程数: %d，每线程请求数: %d，总请求数: %d%n", CONCURRENT_THREADS, REQUESTS_PER_THREAD, total);
        System.out.printf("实际完成: %d%n", totalOperations.get());
        System.out.printf("总耗时: %.2f ms%n", totalTime / 1_000_000.0);
        System.out.printf("吞吐量: %.0f ops/s%n", totalOperations.get() * 1_000_000_000.0 / totalTime);
        System.out.printf("P50 延迟: %.4f ms%n", percentile(latencies, 50));
        System.out.printf("P95 延迟: %.4f ms%n", percentile(latencies, 95));
        System.out.printf("P99 延迟: %.4f ms%n", percentile(latencies, 99));
        System.out.printf("平均延迟: %.4f ms%n", latencies.stream().mapToLong(l -> l).average().orElse(0) / 1_000_000.0);

        assertEquals(total, totalOperations.get(), "所有请求应完成");
    }

    private static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }

    private static void assertEquals(long expected, long actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
}
