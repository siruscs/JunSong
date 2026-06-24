package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizPostAction extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String triggerEvent;
    private String actionType;
    private String targetField;
    private String targetValue;
    private String conditionExpr;
    private String callbackUrl;
    private Integer sortOrder;
    private String status;
    private String delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getBizCode()
    {
        return bizCode;
    }

    public void setBizCode(String bizCode)
    {
        this.bizCode = bizCode;
    }

    public String getTriggerEvent()
    {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent)
    {
        this.triggerEvent = triggerEvent;
    }

    public String getActionType()
    {
        return actionType;
    }

    public void setActionType(String actionType)
    {
        this.actionType = actionType;
    }

    public String getTargetField()
    {
        return targetField;
    }

    public void setTargetField(String targetField)
    {
        this.targetField = targetField;
    }

    public String getTargetValue()
    {
        return targetValue;
    }

    public void setTargetValue(String targetValue)
    {
        this.targetValue = targetValue;
    }

    public String getConditionExpr()
    {
        return conditionExpr;
    }

    public void setConditionExpr(String conditionExpr)
    {
        this.conditionExpr = conditionExpr;
    }

    public String getCallbackUrl()
    {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl)
    {
        this.callbackUrl = callbackUrl;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("bizCode", getBizCode())
                .append("triggerEvent", getTriggerEvent())
                .append("actionType", getActionType())
                .append("sortOrder", getSortOrder())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .toString();
    }
}
