package com.junsong.system.domain;

import com.junsong.common.core.web.domain.BaseEntity;

/**
 * R25数据留存策略 sys_data_retention_policy
 */
public class SysDataRetentionPolicy extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 策略ID */
    private Long policyId;

    /** 表名 */
    private String tableName;

    /** 留存天数 */
    private Integer retentionDays;

    /** 归档模式 SUMMARY_ONLY/SOFT_ARCHIVE/DISABLED */
    private String archiveMode;

    /** 是否启用 1是 0否 */
    private String enabled;

    public Long getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId(Long policyId)
    {
        this.policyId = policyId;
    }

    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public Integer getRetentionDays()
    {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays)
    {
        this.retentionDays = retentionDays;
    }

    public String getArchiveMode()
    {
        return archiveMode;
    }

    public void setArchiveMode(String archiveMode)
    {
        this.archiveMode = archiveMode;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }
}
