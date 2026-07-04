package com.junsong.system.domain.vo;

public class ActionCenterQueryParams {
    private String sourceType;
    private String status;
    private String priority;
    private String beginDate;
    private String endDate;
    private Boolean onlyToday;
    private Boolean onlyOverdue;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getBeginDate() { return beginDate; }
    public void setBeginDate(String beginDate) { this.beginDate = beginDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Boolean getOnlyToday() { return onlyToday; }
    public void setOnlyToday(Boolean onlyToday) { this.onlyToday = onlyToday; }
    public Boolean getOnlyOverdue() { return onlyOverdue; }
    public void setOnlyOverdue(Boolean onlyOverdue) { this.onlyOverdue = onlyOverdue; }
}
