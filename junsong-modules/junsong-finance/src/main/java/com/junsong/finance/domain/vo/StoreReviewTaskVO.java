package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class StoreReviewTaskVO {
    private Long deptId;
    private String deptName;
    private String taskType;
    private String severity;
    private String title;
    private String reason;
    private BigDecimal amount;
    private Integer priority;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
