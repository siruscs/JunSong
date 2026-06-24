package com.junsong.workflow.service.instance;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.junsong.workflow.controller.ProcessInstanceController.StartInstanceReq;
import com.junsong.workflow.security.CurrentWorkflowUser;
import com.junsong.workflow.security.CurrentWorkflowUserFacade;
import com.junsong.workflow.security.ProcessAuthorizationService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowInstanceServiceTest
{
    private RuntimeService runtimeService;
    private RepositoryService repositoryService;
    private HistoryService historyService;
    private TaskService taskService;
    private CurrentWorkflowUserFacade currentWorkflowUserFacade;
    private ProcessAuthorizationService processAuthorizationService;
    private WorkflowInstanceService service;
    private CurrentWorkflowUser actor;

    @BeforeEach
    void setUp()
    {
        runtimeService = mock(RuntimeService.class);
        repositoryService = mock(RepositoryService.class);
        historyService = mock(HistoryService.class);
        taskService = mock(TaskService.class);
        currentWorkflowUserFacade = mock(CurrentWorkflowUserFacade.class);
        processAuthorizationService = mock(ProcessAuthorizationService.class);
        service = new WorkflowInstanceService(runtimeService, repositoryService, historyService, taskService,
                currentWorkflowUserFacade, processAuthorizationService);
        actor = new CurrentWorkflowUser(17L, "wjs", Set.of("finance"), Set.of());
        when(currentWorkflowUserFacade.current()).thenReturn(actor);
    }

    @Test
    void startUsesCurrentUserAsInitiator()
    {
        ProcessDefinitionQuery definitionQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        ProcessInstance instance = mock(ProcessInstance.class);
        StartInstanceReq request = new StartInstanceReq();
        request.processKey = "leave_apply";
        request.businessKey = "LEAVE-1";
        request.variables = Map.of("initiator", "fake", "days", 3);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionKey("leave_apply")).thenReturn(definitionQuery);
        when(definitionQuery.latestVersion()).thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(definition);
        when(definition.isSuspended()).thenReturn(false);
        when(definition.getId()).thenReturn("def-1");
        when(definition.getKey()).thenReturn("leave_apply");
        when(definition.getName()).thenReturn("请假");
        when(runtimeService.startProcessInstanceById("def-1", "LEAVE-1",
                Map.of("initiator", "wjs", "days", 3))).thenReturn(instance);

        service.start(request);

        verify(runtimeService).startProcessInstanceById("def-1", "LEAVE-1", Map.of("initiator", "wjs", "days", 3));
    }

    @Test
    void listFiltersByCurrentStarter()
    {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.startedBy("wjs")).thenReturn(query);
        when(query.processDefinitionKey("leave_apply")).thenReturn(query);
        when(query.processInstanceBusinessKey("LEAVE-1")).thenReturn(query);
        when(query.orderByProcessInstanceId()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(instance));

        service.list("leave_apply", "LEAVE-1");

        verify(query).startedBy("wjs");
    }

    @Test
    void detailRequiresVisibilityAuthorization()
    {
        ProcessInstanceQuery runtimeQuery = mock(ProcessInstanceQuery.class);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("pi-1")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(instance);

        service.detail("pi-1");

        verify(processAuthorizationService).requireVisibleInstance("pi-1", actor);
    }

    @Test
    void terminateRequiresAuthorization()
    {
        service.terminate("pi-1", "manual");

        verify(processAuthorizationService).requireTerminableInstance("pi-1", actor);
        verify(runtimeService).deleteProcessInstance("pi-1", "manual");
    }

    @Test
    void runningTasksRequireVisibilityAuthorization()
    {
        TaskQuery query = mock(TaskQuery.class);
        Task task = mock(Task.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.processInstanceId("pi-1")).thenReturn(query);
        when(query.orderByTaskCreateTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(task));

        service.runningTasks("pi-1");

        verify(processAuthorizationService).requireVisibleInstance("pi-1", actor);
    }

    @Test
    void requestBodyNoLongerExposesInitiatorField()
    {
        assertEquals(List.of("businessKey", "processKey", "variables"),
                java.util.Arrays.stream(StartInstanceReq.class.getDeclaredFields())
                        .map(field -> field.getName())
                        .sorted()
                        .toList());
    }
}
