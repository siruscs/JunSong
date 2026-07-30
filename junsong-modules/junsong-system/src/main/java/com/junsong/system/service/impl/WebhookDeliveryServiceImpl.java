package com.junsong.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.system.config.RabbitMQConfig;
import com.junsong.system.domain.WebhookDelivery;
import com.junsong.system.domain.WebhookSubscription;
import com.junsong.system.domain.dto.WebhookDeliveryMessage;
import com.junsong.system.mapper.WebhookDeliveryMapper;
import com.junsong.system.mapper.WebhookSubscriptionMapper;
import com.junsong.system.service.IWebhookDeliveryService;

/**
 * Webhook投递Service实现
 * MQ生产者 + HMAC-SHA256签名 + 指数退避重试
 *
 * 流程：业务事件 → 落库投递记录 → 发送MQ消息 → 消费者执行HTTP投递 → 成功/失败/重试
 *
 * @author junsong
 */
@Service
public class WebhookDeliveryServiceImpl implements IWebhookDeliveryService
{
    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryServiceImpl.class);

    @Autowired
    private WebhookSubscriptionMapper subscriptionMapper;

    @Autowired
    private WebhookDeliveryMapper deliveryMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** Webhook HTTP投递超时配置：连接10秒，读取10秒，禁止跟随重定向 */
    private final RestTemplate restTemplate;
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory()
        {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod)
                    throws java.io.IOException
            {
                super.prepareConnection(connection, httpMethod);
                // 禁止跟随重定向到内网地址（SSRF 防御）
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<WebhookDelivery> selectWebhookDeliveryList(WebhookDelivery webhookDelivery)
    {
        return deliveryMapper.selectWebhookDeliveryList(webhookDelivery);
    }

    @Override
    public void publishEvent(String eventType, String eventId, Object payload)
    {
        if (StringUtils.isEmpty(eventId))
        {
            eventId = UUID.randomUUID().toString().replace("-", "");
        }
        String payloadJson = payload instanceof String ? (String) payload : JSON.toJSONString(payload);

        List<WebhookSubscription> subscriptions = subscriptionMapper.selectActiveSubscriptionsByEventType(eventType);
        if (subscriptions == null || subscriptions.isEmpty())
        {
            log.debug("Webhook事件无订阅者: eventType={}", eventType);
            return;
        }

        for (WebhookSubscription sub : subscriptions)
        {
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setTenantId(sub.getTenantId());
            delivery.setSubscriptionId(sub.getId());
            delivery.setEventType(eventType);
            delivery.setEventId(eventId);
            delivery.setPayload(payloadJson);
            delivery.setRetryCount(0);
            delivery.setStatus("PENDING");
            delivery.setCreateTime(new Date());
            deliveryMapper.insertWebhookDelivery(delivery);

            WebhookDeliveryMessage msg = new WebhookDeliveryMessage();
            msg.setDeliveryId(delivery.getId());
            msg.setSubscriptionId(sub.getId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, msg);
        }
        log.info("Webhook事件已触发: eventType={}, eventId={}, 订阅者={}", eventType, eventId, subscriptions.size());
    }

    /**
     * 消费者调用的HTTP投递方法
     *
     * 幂等性保证：MQ 可能重复投递同一消息（at-least-once 语义）。
     * 本方法在执行前检查 delivery 状态，若已处于终态（SUCCESS/FAILED）则直接跳过，
     * 避免对订阅方造成重复通知。
     */
    public void executeDeliveryFromMQ(Long deliveryId, WebhookSubscription sub)
    {
        WebhookDelivery delivery = deliveryMapper.selectWebhookDeliveryById(deliveryId);
        if (delivery == null)
        {
            return;
        }
        // 幂等检查：终态记录跳过（防止 MQ 重复消费导致重复投递）
        String status = delivery.getStatus();
        if ("SUCCESS".equals(status) || "FAILED".equals(status))
        {
            log.info("Webhook投递跳过（终态记录，MQ重复消费幂等保护）: deliveryId={}, status={}",
                    deliveryId, status);
            return;
        }
        executeDelivery(delivery, sub);
    }

    private void executeDelivery(WebhookDelivery delivery, WebhookSubscription sub)
    {
        String payload = delivery.getPayload();
        try
        {
            // 投递前再次校验URL安全性，防止订阅保存后规则变更或DNS重绑定
            WebhookUrlValidator.validate(sub.getCallbackUrl());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Webhook-Event", sub.getEventType());
            headers.add("X-Webhook-EventId", delivery.getEventId());

            if (StringUtils.isNotEmpty(sub.getSecretToken()))
            {
                String signature = hmacSha256(payload, sub.getSecretToken());
                headers.add("X-Webhook-Signature", signature);
            }

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    sub.getCallbackUrl(), HttpMethod.POST, request, String.class);

            int statusCode = response.getStatusCode().value();
            String body = response.getBody();
            if (body != null && body.length() > 2000)
            {
                body = body.substring(0, 2000);
            }

            if (statusCode >= 200 && statusCode < 300)
            {
                delivery.setHttpStatus(statusCode);
                delivery.setResponseBody(body);
                delivery.setStatus("SUCCESS");
                delivery.setCompleteTime(new Date());
                deliveryMapper.updateWebhookDelivery(delivery);
                log.info("Webhook投递成功: deliveryId={}, url={}, status={}", delivery.getId(), sub.getCallbackUrl(), statusCode);
            }
            else
            {
                handleFailure(delivery, sub, statusCode, body, "HTTP " + statusCode);
            }
        }
        catch (Exception e)
        {
            handleFailure(delivery, sub, null, null, e.getMessage());
        }
    }

    private void handleFailure(WebhookDelivery delivery, WebhookSubscription sub, Integer httpStatus, String responseBody, String errorMsg)
    {
        delivery.setHttpStatus(httpStatus);
        delivery.setResponseBody(responseBody != null ? responseBody.substring(0, Math.min(2000, responseBody.length())) : errorMsg);
        delivery.setRetryCount(delivery.getRetryCount() + 1);

        int maxRetries = sub.getMaxRetries() != null ? sub.getMaxRetries() : 3;
        if (delivery.getRetryCount() >= maxRetries)
        {
            delivery.setStatus("DEAD");
            delivery.setCompleteTime(new Date());
            log.error("Webhook投递失败(死信): deliveryId={}, retries={}", delivery.getId(), delivery.getRetryCount());
        }
        else
        {
            delivery.setStatus("FAILED");
            long delayMinutes = (long) Math.pow(2, delivery.getRetryCount());
            delivery.setNextRetryTime(new Date(System.currentTimeMillis() + delayMinutes * 60 * 1000));
            log.warn("Webhook投递失败(待重试): deliveryId={}, retry={}/{}, nextRetry={}min", delivery.getId(), delivery.getRetryCount(), maxRetries, delayMinutes);
        }
        deliveryMapper.updateWebhookDelivery(delivery);
    }

    @Override
    public void processPendingRetries()
    {
        List<WebhookDelivery> pending = deliveryMapper.selectPendingRetries(new Date());
        if (pending == null || pending.isEmpty())
        {
            return;
        }
        log.info("Webhook重试扫描: {}条待重试", pending.size());
        for (WebhookDelivery delivery : pending)
        {
            WebhookSubscription sub = subscriptionMapper.selectWebhookSubscriptionById(delivery.getSubscriptionId());
            if (sub == null || "1".equals(sub.getStatus()))
            {
                delivery.setStatus("DEAD");
                delivery.setCompleteTime(new Date());
                deliveryMapper.updateWebhookDelivery(delivery);
                continue;
            }
            delivery.setStatus("PENDING");
            deliveryMapper.updateWebhookDelivery(delivery);
            executeDelivery(delivery, sub);
        }
    }

    private String hmacSha256(String data, String secret)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "sha256=" + hexString;
        }
        catch (Exception e)
        {
            log.error("HMAC-SHA256签名失败", e);
            return "";
        }
    }
}
