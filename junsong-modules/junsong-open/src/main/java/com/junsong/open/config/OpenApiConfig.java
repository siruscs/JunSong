package com.junsong.open.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * 开放平台配置类
 *
 * @author junsong
 */
@Configuration
public class OpenApiConfig
{
    /**
     * RestTemplate(注入内部调用身份header)
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
     * 注入用户身份header，让下游服务识别为管理员调用
     */
    private ClientHttpRequestInterceptor internalCallInterceptor()
    {
        return (request, body, execution) -> {
            request.getHeaders().add("from-source", "open-api");
            request.getHeaders().add("user_id", "1");
            request.getHeaders().add("username", "admin");
            request.getHeaders().add("user_key", "openapi-internal");
            return execution.execute(request, body);
        };
    }
}
