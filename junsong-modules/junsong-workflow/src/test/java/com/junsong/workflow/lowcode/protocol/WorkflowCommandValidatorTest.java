package com.junsong.workflow.lowcode.protocol;

import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessContext;
import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessHandler;
import com.junsong.workflow.lowcode.engine.handler.WorkflowBusinessHandlerRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowCommandValidatorTest
{
    private final WorkflowCommandValidator validator = new WorkflowCommandValidator();

    @Test
    void acceptsStartAndSyncWithRequiredContext()
    {
        WorkflowBusinessContext startContext = context(null);
        assertDoesNotThrow(() -> validator.validate(new WorkflowCommand(
                WorkflowAction.START, startContext, "admin", Map.of("amount", 10))));
        assertDoesNotThrow(() -> validator.validate(new WorkflowCommand(
                WorkflowAction.SYNC, context("pi-1"), "system", Map.of())));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new WorkflowCommand(
                WorkflowAction.START, context(null, null), "admin", Map.of())));
    }

    @Test
    void rejectsMissingOperatorAndInvalidInstanceCombinations()
    {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new WorkflowCommand(WorkflowAction.START, context(null), "", Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new WorkflowCommand(WorkflowAction.START, context("pi-1"), "admin", Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new WorkflowCommand(WorkflowAction.SYNC, context(null), "system", Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new WorkflowCommand(WorkflowAction.APPROVE, context(null), "admin", Map.of("taskKey", "approve"))));
    }

    @Test
    void dispatcherRoutesActionToBusinessHandler()
    {
        WorkflowBusinessHandlerRegistry registry = new WorkflowBusinessHandlerRegistry();
        RecordingHandler handler = new RecordingHandler();
        registry.register(handler);
        WorkflowCommandDispatcher dispatcher = new WorkflowCommandDispatcher(registry, validator);

        dispatcher.dispatch(new WorkflowCommand(WorkflowAction.APPROVE, context("pi-1"), "admin", Map.of("taskKey", "Task_Approve")));

        org.junit.jupiter.api.Assertions.assertEquals(1, handler.approved);
        org.junit.jupiter.api.Assertions.assertEquals("Task_Approve", handler.taskKey);
        assertThrows(IllegalArgumentException.class,
                () -> dispatcher.dispatch(new WorkflowCommand(WorkflowAction.APPROVE, context("pi-1"), "admin", Map.of())));
    }

    @Test
    void dispatcherRoutesRejectAndCancelCallbacks()
    {
        WorkflowBusinessHandlerRegistry registry = new WorkflowBusinessHandlerRegistry();
        RecordingHandler handler = new RecordingHandler();
        registry.register(handler);
        WorkflowCommandDispatcher dispatcher = new WorkflowCommandDispatcher(registry, validator);

        dispatcher.dispatch(new WorkflowCommand(WorkflowAction.REJECT, context("pi-1"), "admin", Map.of()));
        dispatcher.dispatch(new WorkflowCommand(WorkflowAction.CANCEL, context("pi-1"), "admin", Map.of()));

        org.junit.jupiter.api.Assertions.assertEquals(1, handler.rejected);
        org.junit.jupiter.api.Assertions.assertEquals(1, handler.cancelled);
    }

    @Test
    void dispatcherRejectsUnboundFulfillmentInsteadOfSilentlySucceeding()
    {
        WorkflowBusinessHandlerRegistry registry = new WorkflowBusinessHandlerRegistry();
        registry.register(new RecordingHandler());
        WorkflowCommandDispatcher dispatcher = new WorkflowCommandDispatcher(registry, validator);

        assertThrows(IllegalStateException.class,
                () -> dispatcher.dispatch(new WorkflowCommand(
                        WorkflowAction.FULFILL, context("pi-1"), "admin", Map.of())));
    }

    private static WorkflowBusinessContext context(String processInstanceId)
    {
        return context(processInstanceId, "key-1");
    }

    private static WorkflowBusinessContext context(String processInstanceId, String idempotencyKey)
    {
        return new WorkflowBusinessContext("expense", "1", "EXP-1", 1L, 2L, processInstanceId, idempotencyKey, Map.of());
    }

    private static final class RecordingHandler implements WorkflowBusinessHandler
    {
        private int approved;
        private int rejected;
        private int cancelled;
        private String taskKey;
        public String businessType() { return "expense"; }
        public void validateBeforeSubmit(WorkflowBusinessContext context) { }
        public void beforeTaskComplete(WorkflowBusinessContext context, String taskKey) { this.taskKey = taskKey; }
        public void afterApprove(WorkflowBusinessContext context) { approved++; }
        public void afterReject(WorkflowBusinessContext context) { rejected++; }
        public void afterCancel(WorkflowBusinessContext context) { cancelled++; }
    }
}
