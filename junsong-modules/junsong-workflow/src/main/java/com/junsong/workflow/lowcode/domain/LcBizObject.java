package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizObject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String bizName;
    private String storageMode;
    private String tableName;
    private String pkField;
    private String orderNoField;
    private String orderNoPrefix;
    private String statusField;
    private String workflowEnabled;
    private String processKey;
    private String fulfillmentEnabled;
    private String menuParentPath;
    private String submitValidators;  // 提交前校验器配置(JSON数组)
    private String status;
    private String configStatus;      // 配置状态: DRAFT/PUBLISHED
    private Integer publishedVersion; // 已发布版本号
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

    public String getBizName()
    {
        return bizName;
    }

    public void setBizName(String bizName)
    {
        this.bizName = bizName;
    }

    public String getStorageMode()
    {
        return storageMode;
    }

    public void setStorageMode(String storageMode)
    {
        this.storageMode = storageMode;
    }

    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public String getPkField()
    {
        return pkField;
    }

    public void setPkField(String pkField)
    {
        this.pkField = pkField;
    }

    public String getOrderNoField()
    {
        return orderNoField;
    }

    public void setOrderNoField(String orderNoField)
    {
        this.orderNoField = orderNoField;
    }

    public String getOrderNoPrefix()
    {
        return orderNoPrefix;
    }

    public void setOrderNoPrefix(String orderNoPrefix)
    {
        this.orderNoPrefix = orderNoPrefix;
    }

    public String getStatusField()
    {
        return statusField;
    }

    public void setStatusField(String statusField)
    {
        this.statusField = statusField;
    }

    public String getWorkflowEnabled()
    {
        return workflowEnabled;
    }

    public void setWorkflowEnabled(String workflowEnabled)
    {
        this.workflowEnabled = workflowEnabled;
    }

    public String getProcessKey()
    {
        return processKey;
    }

    public void setProcessKey(String processKey)
    {
        this.processKey = processKey;
    }

    public String getFulfillmentEnabled()
    {
        return fulfillmentEnabled;
    }

    public void setFulfillmentEnabled(String fulfillmentEnabled)
    {
        this.fulfillmentEnabled = fulfillmentEnabled;
    }

    public String getMenuParentPath()
    {
        return menuParentPath;
    }

    public void setMenuParentPath(String menuParentPath)
    {
        this.menuParentPath = menuParentPath;
    }

    public String getSubmitValidators()
    {
        return submitValidators;
    }

    public void setSubmitValidators(String submitValidators)
    {
        this.submitValidators = submitValidators;
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

    public String getConfigStatus()
    {
        return configStatus;
    }

    public void setConfigStatus(String configStatus)
    {
        this.configStatus = configStatus;
    }

    public Integer getPublishedVersion()
    {
        return publishedVersion;
    }

    public void setPublishedVersion(Integer publishedVersion)
    {
        this.publishedVersion = publishedVersion;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("bizCode", getBizCode())
                .append("bizName", getBizName())
                .append("storageMode", getStorageMode())
                .append("processKey", getProcessKey())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
