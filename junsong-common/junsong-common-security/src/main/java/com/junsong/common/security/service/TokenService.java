package com.junsong.common.security.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.utils.JwtUtils;
import com.junsong.common.core.utils.ServletUtils;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.utils.ip.IpUtils;
import com.junsong.common.core.utils.uuid.IdUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.model.LoginUser;

/**
 * token验证处理
 * 
 * @author junsong
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private RedisService redisService;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private final static long TOKEN_EXPIRE_TIME = CacheConstants.EXPIRATION;

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;

    private final static Long TOKEN_REFRESH_THRESHOLD_MINUTES = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /**
     * 创建令牌（PC端，受单点登录参数控制）
     */
    public Map<String, Object> createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        Long userId = loginUser.getSysUser().getUserId();
        String userName = loginUser.getSysUser().getUserName();
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr());
        loginUser.setAuthSource("PASSWORD");

        boolean isKickout = false;
        // 检查是否开启单点登录
        String singleLoginConfig = redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.login.singleLogin");
        boolean singleLoginEnabled = !"false".equals(singleLoginConfig);

        if (singleLoginEnabled)
        {
            // 单点登录：踢出旧会话
            String userTokenKey = CacheConstants.USER_TOKEN_KEY + userId;
            String oldTokenUuid = redisService.getCacheObject(userTokenKey);
            if (StringUtils.isNotEmpty(oldTokenUuid))
            {
                redisService.deleteObject(getTokenKey(oldTokenUuid));
                // 写入踢出标记（30秒有效期），被踢用户可通过此标记判断是被踢出还是token过期
                redisService.setCacheObject(CacheConstants.SYS_CONFIG_KEY + "kickout:" + userId, "1", 30L, TimeUnit.SECONDS);
                isKickout = true;
            }
            // 重要：必须刷新 token 到 Redis
            refreshToken(loginUser);
            // 记录 userId -> token 映射
            redisService.setCacheObject(userTokenKey, token, TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        else
        {
            refreshToken(loginUser);
        }

        // Jwt存储信息
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, userId);
        claimsMap.put(SecurityConstants.DETAILS_USERNAME, userName);

        // 接口返回信息
        Map<String, Object> rspMap = new HashMap<String, Object>();
        rspMap.put("access_token", JwtUtils.createToken(claimsMap));
        rspMap.put("expires_in", TOKEN_EXPIRE_TIME);
        rspMap.put("is_kickout", isKickout);
        return rspMap;
    }

    /**
     * 创建令牌（小程序端，不触发单点登录互踢）
     *
     * <p>注意：本方法不设置 authSource，由调用方通过 {@link #createTokenMp(LoginUser, String)} 指定，
     * 以区分"小程序密码登录"（PASSWORD）和"微信快捷登录"（WECHAT_MP）。
     * 仅当 authSource=WECHAT_MP 时才参与微信会话版本号校验。</p>
     *
     * @param loginUser 登录用户信息
     * @deprecated 使用 {@link #createTokenMp(LoginUser, String)} 显式指定登录来源
     */
    @Deprecated
    public Map<String, Object> createTokenMp(LoginUser loginUser)
    {
        return createTokenMp(loginUser, "PASSWORD");
    }

    /**
     * 创建令牌（小程序端，不触发单点登录互踢）
     *
     * @param loginUser  登录用户信息
     * @param authSource 登录来源：PASSWORD（密码登录）或 WECHAT_MP（微信快捷登录）
     */
    public Map<String, Object> createTokenMp(LoginUser loginUser, String authSource)
    {
        String token = IdUtils.fastUUID();
        Long userId = loginUser.getSysUser().getUserId();
        String userName = loginUser.getSysUser().getUserName();
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr());
        loginUser.setAuthSource(authSource);
        // 仅微信登录记录会话版本号，密码登录不参与 epoch 校验
        if ("WECHAT_MP".equals(authSource))
        {
            Long tenantId = loginUser.getSysUser().getTenantId();
            loginUser.setWechatSessionEpoch(getWechatSessionEpoch(tenantId));
        }
        else
        {
            loginUser.setWechatSessionEpoch(null);
        }

        // 小程序使用独立的 token key，不与 PC 互踢
        String userTokenKey = CacheConstants.USER_TOKEN_MP_KEY + userId;
        String oldTokenUuid = redisService.getCacheObject(userTokenKey);
        if (StringUtils.isNotEmpty(oldTokenUuid))
        {
            redisService.deleteObject(getTokenKey(oldTokenUuid));
        }
        refreshToken(loginUser);
        // 记录小程序端 userId -> token 映射
        redisService.setCacheObject(userTokenKey, token, TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);

        // Jwt存储信息
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, userId);
        claimsMap.put(SecurityConstants.DETAILS_USERNAME, userName);

        // 接口返回信息
        Map<String, Object> rspMap = new HashMap<String, Object>();
        rspMap.put("access_token", JwtUtils.createToken(claimsMap));
        rspMap.put("expires_in", TOKEN_EXPIRE_TIME);
        rspMap.put("is_kickout", false);
        return rspMap;
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser()
    {
        return getLoginUser(ServletUtils.getRequest());
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = SecurityUtils.getToken(request);
        return getLoginUser(token);
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(String token)
    {
        LoginUser user = null;
        try
        {
            if (StringUtils.isNotEmpty(token))
            {
                String userkey = JwtUtils.getUserKey(token);
                user = redisService.getCacheObject(getTokenKey(userkey));
                return user;
            }
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常'{}'", e.getMessage());
        }
        return user;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户缓存信息
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userkey = JwtUtils.getUserKey(token);
            redisService.deleteObject(getTokenKey(userkey));
        }
    }

    /**
     * 验证令牌有效期，相差不足120分钟，自动刷新缓存
     *
     * @param loginUser
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= TOKEN_REFRESH_THRESHOLD_MINUTES)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + TOKEN_EXPIRE_TIME * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisService.setCacheObject(userKey, loginUser, TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    private String getTokenKey(String token)
    {
        return ACCESS_TOKEN + token;
    }

    // =========================================================================
    // 微信会话版本号（epoch）管理
    // =========================================================================

    /**
     * 获取指定租户的微信会话版本号
     *
     * @param tenantId 租户ID
     * @return 当前版本号，未设置时返回 0
     */
    public Long getWechatSessionEpoch(Long tenantId)
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
            log.warn("getWechatSessionEpoch: invalid value, raw={}", raw);
            return 0L;
        }
    }

    /**
     * 原子递增指定租户的微信会话版本号
     *
     * <p>递增后，该租户所有已登录的微信会话在下次请求时因版本不匹配而被注销。
     * 密码登录会话不受影响。</p>
     *
     * @param tenantId 租户ID
     * @return 递增后的新版本号
     */
    public Long incrementWechatSessionEpoch(Long tenantId)
    {
        if (tenantId == null)
        {
            throw new IllegalArgumentException("tenantId 不能为空");
        }
        return redisService.increment(CacheConstants.WECHAT_SESSION_EPOCH_KEY + tenantId);
    }

    /**
     * 校验微信会话版本号是否一致
     *
     * <p>仅对 authSource=WECHAT_MP 的会话校验。PASSWORD 会话或 null 视为有效（向后兼容）。
     * 版本不匹配时删除当前 Token 并返回 false。</p>
     *
     * @param loginUser 登录用户信息
     * @return true 如果会话有效或非微信来源；false 如果版本不匹配需注销
     */
    public boolean verifyWechatSessionEpoch(LoginUser loginUser)
    {
        if (loginUser == null)
        {
            return true;
        }
        if (!"WECHAT_MP".equals(loginUser.getAuthSource()))
        {
            return true;
        }
        Long tenantId = loginUser.getSysUser() != null ? loginUser.getSysUser().getTenantId() : null;
        if (tenantId == null)
        {
            // 微信会话但无租户信息，fail-closed
            log.warn("verifyWechatSessionEpoch: tenantId is null, userId={}, sysUser is null={}, authSource={}",
                    loginUser.getUserid(), loginUser.getSysUser() == null, loginUser.getAuthSource());
            delLoginUser(loginUser.getToken());
            return false;
        }
        Long currentEpoch = getWechatSessionEpoch(tenantId);
        Long sessionEpoch = loginUser.getWechatSessionEpoch();
        // 使用 longValue() 比较而非 equals()，避免 FastJson2 反序列化时
        // 可能将 Long 字段设为 Integer 运行时类型导致 Integer.equals(Long) 永远返回 false
        if (sessionEpoch == null || sessionEpoch.longValue() != currentEpoch.longValue())
        {
            log.warn("verifyWechatSessionEpoch: epoch mismatch, tenantId={}, sessionEpoch={}, currentEpoch={}, sessionEpochClass={}",
                    tenantId, sessionEpoch, currentEpoch,
                    sessionEpoch != null ? sessionEpoch.getClass().getName() : "null");
            delLoginUser(loginUser.getToken());
            return false;
        }
        return true;
    }
}