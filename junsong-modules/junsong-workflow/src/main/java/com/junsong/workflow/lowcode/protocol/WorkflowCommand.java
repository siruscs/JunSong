package com.junsong.workflow.lowcode.protocol;

import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessContext;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record WorkflowCommand(WorkflowAction action, WorkflowBusinessContext context,
                              String operator, Map<String, Object> variables)
{
    public WorkflowCommand
    {
        variables = variables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }
}
