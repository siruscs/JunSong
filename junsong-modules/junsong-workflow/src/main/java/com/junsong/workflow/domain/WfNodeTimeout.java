package com.junsong.workflow.domain;

import java.util.Date;

public class WfNodeTimeout
{
    private Long id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String activityId;
    private String activityName;
    private Integer timeoutMinutes;
    private String escalationType;
    private String escalationTarget;
    private String isWorkday;
    private Date lastTriggerTime;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String processDefinitionId) { this.processDefinitionId = processDefinitionId; }
    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }
    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public Integer getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(Integer timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
    public String getEscalationType() { return escalationType; }
    public void setEscalationType(String escalationType) { this.escalationType = escalationType; }
    public String getEscalationTarget() { return escalationTarget; }
    public void setEscalationTarget(String escalationTarget) { this.escalationTarget = escalationTarget; }
    public String getIsWorkday() { return isWorkday; }
    public void setIsWorkday(String isWorkday) { this.isWorkday = isWorkday; }
    public Date getLastTriggerTime() { return lastTriggerTime; }
    public void setLastTriggerTime(Date lastTriggerTime) { this.lastTriggerTime = lastTriggerTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
