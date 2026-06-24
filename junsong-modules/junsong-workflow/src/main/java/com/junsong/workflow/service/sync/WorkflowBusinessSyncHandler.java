package com.junsong.workflow.service.sync;

import java.util.Map;

public interface WorkflowBusinessSyncHandler
{
    boolean supports(String processDefinitionId);

    default void afterApprove(String currentTaskName, String processInstanceId, String operator)
    {
        afterApprove(currentTaskName, processInstanceId, operator, Map.of());
    }

    default void afterApprove(String currentTaskName, String processInstanceId, String operator, Map<String, Object> variables)
    {
        afterApprove(currentTaskName, processInstanceId, operator);
    }

    void afterReject(String processInstanceId, String operator);

    default void afterSubmit(String processInstanceId, String operator) {}

    default void afterWithdraw(String processInstanceId, String operator) {}

    default void afterFulfill(String processInstanceId, String operator) {}
}
