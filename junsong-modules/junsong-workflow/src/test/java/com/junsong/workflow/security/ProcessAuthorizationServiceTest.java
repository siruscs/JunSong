package com.junsong.workflow.security;

import java.util.Set;

import com.junsong.common.core.exception.ServiceException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAuthorizationServiceTest
{
    private RuntimeService runtimeService;
    private HistoryService historyService;
    private TaskService taskService;
    private ProcessAuthorizationService service;
    private CurrentWorkflowUser actor;

    @BeforeEach
    void setUp()
    {
        runtimeService = mock(RuntimeService.class);
        historyService = mock(HistoryService.class);
        taskService = mock(TaskService.class);
        service = new ProcessAuthorizationService(runtimeService, historyService, taskService);
        actor = new CurrentWorkflowUser(17L, "wjs", Set.of("finance"), Set.of());
    }

    @Test
    void allowsStarterToViewInstance()
    {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        HistoricProcessInstanceQuery historyQuery = mock(HistoricProcessInstanceQuery.class);
        ProcessInstance runtimeInstance = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-1")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(runtimeInstance);
        when(runtimeInstance.getStartUserId()).thenReturn("wjs");
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historyQuery);
        when(historyQuery.processInstanceId("pi-1")).thenReturn(historyQuery);
        when(historyQuery.singleResult()).thenReturn(null);

        service.requireVisibleInstance("pi-1", actor);

        verify(taskService, never()).createTaskQuery();
    }

    @Test
    void allowsAssigneeToViewInstance()
    {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        HistoricProcessInstanceQuery historyQuery = mock(HistoricProcessInstanceQuery.class);
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-1")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(null);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historyQuery);
        when(historyQuery.processInstanceId("pi-1")).thenReturn(historyQuery);
        when(historyQuery.singleResult()).thenReturn(mock(HistoricProcessInstance.class));
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("pi-1")).thenReturn(taskQuery);
        when(taskQuery.taskAssignee("wjs")).thenReturn(taskQuery);
        when(taskQuery.count()).thenReturn(1L);

        service.requireVisibleInstance("pi-1", actor);

        verify(taskQuery).count();
    }

    @Test
    void rejectsInvisibleInstance()
    {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        HistoricProcessInstanceQuery historyQuery = mock(HistoricProcessInstanceQuery.class);
        HistoricProcessInstance historyInstance = mock(HistoricProcessInstance.class);
        TaskQuery taskQuery = mock(TaskQuery.class);
        HistoricTaskInstanceQuery historicTaskQuery = mock(HistoricTaskInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-1")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(null);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historyQuery);
        when(historyQuery.processInstanceId("pi-1")).thenReturn(historyQuery);
        when(historyQuery.singleResult()).thenReturn(historyInstance);
        when(historyInstance.getStartUserId()).thenReturn("lisi");
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("pi-1")).thenReturn(taskQuery);
        when(taskQuery.taskAssignee("wjs")).thenReturn(taskQuery);
        when(taskQuery.count()).thenReturn(0L);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historicTaskQuery);
        when(historicTaskQuery.processInstanceId("pi-1")).thenReturn(historicTaskQuery);
        when(historicTaskQuery.taskAssignee("wjs")).thenReturn(historicTaskQuery);
        when(historicTaskQuery.count()).thenReturn(0L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.requireVisibleInstance("pi-1", actor));

        assertEquals("无权查看该流程实例", exception.getMessage());
    }

    @Test
    void allowsTerminatePermissionBypass()
    {
        CurrentWorkflowUser admin = new CurrentWorkflowUser(1L, "admin", Set.of(), Set.of("workflow:instance:terminate"));
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        ProcessInstance runtimeInstance = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-1")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(runtimeInstance);

        service.requireTerminableInstance("pi-1", admin);
    }

    @Test
    void rejectsTerminateForNonStarterWithoutPermission()
    {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        ProcessInstance runtimeInstance = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-1")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(runtimeInstance);
        when(runtimeInstance.getStartUserId()).thenReturn("lisi");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.requireTerminableInstance("pi-1", actor));

        assertEquals("仅流程发起人可以终止流程实例", exception.getMessage());
    }
}
