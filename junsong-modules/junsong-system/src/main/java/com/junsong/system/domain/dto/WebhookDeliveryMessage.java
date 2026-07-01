package com.junsong.system.domain.dto;

import java.io.Serializable;

/**
 * Webhook投递消息体
 * 通过RabbitMQ传递给消费者执行HTTP投递
 *
 * @author junsong
 */
public class WebhookDeliveryMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long deliveryId;

    private Long subscriptionId;

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public Long getSubscriptionId()
    {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId)
    {
        this.subscriptionId = subscriptionId;
    }
}
