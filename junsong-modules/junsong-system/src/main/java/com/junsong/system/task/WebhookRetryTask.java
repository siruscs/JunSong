package com.junsong.system.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.junsong.system.service.IWebhookDeliveryService;

/**
 * Webhook定时重试任务
 * 每2分钟扫描一次待重试的投递记录
 *
 * @author junsong
 */
@Component
@EnableScheduling
public class WebhookRetryTask
{
    private static final Logger log = LoggerFactory.getLogger(WebhookRetryTask.class);

    @Autowired
    private IWebhookDeliveryService webhookDeliveryService;

    @Scheduled(fixedDelay = 120000)
    public void retryPendingDeliveries()
    {
        try
        {
            webhookDeliveryService.processPendingRetries();
        }
        catch (Exception e)
        {
            log.error("Webhook重试任务执行异常", e);
        }
    }
}
