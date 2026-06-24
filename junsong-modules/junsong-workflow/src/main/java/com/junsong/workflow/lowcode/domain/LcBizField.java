package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizField extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private String componentType;
    private String required;
    private String defaultValue;
    private String dictType;
    private String uploadConfig;
    private String fieldExt;
    private String validateRule;
    private String stage;
    private String isQuery;
    private String isList;
    private String isDetail;
    private String isProcessVar;
    private String processVarName;
    private String parentFieldKey;
    private Integer orderNum;
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

    public String getFieldKey()
    {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey)
    {
        this.fieldKey = fieldKey;
    }

    public String getFieldLabel()
    {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel)
    {
        this.fieldLabel = fieldLabel;
    }

    public String getFieldType()
    {
        return fieldType;
    }

    public void setFieldType(String fieldType)
    {
        this.fieldType = fieldType;
    }

    public String getComponentType()
    {
        return componentType;
    }

    public void setComponentType(String componentType)
    {
        this.componentType = componentType;
    }

    public String getRequired()
    {
        return required;
    }

    public void setRequired(String required)
    {
        this.required = required;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public String getDictType()
    {
        return dictType;
    }

    public void setDictType(String dictType)
    {
        this.dictType = dictType;
    }

    public String getUploadConfig()
    {
        return uploadConfig;
    }

    public void setUploadConfig(String uploadConfig)
    {
        this.uploadConfig = uploadConfig;
    }

    public String getFieldExt()
    {
        return fieldExt;
    }

    public void setFieldExt(String fieldExt)
    {
        this.fieldExt = fieldExt;
    }

    public String getValidateRule()
    {
        return validateRule;
    }

    public void setValidateRule(String validateRule)
    {
        this.validateRule = validateRule;
    }

    public String getStage()
    {
        return stage;
    }

    public void setStage(String stage)
    {
        this.stage = stage;
    }

    public String getIsQuery()
    {
        return isQuery;
    }

    public void setIsQuery(String isQuery)
    {
        this.isQuery = isQuery;
    }

    public String getIsList()
    {
        return isList;
    }

    public void setIsList(String isList)
    {
        this.isList = isList;
    }

    public String getIsDetail()
    {
        return isDetail;
    }

    public void setIsDetail(String isDetail)
    {
        this.isDetail = isDetail;
    }

    public String getIsProcessVar()
    {
        return isProcessVar;
    }

    public void setIsProcessVar(String isProcessVar)
    {
        this.isProcessVar = isProcessVar;
    }

    public String getProcessVarName()
    {
        return processVarName;
    }

    public void setProcessVarName(String processVarName)
    {
        this.processVarName = processVarName;
    }

    public String getParentFieldKey()
    {
        return parentFieldKey;
    }

    public void setParentFieldKey(String parentFieldKey)
    {
        this.parentFieldKey = parentFieldKey;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
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
                .append("fieldKey", getFieldKey())
                .append("fieldLabel", getFieldLabel())
                .append("fieldType", getFieldType())
                .append("componentType", getComponentType())
                .append("stage", getStage())
                .append("orderNum", getOrderNum())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
