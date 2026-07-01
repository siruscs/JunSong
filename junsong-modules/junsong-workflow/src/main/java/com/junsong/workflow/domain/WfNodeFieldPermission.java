package com.junsong.workflow.domain;

import java.util.Date;

public class WfNodeFieldPermission
{
    private Long id;
    private String processDefinitionKey;
    private String activityId;
    private String fieldKey;
    private String fieldLabel;
    private String permission;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }
    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
