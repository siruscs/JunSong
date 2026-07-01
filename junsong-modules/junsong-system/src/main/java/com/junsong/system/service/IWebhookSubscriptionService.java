package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.WebhookSubscription;

/**
 * Webhook订阅Service接口
 *
 * @author junsong
 */
public interface IWebhookSubscriptionService
{
    public WebhookSubscription selectWebhookSubscriptionById(Long id);

    public List<WebhookSubscription> selectWebhookSubscriptionList(WebhookSubscription webhookSubscription);

    public int insertWebhookSubscription(WebhookSubscription webhookSubscription);

    public int updateWebhookSubscription(WebhookSubscription webhookSubscription);

    public int deleteWebhookSubscriptionByIds(Long[] ids);

    public int changeStatus(WebhookSubscription webhookSubscription);

    public String generateSecretToken();
}
