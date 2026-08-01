package com.junsong.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import com.junsong.common.security.auth.AuthLogic;

/**
 * 系统配置
 *
 * @author junsong
 */
public class ApplicationConfig
{
    /**
     * 时区配置
     * 统一使用 GMT+8（北京时间），避免容器 JVM 默认时区为 UTC 时
     * 导致时间序列化偏移 8 小时
     */
    @Bean
    public ObjectMapper objectMapper()
    {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        objectMapper.findAndRegisterModules();
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return objectMapper;
    }

    /**
     * 权限验证 bean，供 @PreAuthorize("@ss.hasPermi('...')") 使用
     */
    @Bean(name = "ss")
    public AuthLogic authLogic()
    {
        return new AuthLogic();
    }
}
