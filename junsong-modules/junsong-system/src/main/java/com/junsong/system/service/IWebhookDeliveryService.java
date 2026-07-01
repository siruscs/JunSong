package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.WebhookDelivery;

/**
 * Webhook投递记录Service接口
 *
 * @author junsong
 */
public interface IWebhookDeliveryService
{
    public List<WebhookDelivery> selectWebhookDeliveryList(WebhookDelivery webhookDelivery);

    public void publishEvent(String eventType, String eventId, Object payload);

    public void processPendingRetries();
}
