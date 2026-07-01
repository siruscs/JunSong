package com.junsong.system.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感配置脱敏测试
 *
 * 验证 SysConfigServiceImpl 的 isSensitiveKey 和 maskConfigValue
 * 方法能正确识别敏感配置键并对值进行脱敏。
 */
class SysConfigMaskingTest
{
    // ── isSensitiveKey：敏感关键词子串匹配 ──

    @Test
    void passwordKeywordShouldBeSensitive()
    {
        assertTrue(SysConfigServiceImpl.isSensitiveKey("sys.user.initPassword"));
        assertTrue(SysConfigServiceImpl.isSensitiveKey("db.password"));
        assertTrue(SysConfigServiceImpl.isSensitiveKey("smtp_password"));
    }

    @Test
    void secretKeywordShouldBeSensitive()
    {
        assertTrue(SysConfigServiceImpl.isSensitiveKey("api.secret"));
        assertTrue(SysConfigServiceImpl.isSensitiveKey("wechat.appSecret"));
    }

    @Test
    void tokenKeywordShouldBeSensitive()
    {
        assertTrue(SysConfigServiceImpl.isSensitiveKey("access.token"));
        assertTrue(SysConfigServiceImpl.isSensitiveKey("refreshToken"));
    }

    @Test
    void credentialKeywordShouldBeSensitive()
    {
        assertTrue(SysConfigServiceImpl.isSensitiveKey("oss.credential"));
        assertTrue(SysConfigServiceImpl.isSensitiveKey("serviceCredentialId"));
    }

    @Test
    void keySegmentShouldBeSensitive()
    {
        assertTrue(SysConfigServiceImpl.isSensitiveKey("api.key"), ".key 结尾应敏感");
        assertTrue(SysConfigServiceImpl.isSensitiveKey("key.value"), "key. 开头应敏感");
        assertTrue(SysConfigServiceImpl.isSensitiveKey("app.api.key"), ".key 结尾应敏感");
        assertTrue(SysConfigServiceImpl.isSensitiveKey("api_key"), "_key 结尾应敏感");
    }

    @Test
    void keyAsSubstringShouldNotBeSensitive()
    {
        assertFalse(SysConfigServiceImpl.isSensitiveKey("keyboard.layout"), "keyboard 不应匹配");
        assertFalse(SysConfigServiceImpl.isSensitiveKey("monkey.count"), "monkey 不应匹配");
        assertFalse(SysConfigServiceImpl.isSensitiveKey("sys.keyword.list"), "keyword 不应匹配");
    }

    @Test
    void nonSensitiveKeysShouldPass()
    {
        assertFalse(SysConfigServiceImpl.isSensitiveKey("sys.index.skinName"));
        assertFalse(SysConfigServiceImpl.isSensitiveKey("sys.account.registerUser"));
        assertFalse(SysConfigServiceImpl.isSensitiveKey("sys.login.blackIPList"));
        assertFalse(SysConfigServiceImpl.isSensitiveKey("sys.account.captchaEnabled"));
    }

    @Test
    void nullKeyShouldNotBeSensitive()
    {
        assertFalse(SysConfigServiceImpl.isSensitiveKey(null));
    }

    // ── maskConfigValue：脱敏逻辑 ──

    @Test
    void maskShouldReturnMaskedForSensitiveKey()
    {
        assertEquals("******", SysConfigServiceImpl.maskConfigValue("sys.user.initPassword", "123456"));
    }

    @Test
    void maskShouldReturnOriginalForNonSensitiveKey()
    {
        assertEquals("skin-blue", SysConfigServiceImpl.maskConfigValue("sys.index.skinName", "skin-blue"));
    }

    @Test
    void maskShouldHandleNullValue()
    {
        assertEquals("******", SysConfigServiceImpl.maskConfigValue("db.password", null));
        assertNull(SysConfigServiceImpl.maskConfigValue("sys.index.skinName", null));
    }

    @Test
    void maskedValueShouldAlwaysBeSixStars()
    {
        String masked = SysConfigServiceImpl.maskConfigValue("api.secret", "my-super-long-secret-value");
        assertEquals("******", masked, "脱敏值应固定为 ******");
    }
}
