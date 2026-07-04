package com.junsong.common.core.utils;

/**
 * 敏感数据脱敏工具（占位实现，将在 Task 4 中完善）。
 * 当前仅做透传，保证 R25 审计快照流程可编译通过。
 */
public class SensitiveDataMasker
{
    public static String maskSensitive(String input)
    {
        return input;
    }

    public static String maskMobile(String value)
    {
        return value;
    }

    public static String maskIdCard(String value)
    {
        return value;
    }

    public static String maskBankCard(String value)
    {
        return value;
    }

    public static String maskEmail(String value)
    {
        return value;
    }

    public static String maskUrl(String value)
    {
        return value;
    }
}
