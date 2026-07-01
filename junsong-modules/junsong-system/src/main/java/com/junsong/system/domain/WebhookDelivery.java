package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Webhook投递记录对象 webhook_delivery
 *
 * @author junsong
 */
public class WebhookDelivery
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long subscriptionId;

    private String eventType;

    private String eventId;

    private String payload;

    private Integer httpStatus;

    private String responseBody;

    private Integer retryCount;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextRetryTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getSubscriptionId()
    {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId)
    {
        this.subscriptionId = subscriptionId;
    }

    public String getEventType()
    {
        return eventType;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public String getEventId()
    {
        return eventId;
    }

    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    public String getPayload()
    {
        return payload;
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }

    public Integer getHttpStatus()
    {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus)
    {
        this.httpStatus = httpStatus;
    }

    public String getResponseBody()
    {
        return responseBody;
    }

    public void setResponseBody(String responseBody)
    {
        this.responseBody = responseBody;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getNextRetryTime()
    {
        return nextRetryTime;
    }

    public void setNextRetryTime(Date nextRetryTime)
    {
        this.nextRetryTime = nextRetryTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getCompleteTime()
    {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime)
    {
        this.completeTime = completeTime;
    }
}
