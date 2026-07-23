package com.junsong.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.utils.ServletUtils;
import com.junsong.common.redis.service.RedisService;
import reactor.core.publisher.Mono;

/** 普通业务接口的多维度滑动窗口保护，防止账号或出口 IP 被脚本高频拉取。 */
@Component
public class BusinessRateLimitFilter implements GlobalFilter, Ordered {
    private static final String KEY_PREFIX = "gateway:business-rate:";
    private static final long IP_LIMIT = 180L;
    private static final long USER_LIMIT = 300L;
    private static final long PATH_LIMIT = 1200L;
    private static final long WINDOW_SECONDS = 60L;

    @Autowired
    private RedisService redisService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isExcluded(path)) return chain.filter(exchange);

        String ip = clientIp(exchange);
        String userId = exchange.getRequest().getHeaders().getFirst(SecurityConstants.DETAILS_USER_ID);
        String pathKey = digest(path);
        if (exceeded(KEY_PREFIX + "ip:" + digest(ip), IP_LIMIT)
                || (userId != null && exceeded(KEY_PREFIX + "user:" + digest(userId), USER_LIMIT))
                || exceeded(KEY_PREFIX + "path:" + pathKey, PATH_LIMIT)) {
            exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(WINDOW_SECONDS));
            return ServletUtils.webFluxResponseWriter(exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS,
                    "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS.value());
        }
        return chain.filter(exchange);
    }

    private boolean exceeded(String key, long limit) {
        Long count = redisService.increment(key);
        if (count != null && count == 1L) redisService.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        return count != null && count > limit;
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/openapi/") || path.equals("/auth/login") || path.startsWith("/auth/" + "wechat")
                || path.startsWith("/code") || path.startsWith("/captcha") || path.startsWith("/actuator/");
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        if (exchange.getRequest().getRemoteAddress() == null) return "unknown";
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 12);
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    @Override
    public int getOrder() { return -140; }
}
