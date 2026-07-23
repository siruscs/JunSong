package com.junsong.gateway.filter;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import com.junsong.common.core.constant.SecurityConstants;
import reactor.core.publisher.Mono;

/** 敏感接口访问审计。只记录元数据，不记录 Token、密码和请求体。 */
@Component
public class SecurityAuditFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);
    private static final List<String> SENSITIVE_PREFIXES = List.of(
            "/system/user", "/system/dept", "/finance/", "/member/", "/auth/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (SENSITIVE_PREFIXES.stream().noneMatch(path::startsWith)) return chain.filter(exchange);
        long started = System.nanoTime();
        String userId = value(exchange, SecurityConstants.DETAILS_USER_ID, "anonymous");
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = exchange.getRequest().getRemoteAddress() == null ? "unknown"
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        String method = exchange.getRequest().getMethod() == null ? "UNKNOWN"
                : exchange.getRequest().getMethod().name();
        String auditIp = ip;
        return chain.filter(exchange).doFinally(signal -> log.info(
                "security_audit userId={} ip={} method={} path={} status={} durationMs={}",
                userId, auditIp, method, path,
                exchange.getResponse().getStatusCode() == null ? 0 : exchange.getResponse().getStatusCode().value(),
                Duration.ofNanos(System.nanoTime() - started).toMillis()));
    }

    private String value(ServerWebExchange exchange, String name, String fallback) {
        String value = exchange.getRequest().getHeaders().getFirst(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public int getOrder() { return -130; }
}
