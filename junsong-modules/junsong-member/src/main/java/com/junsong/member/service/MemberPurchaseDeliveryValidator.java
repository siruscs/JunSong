package com.junsong.member.service;

import java.math.BigDecimal;

public final class MemberPurchaseDeliveryValidator
{
    private MemberPurchaseDeliveryValidator() { }

    public static void validate(BigDecimal remainingSale, BigDecimal remainingGift,
                                BigDecimal saleDelivery, BigDecimal giftDelivery)
    {
        if (remainingSale == null || remainingGift == null || saleDelivery == null || giftDelivery == null
                || remainingSale.signum() < 0 || remainingGift.signum() < 0
                || saleDelivery.signum() < 0 || giftDelivery.signum() < 0
                || saleDelivery.compareTo(remainingSale) > 0
                || giftDelivery.compareTo(remainingGift) > 0
                || saleDelivery.add(giftDelivery).signum() == 0)
        {
            throw new IllegalArgumentException("delivery quantity exceeds remaining quantity");
        }
    }
}
