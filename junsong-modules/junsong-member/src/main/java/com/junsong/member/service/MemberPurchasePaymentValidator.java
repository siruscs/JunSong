package com.junsong.member.service;

import java.math.BigDecimal;

public final class MemberPurchasePaymentValidator
{
    private MemberPurchasePaymentValidator() { }

    public static void validate(BigDecimal receivable, BigDecimal payment)
    {
        if (receivable == null || payment == null || receivable.signum() < 0
                || payment.signum() <= 0 || payment.compareTo(receivable) > 0)
        {
            throw new IllegalArgumentException("payment exceeds receivable or is invalid");
        }
    }
}
