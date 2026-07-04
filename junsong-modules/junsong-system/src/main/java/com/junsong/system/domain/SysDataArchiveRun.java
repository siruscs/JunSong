package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * R25数据归档执行记录 sys_data_archive_run
 */
public class SysDataArchiveRun extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 执行ID */
    private Long runId;

    /** 策略ID */
    private Long policyId;

    /** 表名 */
    private String tableName;

    /** 归档截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cutoffTime;

    /** 候选数据量 */
    private Long candidateCount;

    /** 已归档数据量 */
    private Long archivedCount;

    /** 是否试运行 1是 0否 */
    private String dryRun;

    /** 执行状态 SUCCESS/FAILED */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

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

    public Date getCutoffTime()
    {
        return cutoffTime;
    }

    public void setCutoffTime(Date cutoffTime)
    {
        this.cutoffTime = cutoffTime;
    }

    public Long getCandidateCount()
    {
        return candidateCount;
    }

    public void setCandidateCount(Long candidateCount)
    {
        this.candidateCount = candidateCount;
    }

    public Long getArchivedCount()
    {
        return archivedCount;
    }

    public void setArchivedCount(Long archivedCount)
    {
        this.archivedCount = archivedCount;
    }

    public String getDryRun()
    {
        return dryRun;
    }

    public void setDryRun(String dryRun)
    {
        this.dryRun = dryRun;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }
}
