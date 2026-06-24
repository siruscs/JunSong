package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizAction extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String actionCode;
    private String actionName;
    private String actionType;
    private String triggerStatus;
    private String apiEndpoint;
    private String buttonStyle;
    private String buttonIcon;
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

    public String getActionCode()
    {
        return actionCode;
    }

    public void setActionCode(String actionCode)
    {
        this.actionCode = actionCode;
    }

    public String getActionName()
    {
        return actionName;
    }

    public void setActionName(String actionName)
    {
        this.actionName = actionName;
    }

    public String getActionType()
    {
        return actionType;
    }

    public void setActionType(String actionType)
    {
        this.actionType = actionType;
    }

    public String getTriggerStatus()
    {
        return triggerStatus;
    }

    public void setTriggerStatus(String triggerStatus)
    {
        this.triggerStatus = triggerStatus;
    }

    public String getApiEndpoint()
    {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint)
    {
        this.apiEndpoint = apiEndpoint;
    }

    public String getButtonStyle()
    {
        return buttonStyle;
    }

    public void setButtonStyle(String buttonStyle)
    {
        this.buttonStyle = buttonStyle;
    }

    public String getButtonIcon()
    {
        return buttonIcon;
    }

    public void setButtonIcon(String buttonIcon)
    {
        this.buttonIcon = buttonIcon;
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
                .append("actionCode", getActionCode())
                .append("actionName", getActionName())
                .append("actionType", getActionType())
                .append("sortOrder", getSortOrder())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .toString();
    }
}
