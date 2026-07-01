package com.junsong.system.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.junsong.system.config.RabbitMQConfig;
import com.junsong.system.domain.WebhookSubscription;
import com.junsong.system.domain.dto.WebhookDeliveryMessage;
import com.junsong.system.mapper.WebhookSubscriptionMapper;
import com.junsong.system.service.impl.WebhookDeliveryServiceImpl;

/**
 * Webhook投递消息消费者
 * 消费MQ消息，执行HTTP投递
 *
 * @author junsong
 */
@Component
public class WebhookDeliveryConsumer
{
    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryConsumer.class);

    @Autowired
    private WebhookDeliveryServiceImpl webhookDeliveryService;

    @Autowired
    private WebhookSubscriptionMapper subscriptionMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(WebhookDeliveryMessage msg, Message message, Channel channel) throws Exception
    {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try
        {
            WebhookSubscription sub = subscriptionMapper.selectWebhookSubscriptionById(msg.getSubscriptionId());
            if (sub == null || "1".equals(sub.getStatus()))
            {
                log.warn("Webhook订阅不存在或已停用，丢弃: deliveryId={}, subId={}", msg.getDeliveryId(), msg.getSubscriptionId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            webhookDeliveryService.executeDeliveryFromMQ(msg.getDeliveryId(), sub);
            channel.basicAck(deliveryTag, false);
        }
        catch (Exception e)
        {
            log.error("Webhook MQ消费异常: deliveryId={}", msg.getDeliveryId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
