package com.junsong.common.core.idempotency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等状态机测试。
 */
class IdempotencyStatusTest {

    @Test
    void fromStringParsesValidStates() {
        assertEquals(IdempotencyStatus.PROCESSING, IdempotencyStatus.fromString("PROCESSING"));
        assertEquals(IdempotencyStatus.SUCCEEDED, IdempotencyStatus.fromString("SUCCEEDED"));
        assertEquals(IdempotencyStatus.FAILED, IdempotencyStatus.fromString("FAILED"));
    }

    @Test
    void fromStringIsCaseInsensitive() {
        assertEquals(IdempotencyStatus.PROCESSING, IdempotencyStatus.fromString("processing"));
        assertEquals(IdempotencyStatus.SUCCEEDED, IdempotencyStatus.fromString("succeeded"));
        assertEquals(IdempotencyStatus.FAILED, IdempotencyStatus.fromString("failed"));
    }

    @Test
    void fromStringReturnsNullForNull() {
        assertNull(IdempotencyStatus.fromString(null));
        assertNull(IdempotencyStatus.fromString(""));
    }

    @Test
    void fromStringReturnsNullForUnknown() {
        assertNull(IdempotencyStatus.fromString("UNKNOWN"));
        assertNull(IdempotencyStatus.fromString("PENDING"));
    }
}
