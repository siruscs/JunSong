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
     */
    @Bean
    public ObjectMapper objectMapper()
    {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        objectMapper.findAndRegisterModules();
        objectMapper.setTimeZone(TimeZone.getDefault());
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
