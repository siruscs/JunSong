package com.junsong.common.core.idempotency;

import org.apache.ibatis.session.SqlSessionFactory;
import com.junsong.common.core.idempotency.mapper.IdempotencyRecordMapper;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 幂等组件自动配置。
 *
 * 注册 AOP 切面和幂等记录服务。
 * 业务模块依赖 junsong-common-core 即自动启用。
 *
 * <p>条件加载策略：
 * <ul>
 *   <li>使用 @ConditionalOnProperty（PARSE_PHASE）而非 @ConditionalOnBean（REGISTER_BEAN_PHASE），
 *       因为 Spring 7.0 不允许 @Import/@Bean 与 REGISTER_BEAN 阶段条件共存于同一配置类。</li>
 *   <li>默认启用（matchIfMissing=true），无数据源服务（gateway/auth）通过
 *       junsong.idempotency.enabled=false 显式禁用。</li>
 *   <li>@Bean 方法通过参数注入 SqlSessionFactory 天然守卫：无数据源时 Bean 不会被创建。</li>
 * </ul></p>
 *
 * <p>组件注册策略：
 * 业务服务的 @SpringBootApplication 默认只扫描自身包（如 com.junsong.finance），
 * 不会扫描 com.junsong.common.core.idempotency 包下的 @Component/@Service/@Aspect。
 * 此处通过 @Import 显式导入幂等组件（IdempotencyAspect / IdempotencyRecordServiceImpl /
 * HeaderIdempotencyKeyResolver）。</p>
 *
 * @author junsong
 */
@Configuration
@ConditionalOnProperty(name = "junsong.idempotency.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@Import({IdempotencyAspect.class, IdempotencyRecordServiceImpl.class, HeaderIdempotencyKeyResolver.class})
public class IdempotencyAutoConfiguration {

    /**
     * 注册幂等记录 Mapper。
     *
     * <p>使用 MapperFactoryBean 注册 Mapper 接口，而非 @MapperScan。
     * MapperFactoryBean 的 getObjectType() 返回 IdempotencyRecordMapper.class，
     * 确保 @ConditionalOnBean(IdempotencyRecordMapper.class) 能正确匹配。
     * Mapper XML（mapper/system/IdempotencyRecordMapper.xml）由 MyBatis 的
     * mapper-locations=classpath*:mapper/&#42;&#42;/&#42;.xml 配置自动加载。</p>
     */
    @Bean
    public MapperFactoryBean<IdempotencyRecordMapper> idempotencyRecordMapper(SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<IdempotencyRecordMapper> factoryBean = new MapperFactoryBean<>(IdempotencyRecordMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }
}
