package com.junsong.common.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R25 敏感数据脱敏工具测试。
 */
class SensitiveDataMaskerTest
{
    @Test
    void masksKnownSensitiveFieldsInJsonLikeText()
    {
        String input = "{\"mobile\":\"13812348000\",\"idCard\":\"110101199001011234\",\"appSecret\":\"abcdef\",\"webhookUrl\":\"https://bot.example.com/hook?token=abc\"}";
        String masked = SensitiveDataMasker.maskSensitive(input);

        assertTrue(masked.contains("138****8000"), "mobile should be masked");
        assertTrue(masked.contains("110101********1234"), "idCard should be masked");
        assertTrue(masked.contains("\"appSecret\":\"******\""), "appSecret should be masked");
        assertTrue(masked.contains("https://bot.example.com/***"), "webhookUrl should be masked");
        assertFalse(masked.contains("abcdef"), "raw appSecret should not appear");
        assertFalse(masked.contains("token=abc"), "raw query param should not appear");
    }

    @Test
    void maskMobileKeepsFirst3AndLast4()
    {
        assertEquals("138****8000", SensitiveDataMasker.maskMobile("13812348000"));
    }

    @Test
    void maskMobileHandlesShortInput()
    {
        assertEquals("******", SensitiveDataMasker.maskMobile("123"));
    }

    @Test
    void maskIdCardKeepsFirst6AndLast4()
    {
        assertEquals("110101********1234", SensitiveDataMasker.maskIdCard("110101199001011234"));
    }

    @Test
    void maskBankCardKeepsLast4()
    {
        assertEquals("**** **** **** 1234", SensitiveDataMasker.maskBankCard("62220212345678901234"));
    }

    @Test
    void maskEmailKeepsFirstCharAndDomain()
    {
        assertEquals("a***@example.com", SensitiveDataMasker.maskEmail("alice@example.com"));
    }

    @Test
    void maskUrlKeepsProtocolAndDomain()
    {
        assertEquals("https://bot.example.com/***", SensitiveDataMasker.maskUrl("https://bot.example.com/hook?token=abc"));
    }

    @Test
    void maskUrlHandlesUrlWithoutPath()
    {
        assertEquals("https://example.com/***", SensitiveDataMasker.maskUrl("https://example.com"));
    }

    @Test
    void maskSensitiveHandlesNullInput()
    {
        assertEquals("", SensitiveDataMasker.maskSensitive(null));
    }

    @Test
    void maskSensitiveMasksTokenAndPassword()
    {
        String input = "{\"token\":\"bearer-xyz\",\"password\":\"secret123\"}";
        String masked = SensitiveDataMasker.maskSensitive(input);
        assertTrue(masked.contains("\"token\":\"******\""));
        assertTrue(masked.contains("\"password\":\"******\""));
        assertFalse(masked.contains("bearer-xyz"));
        assertFalse(masked.contains("secret123"));
    }

    @Test
    void maskSensitiveMasksBankCardAndEmail()
    {
        String input = "{\"bankCard\":\"6222021234561234\",\"email\":\"test@demo.com\"}";
        String masked = SensitiveDataMasker.maskSensitive(input);
        assertTrue(masked.contains("**** **** **** 1234"));
        assertTrue(masked.contains("t***@demo.com"));
    }
}
