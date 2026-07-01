package com.junsong.gateway.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Swagger聚合配置端点
 * 在网关层提供 /v3/api-docs/swagger-config 端点
 * 返回所有微服务的OpenAPI文档聚合列表
 *
 * @author junsong
 */
@Configuration
@EnableConfigurationProperties(SwaggerUiConfigProperties.class)
public class SwaggerAggregationConfig
{
    @Bean
    @ConditionalOnMissingBean
    public SwaggerUiConfigProperties swaggerUiConfigProperties()
    {
        return new SwaggerUiConfigProperties();
    }

    @Bean
    public RouterFunction<ServerResponse> swaggerConfigRoute(SwaggerUiConfigProperties swaggerUiConfigProperties)
    {
        return RouterFunctions.route()
                .GET("/v3/api-docs/swagger-config", request ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(buildSwaggerConfig(swaggerUiConfigProperties))
                )
                .build();
    }

    private SwaggerConfigResponse buildSwaggerConfig(SwaggerUiConfigProperties swaggerUiConfigProperties)
    {
        SwaggerConfigResponse response = new SwaggerConfigResponse();
        response.setConfigUrl("/v3/api-docs/swagger-config");
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = swaggerUiConfigProperties.getUrls();
        List<SwaggerUrlDto> urlList = new ArrayList<>();
        if (urls != null)
        {
            for (AbstractSwaggerUiConfigProperties.SwaggerUrl original : urls)
            {
                SwaggerUrlDto dto = new SwaggerUrlDto();
                dto.setUrl(original.getUrl());
                String name = original.getName();
                if (name == null || name.isEmpty())
                {
                    String url = original.getUrl();
                    if (url != null && url.startsWith("/"))
                    {
                        String[] parts = url.split("/");
                        if (parts.length > 1)
                        {
                            name = parts[1];
                        }
                    }
                }
                if (name == null || name.isEmpty())
                {
                    name = original.getUrl();
                }
                dto.setName(name);
                urlList.add(dto);
            }
        }
        response.setUrls(urlList);
        return response;
    }

    public static class SwaggerConfigResponse
    {
        private String configUrl;
        private List<SwaggerUrlDto> urls;

        public String getConfigUrl()
        {
            return configUrl;
        }

        public void setConfigUrl(String configUrl)
        {
            this.configUrl = configUrl;
        }

        public List<SwaggerUrlDto> getUrls()
        {
            return urls;
        }

        public void setUrls(List<SwaggerUrlDto> urls)
        {
            this.urls = urls;
        }
    }

    public static class SwaggerUrlDto
    {
        private String url;
        private String name;

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }
}
