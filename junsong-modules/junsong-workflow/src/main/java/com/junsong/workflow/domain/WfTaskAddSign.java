package com.junsong.workflow.domain;

import java.util.Date;

public class WfTaskAddSign {
    private Long id;
    private String originalTaskId;
    private String addsignTaskId;
    private String addsignUser;
    private String type;
    private String processInstanceId;
    private Date createTime;
    private Date completeTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOriginalTaskId() { return originalTaskId; }
    public void setOriginalTaskId(String originalTaskId) { this.originalTaskId = originalTaskId; }
    public String getAddsignTaskId() { return addsignTaskId; }
    public void setAddsignTaskId(String addsignTaskId) { this.addsignTaskId = addsignTaskId; }
    public String getAddsignUser() { return addsignUser; }
    public void setAddsignUser(String addsignUser) { this.addsignUser = addsignUser; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getCompleteTime() { return completeTime; }
    public void setCompleteTime(Date completeTime) { this.completeTime = completeTime; }
}
