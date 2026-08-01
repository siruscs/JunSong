package com.junsong.common.security.config;

import java.util.TimeZone;
import jakarta.annotation.PostConstruct;
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
     * 全局时区设置
     * 统一使用 GMT+8（北京时间），避免容器 JVM 默认时区为 UTC 时
     * 导致时间序列化/解析偏移 8 小时。
     *
     * 设置 JVM 默认时区后，以下全部自动生效：
     * - Jackson @JsonFormat 不指定 timezone 时使用 JVM 默认时区
     * - SimpleDateFormat 解析/格式化使用 JVM 默认时区
     * - new Date() 使用 JVM 默认时区
     *
     * 这是全局方案，无需逐个修改各实体的 @JsonFormat 注解。
     */
    @PostConstruct
    public void setDefaultTimeZone()
    {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
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
