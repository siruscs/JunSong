package com.junsong.member.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Applies a single immutable ledger delta to the account snapshot. */
public final class MemberLoyaltyBalanceCalculator
{
    private MemberLoyaltyBalanceCalculator() { }

    public static Balance apply(BigDecimal currentPoints, long currentGrowth,
                                BigDecimal pointsDelta, long growthDelta)
    {
        if (currentPoints == null || pointsDelta == null || currentGrowth < 0)
        {
            throw new IllegalArgumentException("current loyalty balance is invalid");
        }
        BigDecimal points = currentPoints.add(pointsDelta).setScale(2, RoundingMode.HALF_UP);
        long growth = currentGrowth + growthDelta;
        if (points.signum() < 0 || growth < 0)
        {
            throw new IllegalArgumentException("loyalty balance cannot be negative");
        }
        return new Balance(points, growth);
    }

    public record Balance(BigDecimal points, long growth) { }
}
