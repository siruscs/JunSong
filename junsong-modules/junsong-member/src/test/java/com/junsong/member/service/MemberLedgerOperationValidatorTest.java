package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MemberLedgerOperationValidatorTest
{
    @Test
    void acceptsPositiveAndNegativeAdjustments()
    {
        assertDoesNotThrow(() -> MemberLedgerOperationValidator.validate("MANUAL", "A-1", "k-1", new BigDecimal("1.00")));
        assertDoesNotThrow(() -> MemberLedgerOperationValidator.validate("REVERSAL", "A-1", "k-2", new BigDecimal("-1.00")));
    }

    @Test
    void rejectsMissingSourceOrDedupKey()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberLedgerOperationValidator.validate("MANUAL", "", "k-1", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> MemberLedgerOperationValidator.validate("MANUAL", "A-1", "", BigDecimal.ONE));
    }

    @Test
    void rejectsZeroDelta()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberLedgerOperationValidator.validate("MANUAL", "A-1", "k-1", BigDecimal.ZERO));
    }
}
