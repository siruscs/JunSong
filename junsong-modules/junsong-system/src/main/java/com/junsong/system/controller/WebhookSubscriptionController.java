package com.junsong.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.WebhookDelivery;
import com.junsong.system.domain.WebhookSubscription;
import com.junsong.system.service.IWebhookDeliveryService;
import com.junsong.system.service.IWebhookSubscriptionService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Webhook订阅管理Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/webhook/subscription")
public class WebhookSubscriptionController extends BaseController
{
    @Autowired
    private IWebhookSubscriptionService webhookSubscriptionService;

    @Autowired
    private IWebhookDeliveryService webhookDeliveryService;

    @RequiresPermissions("system:webhook:list")
    @GetMapping("/list")
    public TableDataInfo list(WebhookSubscription webhookSubscription)
    {
        startPage();
        List<WebhookSubscription> list = webhookSubscriptionService.selectWebhookSubscriptionList(webhookSubscription);
        return getDataTable(list);
    }

    @RequiresPermissions("system:webhook:export")
    @Log(title = "Webhook订阅", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WebhookSubscription webhookSubscription)
    {
        List<WebhookSubscription> list = webhookSubscriptionService.selectWebhookSubscriptionList(webhookSubscription);
        ExcelUtil<WebhookSubscription> util = new ExcelUtil<>(WebhookSubscription.class);
        util.exportExcel(response, list, "Webhook订阅数据");
    }

    @RequiresPermissions("system:webhook:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(webhookSubscriptionService.selectWebhookSubscriptionById(id));
    }

    @RequiresPermissions("system:webhook:add")
    @Log(title = "Webhook订阅", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WebhookSubscription webhookSubscription)
    {
        return toAjax(webhookSubscriptionService.insertWebhookSubscription(webhookSubscription));
    }

    @RequiresPermissions("system:webhook:edit")
    @Log(title = "Webhook订阅", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WebhookSubscription webhookSubscription)
    {
        return toAjax(webhookSubscriptionService.updateWebhookSubscription(webhookSubscription));
    }

    @RequiresPermissions("system:webhook:remove")
    @Log(title = "Webhook订阅", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(webhookSubscriptionService.deleteWebhookSubscriptionByIds(ids));
    }

    @RequiresPermissions("system:webhook:edit")
    @Log(title = "Webhook订阅", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody WebhookSubscription webhookSubscription)
    {
        return toAjax(webhookSubscriptionService.changeStatus(webhookSubscription));
    }

    @RequiresPermissions("system:webhook:edit")
    @PostMapping("/generateToken")
    public AjaxResult generateToken()
    {
        return success(webhookSubscriptionService.generateSecretToken());
    }

    @RequiresPermissions("system:webhook:list")
    @GetMapping("/delivery/list")
    public TableDataInfo deliveryList(WebhookDelivery webhookDelivery)
    {
        startPage();
        List<WebhookDelivery> list = webhookDeliveryService.selectWebhookDeliveryList(webhookDelivery);
        return getDataTable(list);
    }

    @RequiresPermissions("system:webhook:test")
    @Log(title = "Webhook订阅", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public AjaxResult testEvent(@RequestBody WebhookSubscription testReq)
    {
        webhookDeliveryService.publishEvent(
                testReq.getEventType(),
                null,
                "{\"test\": true, \"message\": \"Webhook测试事件\", \"timestamp\": " + System.currentTimeMillis() + "}"
        );
        return success("测试事件已触发");
    }
}
