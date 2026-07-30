package com.junsong.workflow.lowcode.engine.handler;

/**
 * 领域业务与通用工作流之间的适配边界。
 * 工作流平台不直接写具体业务表，由领域 Handler 执行业务动作。
 */
public interface WorkflowBusinessHandler
{
    String businessType();

    void validateBeforeSubmit(WorkflowBusinessContext context);

    void beforeTaskComplete(WorkflowBusinessContext context, String taskKey);

    void afterApprove(WorkflowBusinessContext context);

    void afterReject(WorkflowBusinessContext context);

    void afterCancel(WorkflowBusinessContext context);
}
