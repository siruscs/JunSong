package com.junsong.workflow.domain;

import java.util.Date;

public class WfTimeoutTriggerLog {
    private Long id;
    private Long timeoutConfigId;
    private String taskId;
    private String processInstanceId;
    private String escalationType;
    private Date triggerTime;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTimeoutConfigId() { return timeoutConfigId; }
    public void setTimeoutConfigId(Long timeoutConfigId) { this.timeoutConfigId = timeoutConfigId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public String getEscalationType() { return escalationType; }
    public void setEscalationType(String escalationType) { this.escalationType = escalationType; }
    public Date getTriggerTime() { return triggerTime; }
    public void setTriggerTime(Date triggerTime) { this.triggerTime = triggerTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
