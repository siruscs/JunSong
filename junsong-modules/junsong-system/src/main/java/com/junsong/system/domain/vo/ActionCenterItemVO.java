package com.junsong.system.domain.vo;

public class ActionCenterItemVO {
    private String actionId;
    private String sourceType;
    private String sourceId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String ownerName;
    private Long ownerId;
    private Long deptId;
    private String deptName;
    private String dueDate;
    private String effectStatus;
    private String drilldownPath;
    private String latestTouchStatus;
    private String latestTouchTime;
    private Integer touchCount24h;
    private Boolean touchable;
    private String touchDisabledReason;

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getEffectStatus() { return effectStatus; }
    public void setEffectStatus(String effectStatus) { this.effectStatus = effectStatus; }
    public String getDrilldownPath() { return drilldownPath; }
    public void setDrilldownPath(String drilldownPath) { this.drilldownPath = drilldownPath; }
    public String getLatestTouchStatus() { return latestTouchStatus; }
    public void setLatestTouchStatus(String latestTouchStatus) { this.latestTouchStatus = latestTouchStatus; }
    public String getLatestTouchTime() { return latestTouchTime; }
    public void setLatestTouchTime(String latestTouchTime) { this.latestTouchTime = latestTouchTime; }
    public Integer getTouchCount24h() { return touchCount24h; }
    public void setTouchCount24h(Integer touchCount24h) { this.touchCount24h = touchCount24h; }
    public Boolean getTouchable() { return touchable; }
    public void setTouchable(Boolean touchable) { this.touchable = touchable; }
    public String getTouchDisabledReason() { return touchDisabledReason; }
    public void setTouchDisabledReason(String touchDisabledReason) { this.touchDisabledReason = touchDisabledReason; }
}
