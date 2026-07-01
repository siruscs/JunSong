package com.junsong.system.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.system.domain.WebhookSubscription;
import com.junsong.system.mapper.WebhookSubscriptionMapper;
import com.junsong.system.service.IWebhookSubscriptionService;

/**
 * Webhook订阅Service实现
 *
 * @author junsong
 */
@Service
public class WebhookSubscriptionServiceImpl implements IWebhookSubscriptionService
{
    @Autowired
    private WebhookSubscriptionMapper webhookSubscriptionMapper;

    @Override
    public WebhookSubscription selectWebhookSubscriptionById(Long id)
    {
        return webhookSubscriptionMapper.selectWebhookSubscriptionById(id);
    }

    @Override
    public List<WebhookSubscription> selectWebhookSubscriptionList(WebhookSubscription webhookSubscription)
    {
        return webhookSubscriptionMapper.selectWebhookSubscriptionList(webhookSubscription);
    }

    @Override
    public int insertWebhookSubscription(WebhookSubscription webhookSubscription)
    {
        WebhookUrlValidator.validate(webhookSubscription.getCallbackUrl());
        if (StringUtils.isEmpty(webhookSubscription.getSecretToken()))
        {
            webhookSubscription.setSecretToken(generateSecretToken());
        }
        if (webhookSubscription.getMaxRetries() == null)
        {
            webhookSubscription.setMaxRetries(3);
        }
        return webhookSubscriptionMapper.insertWebhookSubscription(webhookSubscription);
    }

    @Override
    public int updateWebhookSubscription(WebhookSubscription webhookSubscription)
    {
        WebhookUrlValidator.validate(webhookSubscription.getCallbackUrl());
        return webhookSubscriptionMapper.updateWebhookSubscription(webhookSubscription);
    }

    @Override
    public int deleteWebhookSubscriptionByIds(Long[] ids)
    {
        return webhookSubscriptionMapper.deleteWebhookSubscriptionByIds(ids);
    }

    @Override
    public int changeStatus(WebhookSubscription webhookSubscription)
    {
        return webhookSubscriptionMapper.updateWebhookSubscription(webhookSubscription);
    }

    @Override
    public String generateSecretToken()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
