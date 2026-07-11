package com.junsong.open.service;

import com.junsong.open.domain.OpenWebhookSubscription;

/**
 * Webhook订阅服务接口
 *
 * @author junsong
 */
public interface IOpenWebhookSubscriptionService
{
    int insertOpenWebhookSubscription(OpenWebhookSubscription subscription);
}
