package com.junsong.member.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates one product's fixed-package policy before it becomes effective. */
public final class MemberCampaignPolicyValidator
{
    private MemberCampaignPolicyValidator()
    {
    }

    public static void validatePackageRules(List<MemberCampaignPackageCalculator.PackageRule> rules)
    {
        if (rules == null || rules.isEmpty())
        {
            throw new IllegalArgumentException("package rules must not be empty");
        }
        Set<Integer> quantities = new HashSet<>();
        int previousGiftQuantity = -1;
        for (MemberCampaignPackageCalculator.PackageRule rule : rules.stream()
                .sorted(Comparator.comparingInt(MemberCampaignPackageCalculator.PackageRule::purchaseQuantity))
                .toList())
        {
            if (!quantities.add(rule.purchaseQuantity()))
            {
                throw new IllegalArgumentException("package purchase quantities must be unique");
            }
            if (rule.giftQuantity() < previousGiftQuantity)
            {
                throw new IllegalArgumentException("gift quantity must not decrease at a higher package");
            }
            previousGiftQuantity = rule.giftQuantity();
        }
    }
}
