package com.junsong.workflow.service.sync;

import java.util.Map;

public interface WorkflowBusinessSyncHandler
{
    boolean supports(String processDefinitionId);

    /** 业务专用同步处理器可覆盖通用低代码处理器。 */
    default int priority()
    {
        return 0;
    }

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

    default void afterComplete(String processInstanceId, String operator) {}

    default void afterComplete(String processInstanceId, String operator, Map<String, Object> variables)
    {
        afterComplete(processInstanceId, operator);
    }
}
