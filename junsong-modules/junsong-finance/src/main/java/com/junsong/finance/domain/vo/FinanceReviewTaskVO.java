package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 店长复盘任务 VO - OPS-C1 店长复盘任务闭环
 */
public class FinanceReviewTaskVO {
    private String taskId;
    private String taskType;     // SALES_DROP / EXPENSE_SPIKE / PROFIT_RATE_DROP / PENDING_VERIFY / PROFIT_SHARE_EXCEPTION / MEMBER_CONTRIBUTION_DROP
    private String taskTitle;
    private String priority;     // HIGH / MEDIUM / LOW
    private Long deptId;
    private String deptName;
    private String reason;
    private BigDecimal metricValue;
    private BigDecimal compareValue;
    private BigDecimal impactAmount;
    private String suggestedAction;
    private String targetRoute;
    private String targetParams;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
    public BigDecimal getCompareValue() { return compareValue; }
    public void setCompareValue(BigDecimal compareValue) { this.compareValue = compareValue; }
    public BigDecimal getImpactAmount() { return impactAmount; }
    public void setImpactAmount(BigDecimal impactAmount) { this.impactAmount = impactAmount; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }
    public String getTargetParams() { return targetParams; }
    public void setTargetParams(String targetParams) { this.targetParams = targetParams; }
}
