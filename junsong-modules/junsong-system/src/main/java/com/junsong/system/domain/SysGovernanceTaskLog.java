package com.junsong.system.domain;

import java.util.Date;

/**
 * 系统治理任务处理轨迹
 */
public class SysGovernanceTaskLog
{
    /** 日志ID */
    private Long logId;

    /** 治理任务类型 */
    private String taskType;

    /** 严重程度 */
    private String severity;

    /** 关联数量 */
    private Integer countValue;

    /** 操作类型：ACK/DONE/IGNORED/REOPEN */
    private String actionType;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 处理备注 */
    private String handlerNote;

    /** 操作时间 */
    private Date actionTime;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public String getTaskType()
    {
        return taskType;
    }

    public void setTaskType(String taskType)
    {
        this.taskType = taskType;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public Integer getCountValue()
    {
        return countValue;
    }

    public void setCountValue(Integer countValue)
    {
        this.countValue = countValue;
    }

    public String getActionType()
    {
        return actionType;
    }

    public void setActionType(String actionType)
    {
        this.actionType = actionType;
    }

    public Long getHandlerId()
    {
        return handlerId;
    }

    public void setHandlerId(Long handlerId)
    {
        this.handlerId = handlerId;
    }

    public String getHandlerName()
    {
        return handlerName;
    }

    public void setHandlerName(String handlerName)
    {
        this.handlerName = handlerName;
    }

    public String getHandlerNote()
    {
        return handlerNote;
    }

    public void setHandlerNote(String handlerNote)
    {
        this.handlerNote = handlerNote;
    }

    public Date getActionTime()
    {
        return actionTime;
    }

    public void setActionTime(Date actionTime)
    {
        this.actionTime = actionTime;
    }
}
