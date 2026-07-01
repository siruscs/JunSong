package com.junsong.system.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * Webhook 投递消息队列 + 死信队列
 *
 * 架构：
 *   webhook.delivery.exchange (DirectExchange)
 *     ├─ webhook.delivery.queue        → 消费者执行 HTTP 投递
 *     └─ webhook.delivery.dlq.queue     → 死信队列（重试耗尽后归档）
 *
 * @author junsong
 */
@Configuration
public class RabbitMQConfig
{
    public static final String EXCHANGE = "webhook.delivery.exchange";
    public static final String QUEUE = "webhook.delivery.queue";
    public static final String ROUTING_KEY = "webhook.delivery";
    public static final String DLX_EXCHANGE = "webhook.delivery.dlx.exchange";
    public static final String DLQ_QUEUE = "webhook.delivery.dlq.queue";
    public static final String DLQ_ROUTING_KEY = "webhook.delivery.dlq";

    @Bean
    public MessageConverter jsonMessageConverter()
    {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange webhookDeliveryExchange()
    {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange webhookDeliveryDlxExchange()
    {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue webhookDeliveryQueue()
    {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue webhookDeliveryDlqQueue()
    {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding webhookDeliveryBinding()
    {
        return BindingBuilder.bind(webhookDeliveryQueue())
                .to(webhookDeliveryExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding webhookDeliveryDlqBinding()
    {
        return BindingBuilder.bind(webhookDeliveryDlqQueue())
                .to(webhookDeliveryDlxExchange())
                .with(DLQ_ROUTING_KEY);
    }
}
