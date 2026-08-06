package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MemberPurchaseDeliveryValidatorTest
{
    @Test
    void acceptsDeliveryWithinRemainingQuantity()
    {
        assertDoesNotThrow(() -> MemberPurchaseDeliveryValidator.validate(
                new BigDecimal("5"), new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("1")));
    }

    @Test
    void rejectsDeliveryBeyondRemainingQuantity()
    {
        assertThrows(IllegalArgumentException.class, () -> MemberPurchaseDeliveryValidator.validate(
                new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("5"), new BigDecimal("1")));
    }

    @Test
    void rejectsNegativeComponent()
    {
        assertThrows(IllegalArgumentException.class, () -> MemberPurchaseDeliveryValidator.validate(
                new BigDecimal("5"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("-1")));
    }
}
