package com.junsong.open.mapper;

import com.junsong.open.domain.OpenWebhookSubscription;

/**
 * Webhook订阅数据层
 *
 * @author junsong
 */
public interface OpenWebhookSubscriptionMapper
{
    int insertOpenWebhookSubscription(OpenWebhookSubscription subscription);
}
