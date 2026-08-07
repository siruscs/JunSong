package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MemberLoyaltyBalanceCalculatorTest
{
    @Test
    void addsPointsAndGrowthWithoutChangingScale()
    {
        MemberLoyaltyBalanceCalculator.Balance result =
                MemberLoyaltyBalanceCalculator.apply(new BigDecimal("10.00"), 20L,
                        new BigDecimal("2.35"), 5L);

        assertEquals(new BigDecimal("12.35"), result.points());
        assertEquals(25L, result.growth());
    }

    @Test
    void rejectsNegativePointsBalance()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberLoyaltyBalanceCalculator.apply(new BigDecimal("1.00"), 0L,
                        new BigDecimal("-2.00"), 0L));
    }
}
