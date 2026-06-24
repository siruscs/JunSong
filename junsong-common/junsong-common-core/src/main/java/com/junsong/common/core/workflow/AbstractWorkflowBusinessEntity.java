package com.junsong.common.core.workflow;

import com.junsong.common.core.web.domain.BaseEntity;
import java.util.Date;

public abstract class AbstractWorkflowBusinessEntity extends BaseEntity
{
    private String processInstanceId;
    private String processDefinitionKey;
    private String workflowStatus;
    private String currentTaskName;
    private String submitter;
    private Date submitTime;

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionKey()
    {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey)
    {
        this.processDefinitionKey = processDefinitionKey;
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

    public String getSubmitter()
    {
        return submitter;
    }

    public void setSubmitter(String submitter)
    {
        this.submitter = submitter;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }
}
