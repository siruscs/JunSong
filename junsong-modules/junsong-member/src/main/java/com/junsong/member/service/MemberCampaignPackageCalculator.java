package com.junsong.member.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Calculates a single purchase's fixed-package composition.
 * The calculator is deliberately independent of persistence and finance.
 */
public final class MemberCampaignPackageCalculator
{
    private MemberCampaignPackageCalculator()
    {
    }

    public static Result calculate(int purchaseQuantity, List<PackageRule> rules)
    {
        if (purchaseQuantity < 0)
        {
            throw new IllegalArgumentException("purchase quantity must not be negative");
        }
        if (rules == null)
        {
            throw new IllegalArgumentException("package rules must not be null");
        }

        List<PackageRule> ordered = rules.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PackageRule::purchaseQuantity).reversed())
                .toList();
        int remaining = purchaseQuantity;
        int giftQuantity = 0;
        List<String> packageCodes = new ArrayList<>();
        for (PackageRule rule : ordered)
        {
            while (remaining >= rule.purchaseQuantity())
            {
                remaining -= rule.purchaseQuantity();
                giftQuantity += rule.giftQuantity();
                packageCodes.add(rule.code());
            }
        }
        return new Result(purchaseQuantity, giftQuantity, purchaseQuantity + giftQuantity, packageCodes);
    }

    public record PackageRule(String code, int purchaseQuantity, int giftQuantity)
    {
        public PackageRule
        {
            if (code == null || code.isBlank())
            {
                throw new IllegalArgumentException("package code must not be blank");
            }
            if (purchaseQuantity <= 0 || giftQuantity < 0)
            {
                throw new IllegalArgumentException("package quantities are invalid");
            }
        }
    }

    public record Result(int purchaseQuantity, int giftQuantity, int totalQuantity, List<String> packageCodes)
    {
        public Result
        {
            packageCodes = List.copyOf(packageCodes);
        }
    }
}
