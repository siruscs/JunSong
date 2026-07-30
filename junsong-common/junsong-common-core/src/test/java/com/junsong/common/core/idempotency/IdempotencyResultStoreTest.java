package com.junsong.common.core.idempotency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等结果存储工具测试。
 *
 * 验证 ThreadLocal 的设置、读取和清理行为。
 */
class IdempotencyResultStoreTest {

    @AfterEach
    void cleanup() {
        IdempotencyResultStore.getAndClear();
    }

    @Test
    void recordLongSetsResourceRef() {
        IdempotencyResultStore.record("fin_sale_record", 123L);

        IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
        assertNotNull(ref);
        assertEquals("fin_sale_record", ref.getResourceType());
        assertEquals("123", ref.getResourceId());
        assertNull(ref.getResultSummary());
    }

    @Test
    void recordStringSetsResourceRef() {
        IdempotencyResultStore.record("order", "ORD-2026-001");

        IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
        assertNotNull(ref);
        assertEquals("order", ref.getResourceType());
        assertEquals("ORD-2026-001", ref.getResourceId());
    }

    @Test
    void recordWithSummarySetsAllFields() {
        IdempotencyResultStore.record("fin_purchase", 456L, "采购单创建成功");

        IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
        assertNotNull(ref);
        assertEquals("fin_purchase", ref.getResourceType());
        assertEquals("456", ref.getResourceId());
        assertEquals("采购单创建成功", ref.getResultSummary());
    }

    @Test
    void getAndClearReturnsNullWhenNotSet() {
        IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
        assertNull(ref);
    }

    @Test
    void getAndClearRemovesThreadLocal() {
        IdempotencyResultStore.record("test", 1L);
        IdempotencyResultStore.getAndClear();

        // 第二次调用应返回 null
        IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
        assertNull(ref, "getAndClear 必须清除 ThreadLocal");
    }

    @Test
    void recordNullLongIdHandled() {
        IdempotencyResultStore.record("test", (Long) null);

        IdempotencyResultStore.ResourceRef ref = IdempotencyResultStore.getAndClear();
        assertNotNull(ref);
        assertEquals("test", ref.getResourceType());
        assertNull(ref.getResourceId());
    }
}
