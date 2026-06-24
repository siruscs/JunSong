package com.junsong.common.core.workflow;

public class WorkflowSyncSnapshot
{
    private String processInstanceId;
    private String workflowStatus;
    private String currentTaskName;
    private String operator;

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public String getWorkflowStatus()
    {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus)
    {
        this.workflowStatus = workflowStatus;
    }

    public String getCurrentTaskName()
    {
        return currentTaskName;
    }

    public void setCurrentTaskName(String currentTaskName)
    {
        this.currentTaskName = currentTaskName;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator(String operator)
    {
        this.operator = operator;
    }
}
