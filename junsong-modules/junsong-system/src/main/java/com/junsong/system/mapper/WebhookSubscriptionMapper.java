package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.WebhookSubscription;

/**
 * Webhook订阅Mapper
 *
 * @author junsong
 */
public interface WebhookSubscriptionMapper
{
    public WebhookSubscription selectWebhookSubscriptionById(Long id);

    public List<WebhookSubscription> selectWebhookSubscriptionList(WebhookSubscription webhookSubscription);

    public List<WebhookSubscription> selectActiveSubscriptionsByEventType(String eventType);

    public int insertWebhookSubscription(WebhookSubscription webhookSubscription);

    public int updateWebhookSubscription(WebhookSubscription webhookSubscription);

    public int deleteWebhookSubscriptionById(Long id);

    public int deleteWebhookSubscriptionByIds(Long[] ids);
}
