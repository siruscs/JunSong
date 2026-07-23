package com.junsong.system.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.redis.service.RedisService;

/**
 * 微信会话管理服务
 *
 * <p>通过租户级 epoch（版本号）实现一键使指定租户所有微信登录会话失效。
 * 不解除微信绑定、不删除绑定历史，仅让当前已登录的微信 Token 在下次请求时失效。
 * 密码登录会话不受影响。</p>
 */
@Service
public class SysWechatSessionService
{
    private static final Logger log = LoggerFactory.getLogger(SysWechatSessionService.class);

    @Autowired
    private RedisService redisService;

    /**
     * 获取指定租户的当前微信会话版本号
     *
     * @param tenantId 租户ID
     * @return 当前版本号，未设置时返回 0
     */
    public Long getEpoch(Long tenantId)
    {
        if (tenantId == null)
        {
            return 0L;
        }
        // Redis 反序列化时小数值可能为 Integer，直接强转 Long 会 ClassCastException
        Object raw = redisService.getCacheObject(CacheConstants.WECHAT_SESSION_EPOCH_KEY + tenantId);
        if (raw == null)
        {
            return 0L;
        }
        if (raw instanceof Number)
        {
            return ((Number) raw).longValue();
        }
        try
        {
            return Long.parseLong(raw.toString());
        }
        catch (NumberFormatException e)
        {
            log.warn("getEpoch: invalid value, raw={}", raw);
            return 0L;
        }
    }

    /**
     * 递增指定租户的微信会话版本号，使该租户所有已登录的微信会话失效
     *
     * <p>原子操作，幂等安全。每次调用版本号递增 1。
     * 返回包含操作前后版本号和租户ID的结果。</p>
     *
     * @param tenantId 租户ID
     * @param operator 操作人用户名
     * @param reason   操作原因
     * @return 包含 tenantId、previousEpoch、currentEpoch 的 Map
     */
    public Map<String, Object> revokeAllWechatSessions(Long tenantId, String operator, String reason)
    {
        Long previousEpoch = getEpoch(tenantId);
        Long currentEpoch = redisService.increment(CacheConstants.WECHAT_SESSION_EPOCH_KEY + tenantId);

        log.info("微信会话一键失效: tenantId={}, operator={}, reason={}, previousEpoch={}, currentEpoch={}",
                tenantId, operator, reason, previousEpoch, currentEpoch);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("previousEpoch", previousEpoch);
        result.put("currentEpoch", currentEpoch);
        // 不返回用户 openid 或 Token 明细
        return result;
    }
}
