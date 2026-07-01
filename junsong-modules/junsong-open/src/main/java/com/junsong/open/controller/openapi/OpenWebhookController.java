package com.junsong.open.controller.openapi;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;

/**
 * Webhook即服务开放API
 *
 * 对外暴露Webhook回调地址登记和事件订阅能力。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/open/webhooks")
public class OpenWebhookController extends BaseController
{
    /**
     * 登记Webhook回调地址和事件订阅
     *
     * @param params 包含 callbackUrl(回调地址) 和 events(订阅事件列表)
     */
    @PostMapping("/subscriptions")
    @RequiresPermissions("open:foundation:webhook:create")
    public AjaxResult createWebhookSubscription(@RequestBody Map<String, Object> params)
    {
        // TODO: 持久化webhook订阅（当前返回确认信息，待后续迭代完善存储层）
        AjaxResult result = success("Webhook订阅已登记");
        result.put("callbackUrl", params.get("callbackUrl"));
        result.put("events", params.get("events"));
        return result;
    }
}
