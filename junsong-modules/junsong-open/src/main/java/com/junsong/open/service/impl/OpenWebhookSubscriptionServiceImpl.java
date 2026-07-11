package com.junsong.open.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.open.domain.OpenWebhookSubscription;
import com.junsong.open.mapper.OpenWebhookSubscriptionMapper;
import com.junsong.open.service.IOpenWebhookSubscriptionService;

/**
 * Webhook订阅服务实现
 *
 * @author junsong
 */
@Service
public class OpenWebhookSubscriptionServiceImpl implements IOpenWebhookSubscriptionService
{
    @Autowired
    private OpenWebhookSubscriptionMapper openWebhookSubscriptionMapper;

    @Override
    public int insertOpenWebhookSubscription(OpenWebhookSubscription subscription)
    {
        return openWebhookSubscriptionMapper.insertOpenWebhookSubscription(subscription);
    }
}
