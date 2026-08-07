package com.junsong.member.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 会员购买单退货的含赠加权退款计算。 */
public class MemberPurchaseReturnCalculator
{
    private static final int QUANTITY_SCALE = 3;
    private static final int MONEY_SCALE = 2;

    public BigDecimal weightedUnitPrice(String totalAmount, String purchaseQuantity, String giftQuantity)
    {
        BigDecimal amount = money(totalAmount);
        BigDecimal totalQuantity = quantity(purchaseQuantity).add(quantity(giftQuantity));
        if (totalQuantity.signum() <= 0)
        {
            throw new IllegalArgumentException("原购买单总数量必须大于0");
        }
        return amount.divide(totalQuantity, 8, RoundingMode.HALF_UP).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal refundAmount(String totalAmount, String purchaseQuantity, String giftQuantity,
                                   String returnSaleQuantity, String returnGiftQuantity)
    {
        BigDecimal purchase = quantity(purchaseQuantity);
        BigDecimal gift = quantity(giftQuantity);
        BigDecimal returnSale = quantity(returnSaleQuantity);
        BigDecimal returnGift = quantity(returnGiftQuantity);
        if (returnSale.compareTo(purchase) > 0 || returnGift.compareTo(gift) > 0
                || returnSale.add(returnGift).compareTo(purchase.add(gift)) > 0)
        {
            throw new IllegalArgumentException("退货数量不能超过原购买单可退数量");
        }
        BigDecimal amount = money(totalAmount);
        BigDecimal total = purchase.add(gift);
        return amount.multiply(returnSale.add(returnGift)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantity(String value)
    {
        return new BigDecimal(value == null || value.isBlank() ? "0" : value)
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(String value)
    {
        return new BigDecimal(value == null || value.isBlank() ? "0" : value)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
