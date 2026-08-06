package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class MemberCampaignPolicyValidatorTest
{
    @Test
    void acceptsIndependentRulesForOneProductAndPeriod()
    {
        List<MemberCampaignPackageCalculator.PackageRule> rules = List.of(
                new MemberCampaignPackageCalculator.PackageRule("P5", 5, 1),
                new MemberCampaignPackageCalculator.PackageRule("P10", 10, 3),
                new MemberCampaignPackageCalculator.PackageRule("P24", 24, 8));

        assertDoesNotThrow(() -> MemberCampaignPolicyValidator.validatePackageRules(rules));
    }

    @Test
    void rejectsDuplicatePurchaseQuantities()
    {
        List<MemberCampaignPackageCalculator.PackageRule> rules = List.of(
                new MemberCampaignPackageCalculator.PackageRule("P5A", 5, 1),
                new MemberCampaignPackageCalculator.PackageRule("P5B", 5, 2));

        assertThrows(IllegalArgumentException.class,
                () -> MemberCampaignPolicyValidator.validatePackageRules(rules));
    }

    @Test
    void rejectsNonIncreasingGiftRules()
    {
        List<MemberCampaignPackageCalculator.PackageRule> rules = List.of(
                new MemberCampaignPackageCalculator.PackageRule("P5", 5, 3),
                new MemberCampaignPackageCalculator.PackageRule("P10", 10, 1));

        assertThrows(IllegalArgumentException.class,
                () -> MemberCampaignPolicyValidator.validatePackageRules(rules));
    }
}
