package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MemberCampaignPackageCalculatorTest
{
    @Test
    void purchase15UsesFiveAndTenPackages()
    {
        List<MemberCampaignPackageCalculator.PackageRule> rules = List.of(
                new MemberCampaignPackageCalculator.PackageRule("P5", 5, 1),
                new MemberCampaignPackageCalculator.PackageRule("P10", 10, 3),
                new MemberCampaignPackageCalculator.PackageRule("P24", 24, 8));

        MemberCampaignPackageCalculator.Result result =
                MemberCampaignPackageCalculator.calculate(15, rules);

        assertEquals(15, result.purchaseQuantity());
        assertEquals(4, result.giftQuantity());
        assertEquals(19, result.totalQuantity());
        assertEquals(List.of("P10", "P5"), result.packageCodes());
    }

    @Test
    void purchase58UsesLargestPackagesThenRemainder()
    {
        List<MemberCampaignPackageCalculator.PackageRule> rules = List.of(
                new MemberCampaignPackageCalculator.PackageRule("P5", 5, 1),
                new MemberCampaignPackageCalculator.PackageRule("P10", 10, 3),
                new MemberCampaignPackageCalculator.PackageRule("P24", 24, 8),
                new MemberCampaignPackageCalculator.PackageRule("P48", 48, 16));

        MemberCampaignPackageCalculator.Result result =
                MemberCampaignPackageCalculator.calculate(58, rules);

        assertEquals(58, result.purchaseQuantity());
        assertEquals(19, result.giftQuantity());
        assertEquals(77, result.totalQuantity());
        assertEquals(List.of("P48", "P10"), result.packageCodes());
    }

    @Test
    void purchase4HasNoGiftPackage()
    {
        List<MemberCampaignPackageCalculator.PackageRule> rules = List.of(
                new MemberCampaignPackageCalculator.PackageRule("P5", 5, 1),
                new MemberCampaignPackageCalculator.PackageRule("P10", 10, 3));

        MemberCampaignPackageCalculator.Result result =
                MemberCampaignPackageCalculator.calculate(4, rules);

        assertEquals(0, result.giftQuantity());
        assertEquals(4, result.totalQuantity());
        assertEquals(List.of(), result.packageCodes());
    }
}
