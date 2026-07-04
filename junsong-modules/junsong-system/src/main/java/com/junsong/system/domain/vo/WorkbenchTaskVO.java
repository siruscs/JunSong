package com.junsong.system.domain.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 统一工作台任务VO。
 *
 * @author junsong
 */
public class WorkbenchTaskVO {

    private String sourceModule;
    private String taskType;
    private String severity;
    private Long deptId;
    private String deptName;
    private String title;
    private String reason;
    private String suggestion;
    private String targetRoute;
    private String status;
    private Date deadline;
    /** 稳定业务ID，用于去重和跳转 */
    private String bizId;
    /** 影响金额 */
    private BigDecimal impactAmount;
    /** 发生时间 */
    private Date occurTime;

    public WorkbenchTaskVO() {}

    public WorkbenchTaskVO(String sourceModule, String taskType, String severity,
                           String title, String reason, String suggestion, String targetRoute) {
        this.sourceModule = sourceModule;
        this.taskType = taskType;
        this.severity = severity;
        this.title = title;
        this.reason = reason;
        this.suggestion = suggestion;
        this.targetRoute = targetRoute;
        this.status = "OPEN";
    }

    public String getSourceModule() { return sourceModule; }
    public void setSourceModule(String sourceModule) { this.sourceModule = sourceModule; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }

    public BigDecimal getImpactAmount() { return impactAmount; }
    public void setImpactAmount(BigDecimal impactAmount) { this.impactAmount = impactAmount; }

    public Date getOccurTime() { return occurTime; }
    public void setOccurTime(Date occurTime) { this.occurTime = occurTime; }
}