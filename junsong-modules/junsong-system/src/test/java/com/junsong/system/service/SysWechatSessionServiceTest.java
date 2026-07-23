package com.junsong.system.service;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysWechatSessionService 单元测试
 *
 * <p>覆盖 Task 6A 验收场景：
 * <ul>
 *   <li>微信会话失效（版本递增）</li>
 *   <li>不同租户不受影响（独立 key）</li>
 *   <li>重复点击幂等（版本继续递增）</li>
 *   <li>结果不包含 openid 或 Token 明细</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SysWechatSessionServiceTest
{
    @Mock
    private com.junsong.common.redis.service.RedisService redisService;

    @InjectMocks
    private SysWechatSessionService wechatSessionService;

    private static final String EPOCH_KEY_PREFIX = "wechat:session:epoch:";

    @BeforeEach
    void setUp()
    {
        // 默认租户 1 的版本号为 0（未设置）
        lenient().when(redisService.getCacheObject(EPOCH_KEY_PREFIX + "1")).thenReturn(null);
        // 租户 2 的版本号为 5
        lenient().when(redisService.getCacheObject(EPOCH_KEY_PREFIX + "2")).thenReturn(5L);
    }

    @Test
    void getEpoch_tenantNotSet_returnsZero()
    {
        Long epoch = wechatSessionService.getEpoch(1L);
        assertEquals(0L, epoch);
    }

    @Test
    void getEpoch_tenantHasValue_returnsValue()
    {
        Long epoch = wechatSessionService.getEpoch(2L);
        assertEquals(5L, epoch);
    }

    @Test
    void getEpoch_nullTenantId_returnsZero()
    {
        Long epoch = wechatSessionService.getEpoch(null);
        assertEquals(0L, epoch);
    }

    @Test
    void revokeAllWechatSessions_incrementsEpochAndReturnsPreviousAndCurrent()
    {
        // 租户 2 当前版本 5，递增后应为 6
        when(redisService.increment(EPOCH_KEY_PREFIX + "2")).thenReturn(6L);

        Map<String, Object> result = wechatSessionService.revokeAllWechatSessions(2L, "admin", "安全审计");

        assertNotNull(result);
        assertEquals(2L, result.get("tenantId"));
        assertEquals(5L, result.get("previousEpoch"));
        assertEquals(6L, result.get("currentEpoch"));
        verify(redisService).increment(EPOCH_KEY_PREFIX + "2");
    }

    @Test
    void revokeAllWechatSessions_tenantNotSet_previousEpochZero()
    {
        // 租户 1 未设置版本号，previousEpoch=0，递增后为 1
        when(redisService.increment(EPOCH_KEY_PREFIX + "1")).thenReturn(1L);

        Map<String, Object> result = wechatSessionService.revokeAllWechatSessions(1L, "admin", null);

        assertEquals(1L, result.get("tenantId"));
        assertEquals(0L, result.get("previousEpoch"));
        assertEquals(1L, result.get("currentEpoch"));
    }

    @Test
    void revokeAllWechatSessions_resultDoesNotContainOpenidOrToken()
    {
        when(redisService.increment(anyString())).thenReturn(10L);

        Map<String, Object> result = wechatSessionService.revokeAllWechatSessions(1L, "admin", "test");

        String json = result.toString().toLowerCase();
        assertTrue(!json.contains("openid"), "结果不应包含 openid");
        assertTrue(!json.contains("token"), "结果不应包含 token 明细");
        assertTrue(!json.contains("unionid"), "结果不应包含 unionid");
    }

    @Test
    void revokeAllWechatSessions_differentTenantsUseDifferentKeys()
    {
        when(redisService.increment(EPOCH_KEY_PREFIX + "1")).thenReturn(1L);
        when(redisService.increment(EPOCH_KEY_PREFIX + "2")).thenReturn(6L);

        Map<String, Object> result1 = wechatSessionService.revokeAllWechatSessions(1L, "admin", "test");
        Map<String, Object> result2 = wechatSessionService.revokeAllWechatSessions(2L, "admin", "test");

        // 不同租户的版本号独立
        assertEquals(1L, result1.get("currentEpoch"));
        assertEquals(6L, result2.get("currentEpoch"));
        verify(redisService).increment(EPOCH_KEY_PREFIX + "1");
        verify(redisService).increment(EPOCH_KEY_PREFIX + "2");
    }

    @Test
    void revokeAllWechatSessions_repeatInvocationContinuesIncrementing()
    {
        // 模拟连续两次调用：第一次 0→1，第二次 1→2
        when(redisService.getCacheObject(EPOCH_KEY_PREFIX + "1")).thenReturn(null).thenReturn(1L);
        when(redisService.increment(EPOCH_KEY_PREFIX + "1")).thenReturn(1L).thenReturn(2L);

        Map<String, Object> first = wechatSessionService.revokeAllWechatSessions(1L, "admin", "first");
        Map<String, Object> second = wechatSessionService.revokeAllWechatSessions(1L, "admin", "second");

        assertEquals(0L, first.get("previousEpoch"));
        assertEquals(1L, first.get("currentEpoch"));
        assertEquals(1L, second.get("previousEpoch"));
        assertEquals(2L, second.get("currentEpoch"));
    }
}
