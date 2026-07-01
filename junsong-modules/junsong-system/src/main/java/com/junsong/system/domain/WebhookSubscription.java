package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * Webhook订阅对象 webhook_subscription
 *
 * @author junsong
 */
public class WebhookSubscription extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String subscriptionName;

    private String eventType;

    private String callbackUrl;

    private String secretToken;

    private String status;

    private Integer maxRetries;

    private String delFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

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

    public String getSubscriptionName()
    {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName)
    {
        this.subscriptionName = subscriptionName;
    }

    public String getEventType()
    {
        return eventType;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public String getCallbackUrl()
    {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl)
    {
        this.callbackUrl = callbackUrl;
    }

    public String getSecretToken()
    {
        return secretToken;
    }

    public void setSecretToken(String secretToken)
    {
        this.secretToken = secretToken;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getMaxRetries()
    {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries)
    {
        this.maxRetries = maxRetries;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public Date getUpdateTime()
    {
        return updateTime;
    }

    @Override
    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
