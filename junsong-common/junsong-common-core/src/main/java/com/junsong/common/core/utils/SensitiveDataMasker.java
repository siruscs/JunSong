package com.junsong.common.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R25 敏感数据脱敏工具。
 *
 * <p>用于操作审计快照、操作日志参数、开放平台返回等场景，
 * 确保 appSecret/token/password/mobile/idCard/bankCard/email/webhookUrl
 * 等字段不落明文。</p>
 */
public class SensitiveDataMasker
{
    private SensitiveDataMasker() {}

    /** 需要完全掩码的 JSON 字段名 */
    private static final Pattern FULL_MASK_FIELDS = Pattern.compile(
        "(?i)\"(password|oldPassword|newPassword|confirmPassword|appSecret|secret|token|accessToken|refreshToken)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /** mobile / phone 字段 */
    private static final Pattern MOBILE_FIELDS = Pattern.compile(
        "(?i)\"(mobile|phone)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /** idCard / idNo 字段 */
    private static final Pattern IDCARD_FIELDS = Pattern.compile(
        "(?i)\"(idCard|idNo)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /** bankCard 字段 */
    private static final Pattern BANKCARD_FIELDS = Pattern.compile(
        "(?i)\"(bankCard)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /** email 字段 */
    private static final Pattern EMAIL_FIELDS = Pattern.compile(
        "(?i)\"(email|address)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /** webhookUrl / callbackUrl 字段 */
    private static final Pattern URL_FIELDS = Pattern.compile(
        "(?i)\"(webhookUrl|callbackUrl)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /** openId / unionId 字段 */
    private static final Pattern OPENID_FIELDS = Pattern.compile(
        "(?i)\"(openId|unionId)\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 对 JSON 字符串中的敏感字段值进行脱敏。
     *
     * @param input 可能包含敏感字段的 JSON 字符串
     * @return 脱敏后的字符串；输入为 null 时返回空串
     */
    public static String maskSensitive(String input)
    {
        if (input == null || input.isEmpty())
        {
            return "";
        }
        String result = input;
        result = replaceAll(result, FULL_MASK_FIELDS, "\"$1\":\"******\"");
        result = replaceAll(result, MOBILE_FIELDS, (m) -> "\"" + m.group(1) + "\":\"" + maskMobile(m.group(2)) + "\"");
        result = replaceAll(result, IDCARD_FIELDS, (m) -> "\"" + m.group(1) + "\":\"" + maskIdCard(m.group(2)) + "\"");
        result = replaceAll(result, BANKCARD_FIELDS, (m) -> "\"" + m.group(1) + "\":\"" + maskBankCard(m.group(2)) + "\"");
        result = replaceAll(result, EMAIL_FIELDS, (m) -> "\"" + m.group(1) + "\":\"" + maskEmail(m.group(2)) + "\"");
        result = replaceAll(result, URL_FIELDS, (m) -> "\"" + m.group(1) + "\":\"" + maskUrl(m.group(2)) + "\"");
        result = replaceAll(result, OPENID_FIELDS, "\"$1\":\"******\"");
        return result;
    }

    /**
     * 手机号脱敏：保留前三位和后四位，例如 138****8000。
     */
    public static String maskMobile(String value)
    {
        if (value == null || value.length() < 7)
        {
            return "******";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    /**
     * 身份证脱敏：保留前六位和后四位，例如 110101********1234。
     */
    public static String maskIdCard(String value)
    {
        if (value == null || value.length() < 10)
        {
            return "******";
        }
        int stars = value.length() - 10;
        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, 6);
        for (int i = 0; i < stars; i++)
        {
            sb.append("*");
        }
        sb.append(value, value.length() - 4, value.length());
        return sb.toString();
    }

    /**
     * 银行卡脱敏：保留后四位，例如 **** **** **** 1234。
     */
    public static String maskBankCard(String value)
    {
        if (value == null || value.length() < 4)
        {
            return "******";
        }
        String last4 = value.substring(value.length() - 4);
        return "**** **** **** " + last4;
    }

    /**
     * 邮箱脱敏：保留首字母和域名，例如 a***@example.com。
     */
    public static String maskEmail(String value)
    {
        if (value == null || value.isEmpty())
        {
            return "******";
        }
        int at = value.indexOf('@');
        if (at <= 0 || at >= value.length() - 1)
        {
            return "******";
        }
        String domain = value.substring(at);
        return value.charAt(0) + "***" + domain;
    }

    /**
     * URL 脱敏：保留协议和域名，不返回 path/query。
     * 例如 https://bot.example.com/hook?token=abc → https://bot.example.com/***
     */
    public static String maskUrl(String value)
    {
        if (value == null || value.isEmpty())
        {
            return "******";
        }
        // 提取协议://域名 部分
        int schemeEnd = value.indexOf("://");
        if (schemeEnd < 0)
        {
            return "******";
        }
        int pathStart = value.indexOf('/', schemeEnd + 3);
        if (pathStart < 0)
        {
            return value + "/***";
        }
        return value.substring(0, pathStart) + "/***";
    }

    // ============ internal helpers ============

    @FunctionalInterface
    private interface Replacer
    {
        String replace(Matcher m);
    }

    private static String replaceAll(String input, Pattern pattern, String replacement)
    {
        return pattern.matcher(input).replaceAll(replacement);
    }

    private static String replaceAll(String input, Pattern pattern, Replacer replacer)
    {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find())
        {
            m.appendReplacement(sb, replacer.replace(m).replace("\\", "\\\\").replace("$", "\\$"));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
