package com.junsong.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 经营任务表 sys_operating_task
 *
 * 跨模块经营任务的统一持久化与状态流转载体。
 * 字段定义见设计文档 §1。tenantId 继承自 BaseEntity，不重复声明。
 *
 * @author junsong
 */
public class SysOperatingTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 任务标题 */
    private String title;

    /** 触发原因（来自源诊断） */
    private String reason;

    /** 建议动作 */
    private String suggestion;

    /** 来源模块：FINANCE/MEMBER/STOCK/SYSTEM/WORKFLOW */
    private String sourceModule;

    /** 来源类型（如 REVIEW_TASK/RECEIVABLE_COLLECTION/NEGATIVE_STOCK 等） */
    private String sourceType;

    /** 来源单据ID */
    private String sourceId;

    /** 来源单据跳转路由（PC 端） */
    private String sourceRoute;

    /** 任务类型码（对应诊断规则） */
    private String taskType;

    /** 状态：PENDING/IN_PROGRESS/DONE/REJECTED/REOPENED */
    private String status;

    /** 优先级：URGENT/HIGH/MEDIUM/LOW */
    private String priority;

    /** 严重级别（来自源）：HIGH/MEDIUM/LOW */
    private String severity;

    /** 关联门店ID */
    private Long deptId;

    /** 门店名称（冗余，便于列表展示） */
    private String deptName;

    /** 负责人ID（认领后写入） */
    private Long assigneeId;

    /** 负责人姓名 */
    private String assigneeName;

    /** 创建人ID */
    private Long creatorId;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 来源发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date occurTime;

    /** 影响金额 */
    private BigDecimal impactAmount;

    /** 幂等键 {tenantId}:{sourceModule}:{sourceType}:{sourceId} */
    private String idempotencyKey;

    /** 处理备注（完成/驳回时写入） */
    private String handlerNote;

    /** 驳回原因 */
    private String rejectReason;

    /** 重开次数 */
    private Integer reopenCount;

    /** 乐观锁版本号 */
    private Integer version;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getSuggestion()
    {
        return suggestion;
    }

    public void setSuggestion(String suggestion)
    {
        this.suggestion = suggestion;
    }

    public String getSourceModule()
    {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule)
    {
        this.sourceModule = sourceModule;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(String sourceId)
    {
        this.sourceId = sourceId;
    }

    public String getSourceRoute()
    {
        return sourceRoute;
    }

    public void setSourceRoute(String sourceRoute)
    {
        this.sourceRoute = sourceRoute;
    }

    public String getTaskType()
    {
        return taskType;
    }

    public void setTaskType(String taskType)
    {
        this.taskType = taskType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public Long getAssigneeId()
    {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId)
    {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName()
    {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName)
    {
        this.assigneeName = assigneeName;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
    }

    public Date getDueTime()
    {
        return dueTime;
    }

    public void setDueTime(Date dueTime)
    {
        this.dueTime = dueTime;
    }

    public Date getOccurTime()
    {
        return occurTime;
    }

    public void setOccurTime(Date occurTime)
    {
        this.occurTime = occurTime;
    }

    public BigDecimal getImpactAmount()
    {
        return impactAmount;
    }

    public void setImpactAmount(BigDecimal impactAmount)
    {
        this.impactAmount = impactAmount;
    }

    public String getIdempotencyKey()
    {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey)
    {
        this.idempotencyKey = idempotencyKey;
    }

    public String getHandlerNote()
    {
        return handlerNote;
    }

    public void setHandlerNote(String handlerNote)
    {
        this.handlerNote = handlerNote;
    }

    public String getRejectReason()
    {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason)
    {
        this.rejectReason = rejectReason;
    }

    public Integer getReopenCount()
    {
        return reopenCount;
    }

    public void setReopenCount(Integer reopenCount)
    {
        this.reopenCount = reopenCount;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }
}
