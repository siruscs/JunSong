package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.workflow.AbstractWorkflowBusinessEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizInstance extends AbstractWorkflowBusinessEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String orderNo;
    private String formData;
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

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getFormData()
    {
        return formData;
    }

    public void setFormData(String formData)
    {
        this.formData = formData;
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
                .append("orderNo", getOrderNo())
                .append("workflowStatus", getWorkflowStatus())
                .append("currentTaskName", getCurrentTaskName())
                .append("processInstanceId", getProcessInstanceId())
                .append("submitter", getSubmitter())
                .append("submitTime", getSubmitTime())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
