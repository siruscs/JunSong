package com.junsong.system.domain;

import com.junsong.common.core.web.domain.BaseEntity;

public class SysRegion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String code;

    private String name;

    private String parentCode;

    private Integer level;

    private Integer sort;

    private String status;

    private String sourceYear;

    private String sourceName;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getParentCode()
    {
        return parentCode;
    }

    public void setParentCode(String parentCode)
    {
        this.parentCode = parentCode;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSourceYear()
    {
        return sourceYear;
    }

    public void setSourceYear(String sourceYear)
    {
        this.sourceYear = sourceYear;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }
}
