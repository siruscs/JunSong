package com.junsong.open.domain;

import com.junsong.common.core.web.domain.BaseEntity;

/**
 * Webhook订阅记录 open_webhook_subscription
 *
 * @author junsong
 */
public class OpenWebhookSubscription extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long appId;

    private String callbackUrl;

    private String events;

    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
