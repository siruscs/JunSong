package com.junsong.system.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.redis.service.RedisService;
import com.junsong.system.domain.SysConfig;
import com.junsong.system.mapper.SysConfigMapper;
import com.junsong.system.service.impl.SysConfigServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * 微信登录开关配置测试（Task 3A）
 *
 * 验证 mp.wechat.login.enabled 参数的读取逻辑：
 * - 值为 true 时返回启用
 * - 值为 false、缺失、非法值时返回关闭
 * - 读取异常时 fail-closed 返回关闭
 * - 支持按 tenantId 隔离查询
 */
@ExtendWith(MockitoExtension.class)
class SysWechatLoginConfigTest
{
    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private SysConfigServiceImpl configService;

    private static final String CONFIG_KEY = "mp.wechat.login.enabled";

    @BeforeEach
    void setUp()
    {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown()
    {
        TenantContext.clear();
    }

    // ── 值为 true → 返回 true ──

    @Test
    void valueIsTrue_returnsEnabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("true");
        assertTrue(configService.isWechatLoginEnabled(null));
    }

    @Test
    void valueIsTrueUppercase_returnsEnabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("TRUE");
        assertTrue(configService.isWechatLoginEnabled(null));
    }

    // ── 值为 false → 返回 false ──

    @Test
    void valueIsFalse_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("false");
        assertFalse(configService.isWechatLoginEnabled(null));
    }

    // ── 值缺失（空字符串）→ 返回 false ──

    @Test
    void valueIsEmpty_returnsDisabled()
    {
        // Redis 返回 null → Convert.toStr 返回 "" → isNotEmpty 为 false → 查 DB
        // DB 也返回 null → 查公共配置 → 公共配置也返回 null → 返回 EMPTY
        lenient().when(redisService.getCacheObject(anyString())).thenReturn(null);
        lenient().when(configMapper.selectConfig(any())).thenReturn(null);
        lenient().when(configMapper.selectPublicConfig(anyString())).thenReturn(null);
        assertFalse(configService.isWechatLoginEnabled(null));
    }

    // ── 非法值 → 返回 false ──

    @Test
    void valueIsInvalid_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("yes");
        assertFalse(configService.isWechatLoginEnabled(null));
    }

    @Test
    void valueIsOne_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("1");
        assertFalse(configService.isWechatLoginEnabled(null));
    }

    @Test
    void valueIsRandomString_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("abc");
        assertFalse(configService.isWechatLoginEnabled(null));
    }

    // ── 读取异常 → fail-closed 返回 false ──

    @Test
    void redisThrowsException_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString()))
                .thenThrow(new RuntimeException("Redis connection failed"));
        assertFalse(configService.isWechatLoginEnabled(null));
    }

    // ── 按 tenantId 隔离查询 ──

    @Test
    void withExplicitTenantId_returnsEnabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("true");
        assertTrue(configService.isWechatLoginEnabled(999L));
        // 调用后 TenantContext 应恢复为默认值
        assertEquals(TenantContext.DEFAULT_TENANT_ID, TenantContext.getTenantId());
    }

    @Test
    void withExplicitTenantId_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn("false");
        assertFalse(configService.isWechatLoginEnabled(888L));
        assertEquals(TenantContext.DEFAULT_TENANT_ID, TenantContext.getTenantId());
    }

    // ── DB 命中公共配置（tenant_id=0）──

    @Test
    void dbPublicConfigIsTrue_returnsEnabled()
    {
        // Redis 缓存未命中
        lenient().when(redisService.getCacheObject(anyString())).thenReturn(null);
        // 当前租户配置不存在
        lenient().when(configMapper.selectConfig(any())).thenReturn(null);
        // 公共配置（tenant_id=0）值为 true
        SysConfig publicConfig = new SysConfig();
        publicConfig.setConfigKey(CONFIG_KEY);
        publicConfig.setConfigValue("true");
        lenient().when(configMapper.selectPublicConfig(anyString())).thenReturn(publicConfig);

        assertTrue(configService.isWechatLoginEnabled(null));
    }

    @Test
    void dbPublicConfigIsFalse_returnsDisabled()
    {
        lenient().when(redisService.getCacheObject(anyString())).thenReturn(null);
        lenient().when(configMapper.selectConfig(any())).thenReturn(null);
        SysConfig publicConfig = new SysConfig();
        publicConfig.setConfigKey(CONFIG_KEY);
        publicConfig.setConfigValue("false");
        lenient().when(configMapper.selectPublicConfig(anyString())).thenReturn(publicConfig);

        assertFalse(configService.isWechatLoginEnabled(null));
    }
}
