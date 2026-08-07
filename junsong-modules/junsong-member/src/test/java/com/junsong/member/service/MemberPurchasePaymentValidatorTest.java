package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MemberPurchasePaymentValidatorTest
{
    @Test
    void acceptsPaymentWithinReceivable()
    {
        assertDoesNotThrow(() -> MemberPurchasePaymentValidator.validate(
                new BigDecimal("100.00"), new BigDecimal("30.00")));
    }

    @Test
    void rejectsPaymentBeyondReceivable()
    {
        assertThrows(IllegalArgumentException.class, () -> MemberPurchasePaymentValidator.validate(
                new BigDecimal("20.00"), new BigDecimal("30.00")));
    }

    @Test
    void rejectsNegativePayment()
    {
        assertThrows(IllegalArgumentException.class, () -> MemberPurchasePaymentValidator.validate(
                new BigDecimal("20.00"), new BigDecimal("-1.00")));
    }
}
