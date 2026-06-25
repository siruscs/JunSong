package com.junsong.workflow.config;

import javax.sql.DataSource;
import org.flowable.engine.impl.cfg.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Flowable 事务一致性配置。
 *
 * 显式绑定 Flowable ProcessEngine 使用 Spring 的 PlatformTransactionManager，
 * 确保 Flowable 操作（流程启动/删除/完成任务）与业务 DB 操作（lc_biz_instance 更新）
 * 在同一个 Spring 事务边界内，避免流程状态与业务单据状态不一致。
 *
 * <p>在 flowable-spring-boot-starter 自动配置下，默认已共享 TransactionManager。
 * 本配置为显式声明，增强可读性和可维护性，同时提供自定义扩展点。</p>
 *
 * @author Genesis·峻松
 */
@Configuration
public class FlowableTransactionConfig
{
    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 显式配置 SpringProcessEngineConfiguration，确保：
     * 1. 使用 Spring 的 DataSource（与业务 DB 一致）
     * 2. 使用 Spring 的 PlatformTransactionManager（事务边界统一）
     * 3. 自定义流程定义加载路径（可选）
     */
    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration() throws Exception
    {
        SpringProcessEngineConfiguration cfg = new SpringProcessEngineConfiguration();
        cfg.setDataSource(dataSource);
        cfg.setTransactionManager(transactionManager);
        cfg.setDatabaseSchemaUpdate("true");

        // 流程定义自动部署
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/processes/**.bpmn20.xml");
        cfg.setDeploymentResources(resources);

        // 异步执行器配置：与 Spring 事务兼容
        cfg.setAsyncExecutorActivate(true);

        // 历史级别：full（保留全部历史，便于审计）
        cfg.setHistoryLevel("full");

        return cfg;
    }
}
