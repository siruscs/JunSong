package com.junsong.finance.service.impl;

import com.junsong.finance.service.ExpenseOcrService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseOcrServiceImplTest
{
    private final ExpenseOcrServiceImpl service = new ExpenseOcrServiceImpl();

    @Test
    void parseResultPrefersActualPaidAmountOverDiscountAndFreight()
    {
        String text = "拼多多\n商品优惠 ¥ 12.00\n运费 ¥ 6.00\n实付款 ¥ 128.50\n支付时间 2026-06-13 12:30:45\n蓝莓果切采购";

        ExpenseOcrService.OcrResult result = service.parseText(text);

        assertEquals("拼多多", result.getPlatform());
        assertEquals("128.50", result.getAmount());
        assertEquals("2026-06-13", result.getDate());
        assertTrue(result.getContent().contains("蓝莓果切采购"));
    }

    @Test
    void parseResultSupportsPaymentScreenshotsWithoutCurrencySymbol()
    {
        String text = "微信支付\n交易时间 2026年6月3日 08:06:09\n付款金额 39.9\n付款方 商户采购\n鲜果包装袋";

        ExpenseOcrService.OcrResult result = service.parseText(text);

        assertEquals("微信", result.getPlatform());
        assertEquals("39.90", result.getAmount());
        assertEquals("2026-06-03", result.getDate());
        assertTrue(result.getContent().contains("鲜果包装袋"));
    }
}
