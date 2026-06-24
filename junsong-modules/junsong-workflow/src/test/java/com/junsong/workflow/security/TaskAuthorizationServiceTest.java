package com.junsong.workflow.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.service.identity.PeerUserService;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskAuthorizationServiceTest
{
    private ManagementService managementService;
    private PeerUserService peerUserService;
    private org.flowable.task.service.TaskService internalTaskService;
    private IdentityLinkService identityLinkService;
    private TaskAuthorizationService authorizationService;
    private CurrentWorkflowUser actor;

    @BeforeEach
    void setUp()
    {
        managementService = mock(ManagementService.class);
        peerUserService = mock(PeerUserService.class);
        internalTaskService = mock(org.flowable.task.service.TaskService.class);
        identityLinkService = mock(IdentityLinkService.class);
        authorizationService = new TaskAuthorizationService(managementService, peerUserService);
        actor = new CurrentWorkflowUser(17L, "wjs", Set.of("finance"), Set.of());
        executeCommandsAgainstInternalServices();
    }

    @Test
    void acceptsCurrentAssigneeInsideAtomicCommand()
    {
        TaskEntity task = task("wjs");
        when(internalTaskService.getTask("task-1")).thenReturn(task);

        assertSame(task, authorizationService.requireActorTask("task-1", actor));

        verify(internalTaskService).getTask("task-1");
        verify(internalTaskService, never()).changeTaskAssignee(task, "wjs");
        verify(identityLinkService, never()).findIdentityLinksByTaskId("task-1");
    }

    @Test
    void rejectsAnotherAssigneeInsideAtomicCommand()
    {
        TaskEntity task = task("lisi");
        when(internalTaskService.getTask("task-1")).thenReturn(task);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.requireActorTask("task-1", actor));

        assertEquals("无权处理该任务", exception.getMessage());
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @Test
    void atomicallyAssignsDirectCandidateUser()
    {
        TaskEntity task = task(null);
        IdentityLinkEntity directCandidate = link(IdentityLinkType.CANDIDATE, "wjs", null);
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1")).thenReturn(List.of(directCandidate));

        assertSame(task, authorizationService.requireActorTask("task-1", actor));

        verify(identityLinkService).findIdentityLinksByTaskId("task-1");
        verify(internalTaskService).changeTaskAssignee(task, "wjs");
    }

    @Test
    void visibleTaskAllowsDirectCandidateWithoutAssigning()
    {
        TaskEntity task = task(null);
        IdentityLinkEntity directCandidate = link(IdentityLinkType.CANDIDATE, "wjs", null);
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1")).thenReturn(List.of(directCandidate));

        assertSame(task, authorizationService.requireActorVisibleTask("task-1", actor));

        verify(identityLinkService).findIdentityLinksByTaskId("task-1");
        verify(internalTaskService, never()).changeTaskAssignee(task, "wjs");
    }

    @Test
    void atomicallyAssignsCandidateGroupMember()
    {
        TaskEntity task = task(null);
        IdentityLinkEntity groupCandidate = link(IdentityLinkType.CANDIDATE, null, "finance");
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1")).thenReturn(List.of(groupCandidate));

        assertSame(task, authorizationService.requireActorTask("task-1", actor));

        verify(identityLinkService).findIdentityLinksByTaskId("task-1");
        verify(internalTaskService).changeTaskAssignee(task, "wjs");
    }

    @Test
    void rejectsCandidateGroupWhenActorHasNoRoles()
    {
        CurrentWorkflowUser actorWithoutRoles = new CurrentWorkflowUser(17L, "wjs", Set.of(), Set.of());
        TaskEntity task = task(null);
        IdentityLinkEntity groupCandidate = link(IdentityLinkType.CANDIDATE, null, "finance");
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1")).thenReturn(List.of(groupCandidate));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.requireActorTask("task-1", actorWithoutRoles));

        assertEquals("无权处理该任务", exception.getMessage());
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @Test
    void visibleTaskRejectsUnauthorizedViewerWithoutAssigning()
    {
        TaskEntity task = task(null);
        IdentityLinkEntity otherGroupCandidate = link(IdentityLinkType.CANDIDATE, null, "hr");
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1"))
                .thenReturn(List.of(otherGroupCandidate));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.requireActorVisibleTask("task-1", actor));

        assertEquals("无权查看该任务", exception.getMessage());
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @Test
    void assignsDirectCandidateUserWhenActorHasNoRoles()
    {
        CurrentWorkflowUser actorWithoutRoles = new CurrentWorkflowUser(17L, "wjs", Set.of(), Set.of());
        TaskEntity task = task(null);
        IdentityLinkEntity directCandidate = link(IdentityLinkType.CANDIDATE, "wjs", null);
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1")).thenReturn(List.of(directCandidate));

        assertSame(task, authorizationService.requireActorTask("task-1", actorWithoutRoles));

        verify(internalTaskService).changeTaskAssignee(task, "wjs");
    }

    @Test
    void rejectsNonCandidateIdentityLinks()
    {
        TaskEntity task = task(null);
        List<IdentityLinkEntity> unauthorizedLinks = List.of(
                link(IdentityLinkType.OWNER, "wjs", null),
                link(IdentityLinkType.CANDIDATE, "lisi", null),
                link(IdentityLinkType.CANDIDATE, null, "hr"));
        when(internalTaskService.getTask("task-1")).thenReturn(task);
        when(identityLinkService.findIdentityLinksByTaskId("task-1")).thenReturn(unauthorizedLinks);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.requireActorTask("task-1", actor));

        assertEquals("无权处理该任务", exception.getMessage());
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @Test
    void reportsMissingTaskInsideAtomicCommand()
    {
        when(internalTaskService.getTask("missing")).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.requireTask("missing"));

        assertEquals("任务不存在或已结束: missing", exception.getMessage());
        verify(internalTaskService).getTask("missing");
    }

    @Test
    void rejectsBlankAndNullTaskIdsWithoutExecutingACommand()
    {
        ServiceException blank = assertThrows(ServiceException.class,
                () -> authorizationService.requireTask("  "));
        ServiceException missing = assertThrows(ServiceException.class,
                () -> authorizationService.requireTask(null));

        assertEquals("任务不存在或已结束:   ", blank.getMessage());
        assertEquals("任务不存在或已结束: null", missing.getMessage());
        verify(managementService, never()).executeCommand(any(Command.class));
    }

    @Test
    void validatesTargetThenAtomicallyTransfersFromExpectedActor()
    {
        TaskEntity task = task("wjs");
        when(peerUserService.requireUsername(" target ")).thenReturn("TargetUser");
        when(internalTaskService.getTask("task-1")).thenReturn(task);

        authorizationService.transfer("task-1", actor, " target ");

        var ordered = inOrder(peerUserService, managementService);
        ordered.verify(peerUserService).requireUsername(" target ");
        ordered.verify(managementService).executeCommand(any(Command.class));
        verify(internalTaskService).getTask("task-1");
        verify(internalTaskService).changeTaskAssignee(task, "TargetUser");
    }

    @Test
    void rejectsTransferWhenActorIsNotCurrentAssigneeWithoutChangingAssignee()
    {
        TaskEntity task = task("lisi");
        when(peerUserService.requireUsername("target")).thenReturn("target");
        when(internalTaskService.getTask("task-1")).thenReturn(task);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.transfer("task-1", actor, "target"));

        assertEquals("仅当前受理人可以转办任务", exception.getMessage());
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @Test
    void propagatesInvalidTargetBeforeExecutingTransferCommand()
    {
        ServiceException targetError = new ServiceException("用户已停用或删除: target");
        when(peerUserService.requireUsername("target")).thenThrow(targetError);

        ServiceException thrown = assertThrows(ServiceException.class,
                () -> authorizationService.transfer("task-1", actor, "target"));

        assertSame(targetError, thrown);
        verify(managementService, never()).executeCommand(any(Command.class));
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @Test
    void translatesOptimisticLockingFailureFromAuthorizationCommand()
    {
        when(managementService.executeCommand(any(Command.class)))
                .thenThrow(new FlowableOptimisticLockingException("revision conflict"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.requireActorTask("task-1", actor));

        assertEquals("任务状态已变化，请刷新后重试", exception.getMessage());
    }

    @Test
    void translatesOptimisticLockingFailureFromTransferCommand()
    {
        when(peerUserService.requireUsername("target")).thenReturn("TargetUser");
        when(managementService.executeCommand(any(Command.class)))
                .thenThrow(new FlowableOptimisticLockingException("revision conflict"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authorizationService.transfer("task-1", actor, "target"));

        assertEquals("任务状态已变化，请刷新后重试", exception.getMessage());
        verify(internalTaskService, never()).changeTaskAssignee(any(), any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void executeCommandsAgainstInternalServices()
    {
        ProcessEngineConfigurationImpl engineConfiguration = mock(ProcessEngineConfigurationImpl.class);
        TaskServiceConfiguration taskConfiguration = mock(TaskServiceConfiguration.class);
        IdentityLinkServiceConfiguration identityLinkConfiguration = mock(IdentityLinkServiceConfiguration.class);
        CommandContext commandContext = mock(CommandContext.class);
        when(commandContext.getEngineConfigurations()).thenReturn(Map.of("cfg.processEngine", engineConfiguration));
        when(engineConfiguration.getTaskServiceConfiguration()).thenReturn(taskConfiguration);
        when(engineConfiguration.getIdentityLinkServiceConfiguration()).thenReturn(identityLinkConfiguration);
        when(taskConfiguration.getTaskService()).thenReturn(internalTaskService);
        when(identityLinkConfiguration.getIdentityLinkService()).thenReturn(identityLinkService);
        doAnswer(invocation -> ((Command) invocation.getArgument(0)).execute(commandContext))
                .when(managementService).executeCommand(any(Command.class));
    }

    private TaskEntity task(String assignee)
    {
        TaskEntity task = mock(TaskEntity.class);
        when(task.getAssignee()).thenReturn(assignee);
        return task;
    }

    private IdentityLinkEntity link(String type, String userId, String groupId)
    {
        IdentityLinkEntity link = mock(IdentityLinkEntity.class);
        when(link.getType()).thenReturn(type);
        when(link.getUserId()).thenReturn(userId);
        when(link.getGroupId()).thenReturn(groupId);
        return link;
    }
}
