package com.junsong.system.api.domain;

import java.io.Serial;
import java.io.Serializable;

public class StoreOpeningWorkflowSyncReq implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

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
