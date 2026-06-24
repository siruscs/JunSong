package com.junsong.workflow.service.task;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.junsong.workflow.controller.TaskController.ApproveReq;
import com.junsong.workflow.controller.TaskController.RejectReq;
import com.junsong.workflow.lowcode.sync.ConfigurablePostActionHandler;
import com.junsong.workflow.security.CurrentWorkflowUser;
import com.junsong.workflow.security.CurrentWorkflowUserFacade;
import com.junsong.workflow.security.TaskAuthorizationService;
import com.junsong.workflow.service.sync.WorkflowBusinessSyncHandler;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowTaskServiceTest
{
    private TaskService taskService;
    private RuntimeService runtimeService;
    private HistoryService historyService;
    private CurrentWorkflowUserFacade currentWorkflowUserFacade;
    private TaskAuthorizationService taskAuthorizationService;
    private WorkflowBusinessSyncHandler workflowBusinessSyncHandler;
    private ConfigurablePostActionHandler postActionHandler;
    private WorkflowTaskService service;
    private CurrentWorkflowUser actor;

    @BeforeEach
    void setUp()
    {
        taskService = mock(TaskService.class);
        runtimeService = mock(RuntimeService.class);
        historyService = mock(HistoryService.class);
        currentWorkflowUserFacade = mock(CurrentWorkflowUserFacade.class);
        taskAuthorizationService = mock(TaskAuthorizationService.class);
        workflowBusinessSyncHandler = mock(WorkflowBusinessSyncHandler.class);
        postActionHandler = mock(ConfigurablePostActionHandler.class);
        service = new WorkflowTaskService(taskService, runtimeService, historyService, currentWorkflowUserFacade,
                taskAuthorizationService, List.of(workflowBusinessSyncHandler), postActionHandler);
        actor = new CurrentWorkflowUser(17L, "wjs", Set.of("finance"), Set.of());
        when(currentWorkflowUserFacade.current()).thenReturn(actor);
    }

    @Test
    void todoUsesCurrentUserAndMergesAssignedCandidateUserAndCandidateGroupTasks()
    {
        Task assigned = task("task-1", "wjs", "pi-1", new Date(2000L));
        Task directCandidate = task("task-2", null, "pi-2", new Date(3000L));
        Task groupCandidate = task("task-3", null, "pi-3", new Date(1000L));
        Task duplicate = task("task-2", null, "pi-2", new Date(3000L));

        TaskQuery assignedQuery = orderedTaskQuery(List.of(assigned));
        TaskQuery candidateUserQuery = orderedTaskQuery(List.of(directCandidate));
        TaskQuery candidateGroupQuery = orderedTaskQuery(List.of(groupCandidate, duplicate));
        when(taskService.createTaskQuery()).thenReturn(assignedQuery, candidateUserQuery, candidateGroupQuery);
        ProcessInstanceQuery piQuery = emptyProcessInstanceQuery();

        var response = service.todo();

        // LinkedHashMap 保序：先放入 assigned(task-1)，再放入 candidateUser(task-2)，最后 candidateGroup(task-3)
        assertEquals(List.of("task-1", "task-2", "task-3"),
                response.getData().stream().map(row -> row.get("taskId").toString()).toList());
        verify(assignedQuery).taskAssignee("wjs");
        verify(candidateUserQuery).taskCandidateUser("wjs");
        verify(candidateGroupQuery).taskCandidateGroupIn(Set.of("finance"));
        // 验证 N+1 已修复：批量查询仅调用一次
        verify(runtimeService, times(1)).createProcessInstanceQuery();
        verify(piQuery, times(1)).processInstanceIds(anySet());
        verify(piQuery, times(1)).list();
    }

    @Test
    void todoSkipsCandidateGroupQueryWhenActorHasNoRoles()
    {
        CurrentWorkflowUser actorWithoutRoles = new CurrentWorkflowUser(17L, "wjs", Set.of(), Set.of());
        when(currentWorkflowUserFacade.current()).thenReturn(actorWithoutRoles);

        TaskQuery assignedQuery = orderedTaskQuery(List.of());
        TaskQuery candidateUserQuery = orderedTaskQuery(List.of());
        when(taskService.createTaskQuery()).thenReturn(assignedQuery, candidateUserQuery);

        service.todo();

        verify(assignedQuery).taskAssignee("wjs");
        verify(candidateUserQuery).taskCandidateUser("wjs");
        verify(candidateUserQuery, never()).taskCandidateGroupIn(anySet());
    }

    @Test
    void doneFiltersByCurrentUsername()
    {
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class);
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(query);
        when(query.taskAssignee("wjs")).thenReturn(query);
        when(query.finished()).thenReturn(query);
        when(query.orderByHistoricTaskInstanceEndTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(task));
        HistoricProcessInstanceQuery piQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(piQuery);
        when(piQuery.processInstanceIds(anySet())).thenReturn(piQuery);
        when(piQuery.list()).thenReturn(List.of());

        service.done();

        verify(query).taskAssignee("wjs");
        // 验证 N+1 已修复：批量查询历史流程实例仅调用一次
        verify(piQuery, times(1)).processInstanceIds(anySet());
        verify(piQuery, times(1)).list();
    }

    @Test
    void appliedFiltersByCurrentUsername()
    {
        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.startedBy("wjs")).thenReturn(query);
        when(query.orderByProcessInstanceStartTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(instance));

        service.applied();

        verify(query).startedBy("wjs");
    }

    @Test
    void detailRequiresAuthorizedVisibleTaskBeforeLoadingVariables()
    {
        Task task = task("task-1", null, "pi-1", new Date());
        when(taskAuthorizationService.requireActorVisibleTask("task-1", actor)).thenReturn(task);
        when(taskService.getVariables("task-1")).thenReturn(Map.of("days", 3));
        ProcessInstanceQuery piQuery = emptyProcessInstanceQuery();

        var response = service.detail("task-1");

        assertEquals("task-1", response.getData().get("taskId"));
        assertEquals(Map.of("days", 3), response.getData().get("variables"));
        verify(taskAuthorizationService).requireActorVisibleTask("task-1", actor);
        // 验证 N+1 已修复：detail 也使用批量查询（单元素）
        verify(runtimeService, times(1)).createProcessInstanceQuery();
        verify(piQuery, times(1)).processInstanceIds(anySet());
    }

    @Test
    void claimUsesCurrentActorAuthorization()
    {
        Task task = task("task-1", "wjs", "pi-1", new Date());
        when(taskAuthorizationService.requireActorTask("task-1", actor)).thenReturn(task);

        service.claim("task-1");

        verify(taskAuthorizationService).requireActorTask("task-1", actor);
        verify(taskService, never()).claim("task-1", "wjs");
    }

    @Test
    void approveUsesCurrentActorAndAuthorizedTask()
    {
        Task task = task("task-1", "wjs", "pi-1", new Date());
        ApproveReq req = new ApproveReq();
        req.comment = "同意";
        req.variables = Map.of("approved", true);
        when(taskAuthorizationService.requireActorTask("task-1", actor)).thenReturn(task);
        when(taskService.getVariables("task-1")).thenReturn(Map.of("refundAmount", 88));

        service.approve("task-1", req);

        verify(taskAuthorizationService).requireActorTask("task-1", actor);
        verify(taskService).addComment("task-1", "pi-1", "approve", "同意");
        verify(taskService).complete("task-1", req.variables);
    }

    @Test
    void approveStoreOpeningTaskSyncsBusinessStatus()
    {
        Task task = task("task-1", "wjs", "pi-1", new Date());
        when(task.getProcessDefinitionId()).thenReturn("store_opening_apply:2:1");
        when(task.getName()).thenReturn("区域负责人审批");
        when(taskAuthorizationService.requireActorTask("task-1", actor)).thenReturn(task);
        when(workflowBusinessSyncHandler.supports("store_opening_apply:2:1")).thenReturn(true);
        when(taskService.getVariables("task-1")).thenReturn(Map.of("regionLeader", "lisi"));

        service.approve("task-1", new ApproveReq());

        verify(workflowBusinessSyncHandler).afterApprove(eq("区域负责人审批"), eq("pi-1"), eq("wjs"), eq(Map.of("regionLeader", "lisi")));
    }

    @Test
    void approveRefundTaskUsesGenericWorkflowBusinessHandler()
    {
        Task task = task("task-2", "wjs", "pi-r-1", new Date());
        when(task.getProcessDefinitionId()).thenReturn("refund_apply:1:1");
        when(task.getName()).thenReturn("门店负责人审批");
        when(taskAuthorizationService.requireActorTask("task-2", actor)).thenReturn(task);
        when(workflowBusinessSyncHandler.supports("refund_apply:1:1")).thenReturn(true);
        when(taskService.getVariables("task-2")).thenReturn(Map.of("refundAmount", 1888));

        service.approve("task-2", new ApproveReq());

        verify(workflowBusinessSyncHandler).afterApprove(eq("门店负责人审批"), eq("pi-r-1"), eq("wjs"), eq(Map.of("refundAmount", 1888)));
    }

    @Test
    void rejectUsesCurrentActorAndAuthorizedTask()
    {
        Task task = task("task-1", "wjs", "pi-1", new Date());
        RejectReq req = new RejectReq();
        req.comment = "退回";
        when(taskAuthorizationService.requireActorTask("task-1", actor)).thenReturn(task);
        when(task.getProcessDefinitionId()).thenReturn("store_opening_apply:2:1");
        when(workflowBusinessSyncHandler.supports("store_opening_apply:2:1")).thenReturn(true);

        service.reject("task-1", req);

        verify(taskAuthorizationService).requireActorTask("task-1", actor);
        verify(taskService).addComment("task-1", "pi-1", "reject", "退回");
        verify(runtimeService).deleteProcessInstance("pi-1", "驳回: 退回");
        verify(workflowBusinessSyncHandler).afterReject("pi-1", "wjs");
    }

    @Test
    void transferUsesCurrentActorInsteadOfCallerSuppliedUser()
    {
        service.transfer("task-1", " target ");

        verify(taskAuthorizationService).transfer("task-1", actor, " target ");
    }

    @Test
    void requestBodiesNoLongerExposeUserField()
    {
        assertEquals(List.of("comment", "variables"),
                Arrays.stream(ApproveReq.class.getDeclaredFields())
                        .map(field -> field.getName())
                        .sorted()
                        .toList());
        assertEquals(List.of("comment"),
                Arrays.stream(RejectReq.class.getDeclaredFields())
                        .map(field -> field.getName())
                        .toList());
    }

    private TaskQuery orderedTaskQuery(List<Task> tasks)
    {
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskAssignee("wjs")).thenReturn(query);
        when(query.taskCandidateUser("wjs")).thenReturn(query);
        when(query.taskCandidateGroupIn(anySet())).thenReturn(query);
        when(query.taskUnassigned()).thenReturn(query);
        when(query.orderByTaskCreateTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(tasks);
        return query;
    }

    private ProcessInstanceQuery emptyProcessInstanceQuery()
    {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceIds(anySet())).thenReturn(query);
        when(query.list()).thenReturn(List.of());
        return query;
    }

    private Task task(String id, String assignee, String processInstanceId, Date createTime)
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getAssignee()).thenReturn(assignee);
        when(task.getProcessInstanceId()).thenReturn(processInstanceId);
        when(task.getCreateTime()).thenReturn(createTime);
        when(task.getName()).thenReturn(id + "-name");
        return task;
    }
}
