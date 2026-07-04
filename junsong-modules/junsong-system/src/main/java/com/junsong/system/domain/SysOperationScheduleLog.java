package com.junsong.system.domain;

import java.util.Date;

/**
 * R21: 运维调度执行日志
 */
public class SysOperationScheduleLog
{
    /** 日志ID */
    private Long logId;

    /** 任务编码 */
    private String jobCode;

    /** 任务名称 */
    private String jobName;

    /** 触发方式：MANUAL / SCHEDULED */
    private String triggerType;

    /** 执行状态：SUCCESS / FAILED / SKIPPED / PARTIAL */
    private String status;

    /** 开始时间 */
    private Date startedAt;

    /** 结束时间 */
    private Date finishedAt;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 影响行数 */
    private Integer affectedRows;

    /** 执行结果摘要 */
    private String resultSummary;

    /** 错误信息 */
    private String errorMessage;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public String getJobCode()
    {
        return jobCode;
    }

    public void setJobCode(String jobCode)
    {
        this.jobCode = jobCode;
    }

    public String getJobName()
    {
        return jobName;
    }

    public void setJobName(String jobName)
    {
        this.jobName = jobName;
    }

    public String getTriggerType()
    {
        return triggerType;
    }

    public void setTriggerType(String triggerType)
    {
        this.triggerType = triggerType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getStartedAt()
    {
        return startedAt;
    }

    public void setStartedAt(Date startedAt)
    {
        this.startedAt = startedAt;
    }

    public Date getFinishedAt()
    {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt)
    {
        this.finishedAt = finishedAt;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public Integer getAffectedRows()
    {
        return affectedRows;
    }

    public void setAffectedRows(Integer affectedRows)
    {
        this.affectedRows = affectedRows;
    }

    public String getResultSummary()
    {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary)
    {
        this.resultSummary = resultSummary;
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
