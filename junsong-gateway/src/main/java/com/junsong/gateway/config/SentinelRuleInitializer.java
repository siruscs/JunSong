package com.junsong.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流规则初始化器
 * 应用启动完成后编程式加载限流规则，不依赖 Nacos 数据源（Nacos 3.x 已废弃 v1 HTTP API）
 *
 * @author junsong
 */
@Component
public class SentinelRuleInitializer implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(SentinelRuleInitializer.class);

    @Override
    public void run(ApplicationArguments args)
    {
        Set<GatewayFlowRule> rules = new HashSet<>();

        rules.add(new GatewayFlowRule("junsong-auth")
                .setCount(100)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("junsong-system")
                .setCount(200)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("junsong-finance")
                .setCount(100)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("junsong-member")
                .setCount(100)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("junsong-workflow")
                .setCount(100)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("junsong-file")
                .setCount(50)
                .setIntervalSec(1));

        try
        {
            GatewayRuleManager.loadRules(rules);
            log.info("Sentinel 网关限流规则已加载: {} 条", rules.size());
            rules.forEach(rule -> log.info("  路由[{}]: {} QPS/{}s",
                    rule.getResource(), rule.getCount(), rule.getIntervalSec()));
        }
        catch (Exception e)
        {
            log.error("Sentinel 网关限流规则加载失败", e);
        }
    }
}
