package com.junsong.workflow.lowcode.protocol;

import org.springframework.stereotype.Component;

/** 通用流程动作契约校验，业务模块不得绕过统一上下文。 */
@Component
public class WorkflowCommandValidator
{
    public void validate(WorkflowCommand command)
    {
        if (command == null || command.action() == null)
        {
            throw new IllegalArgumentException("流程动作不能为空");
        }
        if (command.context() == null)
        {
            throw new IllegalArgumentException("流程业务上下文不能为空");
        }
        if (command.operator() == null || command.operator().isBlank())
        {
            throw new IllegalArgumentException("流程操作人不能为空");
        }
        if ((command.action() == WorkflowAction.START || command.action() == WorkflowAction.SUBMIT)
                && (command.context().getProcessInstanceId() != null
                && !command.context().getProcessInstanceId().isBlank()))
        {
            throw new IllegalArgumentException("启动/提交动作不能携带已有流程实例");
        }
        if ((command.action() == WorkflowAction.START || command.action() == WorkflowAction.SUBMIT)
                && (command.context().getIdempotencyKey() == null
                || command.context().getIdempotencyKey().isBlank()))
        {
            throw new IllegalArgumentException("启动/提交动作必须携带幂等键");
        }
        if (requiresProcessInstance(command.action())
                && (command.context().getProcessInstanceId() == null
                || command.context().getProcessInstanceId().isBlank()))
        {
            throw new IllegalArgumentException("该流程动作必须携带流程实例");
        }
        if (command.action() == WorkflowAction.SYNC
                && (command.context().getProcessInstanceId() == null
                || command.context().getProcessInstanceId().isBlank()))
        {
            throw new IllegalArgumentException("同步动作必须携带流程实例");
        }
    }

    private static boolean requiresProcessInstance(WorkflowAction action)
    {
        return switch (action)
        {
            case APPROVE, REJECT, WITHDRAW, CANCEL, FULFILL, SYNC -> true;
            default -> false;
        };
    }
}
