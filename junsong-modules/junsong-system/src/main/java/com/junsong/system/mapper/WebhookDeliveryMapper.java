package com.junsong.system.mapper;

import java.util.Date;
import java.util.List;
import com.junsong.system.domain.WebhookDelivery;

/**
 * Webhook投递记录Mapper
 *
 * @author junsong
 */
public interface WebhookDeliveryMapper
{
    public WebhookDelivery selectWebhookDeliveryById(Long id);

    public List<WebhookDelivery> selectWebhookDeliveryList(WebhookDelivery webhookDelivery);

    public List<WebhookDelivery> selectPendingRetries(Date beforeTime);

    public int insertWebhookDelivery(WebhookDelivery webhookDelivery);

    public int updateWebhookDelivery(WebhookDelivery webhookDelivery);
}
