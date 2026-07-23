package com.junsong.auth.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsong.auth.config.WechatMiniProgramProperties;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;

/**
 * 微信小程序 code 换身份服务。
 *
 * <p>调用微信 `jscode2session` 接口，用临时 code 换取 openid / unionid。
 * AppSecret 仅在服务端使用，不会出现在任何日志、响应或异常信息中。</p>
 *
 * <p>安全约束：</p>
 * <ul>
 *   <li>AppSecret 仅从 {@link WechatMiniProgramProperties} 读取，不入库、不日志、不响应。</li>
 *   <li>日志只记录 requestId、错误分类、耗时；不记录 code、openid、unionid、AppSecret。</li>
 *   <li>错误信息不向客户端泄露微信原始错误、openid 是否存在等敏感细节。</li>
 *   <li>禁止小程序端直接调用微信换身份接口，必须由本服务代理。</li>
 * </ul>
 */
@Component
@EnableConfigurationProperties(WechatMiniProgramProperties.class)
public class WechatMiniProgramService
{
    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramService.class);

    /** 微信 jscode2session 接口地址 */
    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    private final WechatMiniProgramProperties properties;

    private final RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造方法。
     *
     * <p>直接创建 RestClient 实例，避免依赖 RestClient.Builder 自动配置
     * （spring-boot-starter-webmvc 不一定注册该 bean）。超时通过 JVM 默认配置或
     * Nacos {@code spring.http.client.connect-timeout/read-timeout} 生效。</p>
     *
     * @param properties 微信配置
     */
    @Autowired
    public WechatMiniProgramService(WechatMiniProgramProperties properties)
    {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /**
     * 测试专用构造方法，允许注入自定义 RestClient（配合 MockRestServiceServer）。
     *
     * @param properties   微信配置
     * @param restClient   自定义 RestClient 实例
     */
    WechatMiniProgramService(WechatMiniProgramProperties properties, RestClient restClient)
    {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * 用微信临时 code 换取 openid / unionid。
     *
     * <p>支持有限重试，按 {@code wechat.mp.max-retries} 配置（语义为"失败后的额外重试次数"）。
     * 使用指数退避延迟（200ms / 500ms / 1000ms），总尝试次数受上限约束。</p>
     *
     * <p><b>可重试的情况</b>（临时性故障）：</p>
     * <ul>
     *   <li>网络连接异常、读取超时、DNS 失败（无 HTTP 响应，statusCode=null）</li>
     *   <li>HTTP 408 / 429 / 500 / 502 / 503 / 504</li>
     *   <li>微信错误码 -1（系统繁忙）</li>
     *   <li>微信错误码 45011（请求频繁，短暂等待后可恢复）</li>
     * </ul>
     *
     * <p><b>不重试的情况</b>（明确错误，重试无用或会消耗临时 code）：</p>
     * <ul>
     *   <li>HTTP 400 / 401 / 403 / 404 等非临时性错误</li>
     *   <li>微信错误码 40029（code 无效或已过期）</li>
     *   <li>微信错误码 40226（高风险账号）</li>
     *   <li>AppID/AppSecret 未配置、code 为空</li>
     *   <li>空响应、响应解析失败（响应格式错误）</li>
     *   <li>openid 为空（微信返回成功但无身份标识）</li>
     * </ul>
     *
     * @param code 微信 wx.login() 返回的临时 code
     * @return 微信身份（openid 必填，unionid 可空）
     * @throws ServiceException 当配置缺失、code 过期、微信错误、超时、解析失败时抛出
     */
    public WechatIdentity exchangeCodeForIdentity(String code)
    {
        // 1. 前置校验：配置必须完备（不重试）
        if (StringUtils.isEmpty(properties.getAppId()))
        {
            throw new ServiceException("微信小程序未配置 AppID，请联系管理员");
        }
        if (StringUtils.isEmpty(properties.getAppSecret()))
        {
            throw new ServiceException("微信小程序未配置密钥，请联系管理员");
        }
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("微信登录凭证缺失，请重新发起登录");
        }

        // 安全获取重试次数：负数或超过上限(3)时使用默认值(1)
        int maxRetries = properties.getSafeMaxRetries();
        ServiceException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++)
        {
            // 重试前进行指数退避等待（首次不等待）
            if (attempt > 0)
            {
                sleepBackoff(attempt);
            }

            String requestId = UUID.randomUUID().toString().replace("-", "");
            long startMs = System.currentTimeMillis();

            try
            {
                String responseBody = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("api.weixin.qq.com")
                                .path("/sns/jscode2session")
                                .queryParam("appid", properties.getAppId())
                                .queryParam("secret", properties.getAppSecret())
                                .queryParam("js_code", code)
                                .queryParam("grant_type", "authorization_code")
                                .build())
                        .retrieve()
                        .body(String.class);

                long elapsedMs = System.currentTimeMillis() - startMs;

                if (responseBody == null || responseBody.isEmpty())
                {
                    log.warn("wechat jscode2session empty response, requestId={}, attempt={}/{}, elapsedMs={}",
                            requestId, attempt, maxRetries, elapsedMs);
                    // 响应格式错误不重试，直接失败
                    throw new ServiceException("微信登录服务返回空响应，请稍后重试");
                }

                JsCode2SessionResponse resp;
                try
                {
                    resp = objectMapper.readValue(responseBody, JsCode2SessionResponse.class);
                }
                catch (Exception e)
                {
                    log.warn("wechat jscode2session parse failure, requestId={}, attempt={}/{}, elapsedMs={}",
                            requestId, attempt, maxRetries, elapsedMs);
                    // 响应格式错误不重试，直接失败
                    throw new ServiceException("微信登录服务响应解析失败，请稍后重试");
                }

                // 微信错误码校验
                if (resp.errcode != null && resp.errcode != 0)
                {
                    log.warn("wechat jscode2session error, requestId={}, errcode={}, attempt={}/{}, elapsedMs={}",
                            requestId, resp.errcode, attempt, maxRetries, elapsedMs);
                    if (isRetryableErrcode(resp.errcode))
                    {
                        lastException = new ServiceException(mapErrorMessage(resp.errcode));
                        continue; // 临时性故障，可重试
                    }
                    // 不可重试的错误码直接抛出（40029/40226 等）
                    throw new ServiceException(mapErrorMessage(resp.errcode));
                }

                // openid 必须存在（微信返回成功但无 openid，重试无用）
                if (StringUtils.isEmpty(resp.openid))
                {
                    log.warn("wechat jscode2session empty openid, requestId={}, attempt={}/{}, elapsedMs={}",
                            requestId, attempt, maxRetries, elapsedMs);
                    throw new ServiceException("微信登录服务返回的身份标识无效，请稍后重试");
                }

                log.info("wechat jscode2session success, requestId={}, attempt={}/{}, elapsedMs={}",
                        requestId, attempt, maxRetries, elapsedMs);

                return new WechatIdentity(resp.openid, resp.unionid);
            }
            catch (ServiceException se)
            {
                throw se;
            }
            catch (Exception e)
            {
                long elapsedMs = System.currentTimeMillis() - startMs;
                Integer statusCode = extractHttpStatusCode(e);
                log.warn("wechat jscode2session http error, requestId={}, attempt={}/{}, elapsedMs={}, errorClass={}, statusCode={}",
                        requestId, attempt, maxRetries, elapsedMs, e.getClass().getSimpleName(), statusCode);
                // statusCode == null：网络连接异常、读取超时、DNS 失败等（无 HTTP 响应），可重试
                // statusCode != null：仅 408/429/500/502/503/504 可重试，其他 HTTP 状态码直接失败
                if (statusCode == null || isRetryableHttpStatus(statusCode))
                {
                    lastException = new ServiceException("微信登录服务暂时不可用，请稍后重试");
                    // 可重试的临时性故障（网络异常或可重试 HTTP 状态码）
                }
                else
                {
                    // 不可重试的 HTTP 错误（400/401/403/404 等），直接失败
                    throw new ServiceException("微信登录服务暂时不可用，请稍后重试");
                }
            }
        }

        // 所有重试用尽，返回统一脱敏提示
        throw new ServiceException("微信登录服务暂时不可用，请稍后重试");
    }

    /**
     * 指数退避延迟表（单位毫秒）。
     *
     * <p>第 1 次重试等待 200ms，第 2 次等待 500ms，第 3 次等待 1000ms。
     * 加入少量随机抖动（0-50ms），避免并发请求同时重试。</p>
     *
     * @param attempt 当前是第几次重试（1-based）
     */
    private void sleepBackoff(int attempt)
    {
        long[] delays = {200L, 500L, 1000L};
        int idx = Math.min(attempt - 1, delays.length - 1);
        long baseDelay = delays[idx];
        long jitter = (long) (Math.random() * 50);
        try
        {
            Thread.sleep(baseDelay + jitter);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 判断微信错误码是否可重试。
     *
     * <p>可重试的临时性故障：</p>
     * <ul>
     *   <li>-1：系统繁忙</li>
     *   <li>45011：请求频繁（短暂等待后可恢复）</li>
     * </ul>
     *
     * <p>不重试的明确错误：40029（code 无效）、40226（高风险账号）等。</p>
     *
     * @param errcode 微信错误码
     * @return true 表示可重试
     */
    private boolean isRetryableErrcode(int errcode)
    {
        return errcode == -1 || errcode == 45011;
    }

    /**
     * 判断 HTTP 状态码是否可重试。
     *
     * <p>仅以下状态码重试（临时性故障）：</p>
     * <ul>
     *   <li>408 Request Timeout</li>
     *   <li>429 Too Many Requests</li>
     *   <li>500 Internal Server Error</li>
     *   <li>502 Bad Gateway</li>
     *   <li>503 Service Unavailable</li>
     *   <li>504 Gateway Timeout</li>
     * </ul>
     *
     * <p>其他状态码（400/401/403/404 等）不重试，避免重复消耗微信临时 code。</p>
     *
     * @param statusCode HTTP 状态码
     * @return true 表示可重试
     */
    private boolean isRetryableHttpStatus(int statusCode)
    {
        return statusCode == 408 || statusCode == 429
                || statusCode == 500 || statusCode == 502
                || statusCode == 503 || statusCode == 504;
    }

    /**
     * 从异常中提取 HTTP 状态码。
     *
     * <p>Spring RestClient 在 4xx/5xx 时抛出 {@link HttpStatusCodeException}，
     * 其中包含状态码。其他异常（如连接超时）返回 null。</p>
     *
     * @param e 异常
     * @return HTTP 状态码，无法提取时返回 null
     */
    private Integer extractHttpStatusCode(Exception e)
    {
        if (e instanceof org.springframework.web.client.HttpStatusCodeException httpEx)
        {
            return httpEx.getStatusCode().value();
        }
        return null;
    }

    /**
     * 微信身份记录。
     *
     * @param openid  微信 openid（必填）
     * @param unionid 微信 unionid（可空）
     */
    public record WechatIdentity(String openid, String unionid) {}

    /**
     * 微信 jscode2session 响应体。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JsCode2SessionResponse
    {
        public String openid;
        public String session_key;
        public String unionid;
        public Integer errcode;
        public String errmsg;
    }

    /**
     * 将微信错误码映射为内部错误分类（用于日志，不暴露给客户端）。
     */
    private String mapErrorCode(int errcode)
    {
        return switch (errcode)
        {
            case 40029 -> "INVALID_CODE";
            case 45011 -> "FREQUENCY_LIMITED";
            case 40226 -> "HIGH_RISK_USER";
            case -1 -> "SYSTEM_BUSY";
            default -> "UNKNOWN_ERROR";
        };
    }

    /**
     * 将微信错误码映射为用户可见的错误信息（脱敏，不泄露微信内部错误）。
     */
    private String mapErrorMessage(int errcode)
    {
        return switch (errcode)
        {
            case 40029 -> "微信登录凭证已失效，请重新发起登录";
            case 45011 -> "微信登录请求过于频繁，请稍后重试";
            case 40226 -> "微信账号存在安全风险，请联系微信客服";
            case -1 -> "微信服务暂时繁忙，请稍后重试";
            default -> "微信登录服务暂时不可用，请稍后重试";
        };
    }
}
