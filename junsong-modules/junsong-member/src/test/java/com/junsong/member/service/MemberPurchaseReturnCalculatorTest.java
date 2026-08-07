package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MemberPurchaseReturnCalculatorTest
{
    @Test
    void calculatesRefundBySaleAndGiftTotalQuantity()
    {
        MemberPurchaseReturnCalculator calculator = new MemberPurchaseReturnCalculator();

        assertEquals(new BigDecimal("376.85"), calculator.weightedUnitPrice("4899.00", "10.000", "3.000"));
        assertEquals(new BigDecimal("2261.08"), calculator.refundAmount("4899.00", "10.000", "3.000", "5.000", "1.000"));
    }

    @Test
    void fullReturnKeepsOriginalRefundAmount()
    {
        MemberPurchaseReturnCalculator calculator = new MemberPurchaseReturnCalculator();

        assertEquals(new BigDecimal("4899.00"), calculator.refundAmount("4899.00", "10.000", "3.000", "10.000", "3.000"));
    }

    @Test
    void rejectsReturnQuantityAboveOriginalTotal()
    {
        MemberPurchaseReturnCalculator calculator = new MemberPurchaseReturnCalculator();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> calculator.refundAmount("4899.00", "10.000", "3.000", "11.000", "0.000"));
    }
}
