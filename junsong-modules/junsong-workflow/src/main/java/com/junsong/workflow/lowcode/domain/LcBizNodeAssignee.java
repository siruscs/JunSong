package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizNodeAssignee extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String taskKey;
    private String taskName;
    private String assigneeSource;
    private String assigneeValue;
    private String assigneeExpr;
    private String processVarName;
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

    public String getTaskKey()
    {
        return taskKey;
    }

    public void setTaskKey(String taskKey)
    {
        this.taskKey = taskKey;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getAssigneeSource()
    {
        return assigneeSource;
    }

    public void setAssigneeSource(String assigneeSource)
    {
        this.assigneeSource = assigneeSource;
    }

    public String getAssigneeValue()
    {
        return assigneeValue;
    }

    public void setAssigneeValue(String assigneeValue)
    {
        this.assigneeValue = assigneeValue;
    }

    public String getAssigneeExpr()
    {
        return assigneeExpr;
    }

    public void setAssigneeExpr(String assigneeExpr)
    {
        this.assigneeExpr = assigneeExpr;
    }

    public String getProcessVarName()
    {
        return processVarName;
    }

    public void setProcessVarName(String processVarName)
    {
        this.processVarName = processVarName;
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
                .append("taskKey", getTaskKey())
                .append("taskName", getTaskName())
                .append("assigneeSource", getAssigneeSource())
                .append("assigneeValue", getAssigneeValue())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
