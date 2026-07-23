package com.junsong.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信小程序配置属性。
 *
 * <p>从 Nacos `junsong-auth-${profile}.yml` 读取以下配置项：</p>
 * <ul>
 *   <li>{@code wechat.mp.app-id} — 小程序 AppID（可明文存 Nacos）</li>
 *   <li>{@code wechat.mp.app-secret} — 小程序 AppSecret（仅服务端读取，禁止入库）</li>
 *   <li>{@code wechat.mp.timeout-ms} — 微信接口超时毫秒，默认 5000</li>
 *   <li>{@code wechat.mp.max-retries} — 失败后的<b>额外</b>重试次数，默认 1，最大 3</li>
 * </ul>
 *
 * <p><b>max-retries 语义说明：</b>该值表示"失败后的额外重试次数"，不是"总请求次数"。
 * 例如 max-retries=2 表示最多请求 3 次（1 次初始 + 2 次重试）。</p>
 *
 * <p>安全约束：AppSecret 不会被任何日志、响应或异常信息输出。</p>
 */
@ConfigurationProperties(prefix = "wechat.mp")
public class WechatMiniProgramProperties
{
    /** max-retries 安全上限，防止配置错误导致无限重试 */
    public static final int MAX_RETRIES_LIMIT = 3;

    /** max-retries 默认值（失败后额外重试 1 次） */
    public static final int MAX_RETRIES_DEFAULT = 1;

    /** 小程序 AppID */
    private String appId = "";

    /** 小程序 AppSecret（仅服务端读取） */
    private String appSecret = "";

    /** 微信接口超时毫秒 */
    private int timeoutMs = 5000;

    /**
     * 失败后的额外重试次数。
     *
     * <p>语义：0=不重试（只请求 1 次），1=最多请求 2 次，2=最多请求 3 次。
     * 负数或超过 {@value #MAX_RETRIES_LIMIT} 时按安全默认值 {@value #MAX_RETRIES_DEFAULT} 处理。</p>
     */
    private int maxRetries = MAX_RETRIES_DEFAULT;

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppSecret()
    {
        return appSecret;
    }

    public void setAppSecret(String appSecret)
    {
        this.appSecret = appSecret;
    }

    public int getTimeoutMs()
    {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    /**
     * 获取生效的额外重试次数，已做安全校验。
     *
     * @return 0 到 {@value #MAX_RETRIES_LIMIT} 之间的值
     */
    public int getMaxRetries()
    {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries)
    {
        this.maxRetries = maxRetries;
    }

    /**
     * 获取经过安全校验的重试次数。
     *
     * <p>负数或超过上限时返回默认值 {@value #MAX_RETRIES_DEFAULT}。</p>
     *
     * @return 安全的重试次数（0 到 {@value #MAX_RETRIES_LIMIT}）
     */
    public int getSafeMaxRetries()
    {
        if (maxRetries < 0 || maxRetries > MAX_RETRIES_LIMIT)
        {
            return MAX_RETRIES_DEFAULT;
        }
        return maxRetries;
    }
}
