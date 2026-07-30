package com.junsong.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import com.junsong.common.security.config.ApplicationConfig;
import com.junsong.common.security.feign.FeignAutoConfiguration;
import com.junsong.common.core.config.TenantConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
// 表示通过aop框架暴露该代理对象,AopContext能够访问
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径
// 排除幂等 Mapper（由 IdempotencyAutoConfiguration 按需扫描，避免无数据源服务启动失败）
@MapperScan(value = "com.junsong.**.mapper",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.junsong\\.common\\.core\\.idempotency\\.mapper\\..*"))
// 开启线程异步执行
@EnableAsync
// 自动加载类
@Import({ ApplicationConfig.class, FeignAutoConfiguration.class, TenantConfig.class })
public @interface EnableCustomConfig
{

}
