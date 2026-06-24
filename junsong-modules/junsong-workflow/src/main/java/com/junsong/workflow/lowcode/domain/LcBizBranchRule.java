package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizBranchRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String gatewayKey;
    private String fieldKey;
    private String operator;
    private String compareValue;
    private String targetVarName;
    private String groupOp;
    private Long parentRuleId;
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

    public String getGatewayKey()
    {
        return gatewayKey;
    }

    public void setGatewayKey(String gatewayKey)
    {
        this.gatewayKey = gatewayKey;
    }

    public String getFieldKey()
    {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey)
    {
        this.fieldKey = fieldKey;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator(String operator)
    {
        this.operator = operator;
    }

    public String getCompareValue()
    {
        return compareValue;
    }

    public void setCompareValue(String compareValue)
    {
        this.compareValue = compareValue;
    }

    public String getTargetVarName()
    {
        return targetVarName;
    }

    public void setTargetVarName(String targetVarName)
    {
        this.targetVarName = targetVarName;
    }

    public String getGroupOp()
    {
        return groupOp;
    }

    public void setGroupOp(String groupOp)
    {
        this.groupOp = groupOp;
    }

    public Long getParentRuleId()
    {
        return parentRuleId;
    }

    public void setParentRuleId(Long parentRuleId)
    {
        this.parentRuleId = parentRuleId;
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
                .append("gatewayKey", getGatewayKey())
                .append("fieldKey", getFieldKey())
                .append("operator", getOperator())
                .append("compareValue", getCompareValue())
                .append("targetVarName", getTargetVarName())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
