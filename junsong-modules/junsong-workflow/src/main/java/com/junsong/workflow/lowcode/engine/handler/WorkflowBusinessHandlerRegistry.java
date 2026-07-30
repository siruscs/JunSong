package com.junsong.workflow.lowcode.engine.handler;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务流程 Handler 注册表。业务类型必须唯一，避免多个领域处理器争抢同一流程事件。
 */
@Component
public class WorkflowBusinessHandlerRegistry
{
    private final Map<String, WorkflowBusinessHandler> handlers = new ConcurrentHashMap<>();

    public void register(WorkflowBusinessHandler handler)
    {
        if (handler == null || handler.businessType() == null || handler.businessType().isBlank())
        {
            throw new IllegalArgumentException("业务流程处理器 businessType 不能为空");
        }
        WorkflowBusinessHandler previous = handlers.putIfAbsent(handler.businessType(), handler);
        if (previous != null)
        {
            throw new IllegalStateException("业务流程处理器重复注册: " + handler.businessType());
        }
    }

    public WorkflowBusinessHandler getRequired(String businessType)
    {
        WorkflowBusinessHandler handler = handlers.get(businessType);
        if (handler == null)
        {
            throw new IllegalArgumentException("未注册业务流程处理器: " + businessType);
        }
        return handler;
    }

    public boolean contains(String businessType)
    {
        return handlers.containsKey(businessType);
    }
}
