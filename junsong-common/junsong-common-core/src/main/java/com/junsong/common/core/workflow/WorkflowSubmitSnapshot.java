package com.junsong.common.core.workflow;

public class WorkflowSubmitSnapshot
{
    private Long id;
    private String processInstanceId;
    private String processDefinitionKey;
    private String currentTaskName;
    private String operator;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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
