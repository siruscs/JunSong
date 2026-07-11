package com.junsong.open.config;

import com.junsong.open.interceptor.OpenApiContextInterceptor;
import com.junsong.open.interceptor.OpenApiLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 开放平台 WebMvc 配置
 *
 * 注册开放上下文拦截器和调用日志拦截器，仅覆盖公共 openapi 控制器路径。
 * 注册顺序：context → log，使 afterCompletion 时 log 先执行（context 尚未清除）。
 *
 * @author junsong
 */
@Configuration
public class OpenWebMvcConfig implements WebMvcConfigurer
{
    @Autowired
    private OpenApiContextInterceptor openApiContextInterceptor;

    @Autowired
    private OpenApiLogInterceptor openApiLogInterceptor;

    private static final String[] OPEN_API_PATHS = {
            "/members/**",
            "/workflow/**",
            "/store-openings/**",
            "/store-opening/**",
            "/open/apps/**",
            "/open/webhooks/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(openApiContextInterceptor)
                .addPathPatterns(OPEN_API_PATHS);
        registry.addInterceptor(openApiLogInterceptor)
                .addPathPatterns(OPEN_API_PATHS);
    }
}
