package com.junsong.system.config;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Webhook定时重试任务的线程池配置
 * （MQ投递走RabbitMQ消费者，重试扫描任务用此线程池）
 *
 * @author junsong
 */
@Configuration
public class WebhookAsyncConfig
{
    @Bean("webhookRetryExecutor")
    public Executor webhookRetryExecutor()
    {
        return new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(64),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("webhook-retry-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
