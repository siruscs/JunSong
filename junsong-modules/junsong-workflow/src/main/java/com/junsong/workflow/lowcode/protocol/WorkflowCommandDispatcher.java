package com.junsong.workflow.lowcode.protocol;

import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessContext;
import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessHandler;
import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessHandlerRegistry;
import org.springframework.stereotype.Component;

/** 将通用流程动作分发到业务适配器，不允许平台层直接依赖领域表。 */
@Component
public class WorkflowCommandDispatcher
{
    private final WorkflowBusinessHandlerRegistry registry;
    private final WorkflowCommandValidator validator;

    public WorkflowCommandDispatcher(WorkflowBusinessHandlerRegistry registry,
                                     WorkflowCommandValidator validator)
    {
        this.registry = registry;
        this.validator = validator;
    }

    public void dispatch(WorkflowCommand command)
    {
        validator.validate(command);
        WorkflowBusinessHandler handler = registry.getRequired(command.context().getBusinessType());
        WorkflowBusinessContext context = command.context();
        switch (command.action())
        {
            case START, SUBMIT -> handler.validateBeforeSubmit(context);
            case APPROVE -> {
                Object taskKey = command.variables().get("taskKey");
                if (taskKey == null || taskKey.toString().isBlank())
                {
                    throw new IllegalArgumentException("审批动作必须携带 taskKey");
                }
                handler.beforeTaskComplete(context, taskKey.toString());
                handler.afterApprove(context);
            }
            case REJECT -> handler.afterReject(context);
            case CANCEL, WITHDRAW -> handler.afterCancel(context);
            case FULFILL, SYNC -> throw new IllegalStateException(
                    "流程动作尚未绑定业务适配器: " + command.action());
        }
    }
}
