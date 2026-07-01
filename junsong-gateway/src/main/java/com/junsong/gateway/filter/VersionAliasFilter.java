package com.junsong.gateway.filter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * API版本管理过滤器
 *
 * 功能：
 *   1. 给所有/openapi/响应添加X-API-Version头部(标识当前版本)
 *   2. 给v1响应添加Deprecation头部(标记为即将废弃)
 *   3. 给v1响应添加Sunset头部(废弃时间)
 *   4. /openapi/latest/** 透明转发到v1，响应头标注latest
 *
 * 版本策略：
 *   v1     - 当前稳定版(标记Deprecation，建议迁移到v2)
 *   v2     - 最新稳定版
 *   latest - 别名，指向最新稳定版(v2)
 *
 * @author junsong
 */
@Component
public class VersionAliasFilter implements GlobalFilter, Ordered
{
    private static final String OPENAPI_PREFIX = "/openapi/";

    private static final String HEADER_API_VERSION = "X-API-Version";
    private static final String HEADER_DEPRECATION = "Deprecation";
    private static final String HEADER_SUNSET = "Sunset";
    private static final String HEADER_LATEST_VERSION = "X-API-Latest-Version";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!path.contains(OPENAPI_PREFIX))
        {
            return chain.filter(exchange);
        }

        String version = extractVersion(path);

        exchange.getResponse().getHeaders().add(HEADER_API_VERSION, version);
        exchange.getResponse().getHeaders().add(HEADER_LATEST_VERSION, "v2");

        if ("v1".equals(version))
        {
            exchange.getResponse().getHeaders().add(HEADER_DEPRECATION, "true");
            exchange.getResponse().getHeaders().add(HEADER_SUNSET, ZonedDateTime.now().plusMonths(6)
                    .format(DateTimeFormatter.RFC_1123_DATE_TIME));
        }

        return chain.filter(exchange);
    }

    private String extractVersion(String path)
    {
        int idx = path.indexOf(OPENAPI_PREFIX);
        if (idx < 0)
        {
            return "unknown";
        }
        String after = path.substring(idx + OPENAPI_PREFIX.length());
        int slash = after.indexOf('/');
        if (slash < 0)
        {
            return after;
        }
        String version = after.substring(0, slash);
        if ("latest".equals(version))
        {
            return "v2";
        }
        return version;
    }

    @Override
    public int getOrder()
    {
        return -140;
    }
}
