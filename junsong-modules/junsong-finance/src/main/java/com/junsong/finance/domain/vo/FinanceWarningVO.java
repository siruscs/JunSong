package com.junsong.finance.domain.vo;

public class FinanceWarningVO {
    private String warningType;
    private String severity;
    private String title;
    private String message;
    private Long deptId;
    private String deptName;

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
}
