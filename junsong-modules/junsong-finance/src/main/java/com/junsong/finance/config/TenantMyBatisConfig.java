package com.junsong.finance.config;

import com.junsong.common.core.interceptor.TenantInterceptor;
import com.junsong.common.core.interceptor.TenantSqlInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多租户 MyBatis 拦截器配置
 * 无条件注册拦截器，拦截器内部判断是否执行
 *
 * @author junsong
 */
@Configuration
public class TenantMyBatisConfig
{
    @Bean
    public ConfigurationCustomizer tenantConfigurationCustomizer()
    {
        return configuration -> {
            configuration.addInterceptor(new TenantInterceptor());
            configuration.addInterceptor(new TenantSqlInterceptor());
        };
    }
}
