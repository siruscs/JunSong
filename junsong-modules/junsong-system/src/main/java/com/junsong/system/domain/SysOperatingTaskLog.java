package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 经营任务操作日志表 sys_operating_task_log
 *
 * 每次状态流转在同一事务内写入一条日志。
 *
 * @author junsong
 */
public class SysOperatingTaskLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 关联任务ID */
    private Long taskId;

    /** 动作：CREATE/CLAIM/COMPLETE/REJECT/REOPEN */
    private String action;

    /** 变更前状态 */
    private String oldStatus;

    /** 变更后状态 */
    private String newStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 处理备注/驳回原因/重开原因 */
    private String note;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getOldStatus()
    {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus)
    {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus()
    {
        return newStatus;
    }

    public void setNewStatus(String newStatus)
    {
        this.newStatus = newStatus;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
