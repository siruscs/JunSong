package com.junsong.open.controller.openapi;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.open.context.OpenApiRequestContextHolder;
import com.junsong.open.domain.OpenWebhookSubscription;
import com.junsong.open.service.IOpenWebhookSubscriptionService;

/**
 * Webhook即服务开放API
 *
 * 对外暴露Webhook回调地址登记和事件订阅能力。
 * 公共 openapi 端点由网关 HMAC 签名 + X-Open-* 上下文拦截器保护，不再依赖用户权限注解。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/open/webhooks")
public class OpenWebhookController extends BaseController
{
    @Autowired
    private IOpenWebhookSubscriptionService openWebhookSubscriptionService;

    /**
     * 登记Webhook回调地址和事件订阅
     *
     * @param params 包含 callbackUrl(回调地址) 和 events(订阅事件列表)
     */
    @PostMapping("/subscriptions")
    public AjaxResult createWebhookSubscription(@RequestBody Map<String, Object> params)
    {
        String callbackUrl = params.get("callbackUrl") != null ? params.get("callbackUrl").toString() : null;
        if (callbackUrl == null || callbackUrl.isBlank())
        {
            throw new ServiceException("callbackUrl 不能为空");
        }
        String events = params.get("events") != null ? params.get("events").toString() : "";

        OpenWebhookSubscription subscription = new OpenWebhookSubscription();
        subscription.setTenantId(OpenApiRequestContextHolder.get().getTenantId());
        subscription.setAppId(OpenApiRequestContextHolder.get().getAppId());
        subscription.setCallbackUrl(callbackUrl);
        subscription.setEvents(events);
        subscription.setStatus("0");
        openWebhookSubscriptionService.insertOpenWebhookSubscription(subscription);

        AjaxResult result = success("Webhook订阅已登记");
        result.put("subscriptionId", subscription.getId());
        result.put("callbackUrl", callbackUrl);
        result.put("events", events);
        return result;
    }
}
