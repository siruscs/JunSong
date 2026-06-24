package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class LcBizPageSchema extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String pageType;
    private String schemaJson;
    private Integer version;
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

    public String getPageType()
    {
        return pageType;
    }

    public void setPageType(String pageType)
    {
        this.pageType = pageType;
    }

    public String getSchemaJson()
    {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson)
    {
        this.schemaJson = schemaJson;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
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
                .append("pageType", getPageType())
                .append("version", getVersion())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
