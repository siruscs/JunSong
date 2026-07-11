package com.junsong.open.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import com.junsong.open.context.OpenApiRequestContext;
import com.junsong.open.context.OpenApiRequestContextHolder;

/**
 * 开放平台配置类
 *
 * @author junsong
 */
@Configuration
public class OpenApiConfig
{
    /**
     * RestTemplate(透传网关注入的 X-Open-* 开放上下文)
     */
    @Bean
    public RestTemplate restTemplate()
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(internalCallInterceptor());
        return restTemplate;
    }

    /**
     * 内部调用拦截器
     * 透传来自可信网关的开放上下文，不再用固定 admin 身份冒用。
     */
    ClientHttpRequestInterceptor internalCallInterceptor()
    {
        return (request, body, execution) -> {
            request.getHeaders().add("from-source", "open-api");
            OpenApiRequestContext context = OpenApiRequestContextHolder.get();
            if (context != null)
            {
                request.getHeaders().add("X-Open-App-Id", String.valueOf(context.getAppId()));
                request.getHeaders().add("X-Open-App-Key", context.getAppKey());
                request.getHeaders().add("X-Open-Tenant-Id", String.valueOf(context.getTenantId()));
                request.getHeaders().add("X-Open-Request-Id", context.getRequestId());
            }
            return execution.execute(request, body);
        };
    }
}
