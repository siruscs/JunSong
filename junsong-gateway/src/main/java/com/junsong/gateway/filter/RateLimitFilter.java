package com.junsong.gateway.filter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.junsong.common.core.utils.ServletUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.gateway.constant.OpenApiErrorCodes;
import reactor.core.publisher.Mono;

/**
 * 开放平台应用级限流过滤器
 *
 * 基于Redis计数器实现每日配额限流
 * 限流维度：每个AppKey每天调用次数(由daily_quota决定)
 *
 * Redis Key: openapi:quota:{appKey}:{yyyy-MM-dd}
 * 过期时间：25小时(保证跨天后自动清理)
 *
 * @author junsong
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String OPENAPI_PREFIX = "/openapi/";

    private static final String HEADER_APP_KEY = "X-App-Key";

    @Autowired
    private RedisService redisService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.contains(OPENAPI_PREFIX))
        {
            return chain.filter(exchange);
        }

        String appKey = exchange.getRequest().getHeaders().getFirst(HEADER_APP_KEY);
        if (appKey == null || appKey.isEmpty())
        {
            return chain.filter(exchange);
        }

        String quotaStr = redisService.getCacheObject("openapi:quota_config:" + appKey);
        if (quotaStr == null || quotaStr.isEmpty())
        {
            return chain.filter(exchange);
        }

        int dailyQuota;
        try
        {
            dailyQuota = Integer.parseInt(quotaStr);
        }
        catch (NumberFormatException e)
        {
            return chain.filter(exchange);
        }

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String countKey = "openapi:quota:" + appKey + ":" + today;

        Long currentCount = redisService.increment(countKey);
        if (currentCount != null && currentCount == 1)
        {
            redisService.expire(countKey, 25L, TimeUnit.HOURS);
        }

        if (currentCount != null && currentCount > dailyQuota)
        {
            log.warn("[RateLimit] AppKey={} 超出每日配额 {}/{} (今日已用{})",
                    appKey, currentCount, dailyQuota, currentCount - 1);
            return tooManyRequestsResponse(exchange, dailyQuota, currentCount - 1);
        }

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(dailyQuota));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, dailyQuota - currentCount)));

        return chain.filter(exchange);
    }

    private Mono<Void> tooManyRequestsResponse(ServerWebExchange exchange, int limit, long used)
    {
        log.warn("[RateLimit]请求路径:{}, AppKey超出配额(limit={})", exchange.getRequest().getPath(), limit);
        String responseBody = OpenApiErrorCodes.RATE_LIMIT_EXCEEDED
                + ": 请求超出每日配额限制(" + limit + "次/天)，今日已用" + used + "次";
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(),
                HttpStatus.TOO_MANY_REQUESTS, responseBody, HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Override
    public int getOrder()
    {
        return -145;
    }
}
