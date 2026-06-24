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
     */
    public Map<String, Object> createTokenMp(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        Long userId = loginUser.getSysUser().getUserId();
        String userName = loginUser.getSysUser().getUserName();
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr());

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
}