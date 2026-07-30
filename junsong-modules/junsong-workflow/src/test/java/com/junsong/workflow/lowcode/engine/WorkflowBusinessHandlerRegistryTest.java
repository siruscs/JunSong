package com.junsong.workflow.lowcode.engine;

import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessHandler;
import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessHandlerRegistry;
import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowBusinessHandlerRegistryTest
{
    @Test
    void resolvesHandlerByBusinessType()
    {
        WorkflowBusinessHandlerRegistry registry = new WorkflowBusinessHandlerRegistry();
        WorkflowBusinessHandler handler = new TestHandler("expense_reimbursement");

        registry.register(handler);

        assertEquals(handler, registry.getRequired("expense_reimbursement"));
    }

    @Test
    void rejectsDuplicateBusinessType()
    {
        WorkflowBusinessHandlerRegistry registry = new WorkflowBusinessHandlerRegistry();
        registry.register(new TestHandler("leave_request"));

        assertThrows(IllegalStateException.class,
                () -> registry.register(new TestHandler("leave_request")));
    }

    @Test
    void missingBusinessTypeHasActionableError()
    {
        WorkflowBusinessHandlerRegistry registry = new WorkflowBusinessHandlerRegistry();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> registry.getRequired("stocktake"));

        assertEquals("未注册业务流程处理器: stocktake", error.getMessage());
    }

    @Test
    void contextValidatesRequiredIdentityAndFreezesVariables()
    {
        Map<String, Object> input = new HashMap<>();
        input.put("amount", 10);
        WorkflowBusinessContext context = new WorkflowBusinessContext(
                "expense", "1", "EXP-1", 1L, 2L, null, "key-1", input);
        input.put("amount", 99);
        assertEquals(10, context.getVariables().get("amount"));
        assertThrows(UnsupportedOperationException.class, () -> context.getVariables().put("x", 1));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new WorkflowBusinessContext("", "1", "EXP-1", 1L, 2L, null, null, Map.of()));
        assertTrue(error.getMessage().contains("businessType"));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowBusinessContext("expense", "1", "EXP-1", null, 2L, null, null, Map.of()));
    }

    private record TestHandler(String businessType) implements WorkflowBusinessHandler
    {
        @Override
        public void validateBeforeSubmit(WorkflowBusinessContext context) {}

        @Override
        public void beforeTaskComplete(WorkflowBusinessContext context, String taskKey) {}

        @Override
        public void afterApprove(WorkflowBusinessContext context) {}

        @Override
        public void afterReject(WorkflowBusinessContext context) {}

        @Override
        public void afterCancel(WorkflowBusinessContext context) {}
    }
}
