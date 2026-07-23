package com.junsong.gateway.filter;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import com.junsong.common.core.utils.ServletUtils;
import reactor.core.publisher.Mono;

/** 拦截明显的脚本采集工具特征，不依赖 Referer，避免误伤小程序请求。 */
@Component
public class AntiCrawlerFilter implements GlobalFilter, Ordered {
    private static final Pattern TOOL_USER_AGENT = Pattern.compile(
            "(?:python-requests|python-httpx|scrapy|octoparse|八爪鱼|httrack|wget|curl/|libwww-perl|go-http-client|java/)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/openapi/") || path.startsWith("/actuator/") || path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        if (userAgent != null && TOOL_USER_AGENT.matcher(userAgent.toLowerCase(Locale.ROOT)).find()) {
            return ServletUtils.webFluxResponseWriter(exchange.getResponse(), HttpStatus.FORBIDDEN,
                    "当前客户端不受支持", HttpStatus.FORBIDDEN.value());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return -155; }
}
