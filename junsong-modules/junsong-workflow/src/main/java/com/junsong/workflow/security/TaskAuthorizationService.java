package com.junsong.workflow.security;

import java.util.Objects;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.service.identity.PeerUserService;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.stereotype.Service;

@Service
public class TaskAuthorizationService
{
    private static final String TASK_CHANGED_MESSAGE = "任务状态已变化，请刷新后重试";

    private final ManagementService managementService;
    private final PeerUserService peerUserService;

    public TaskAuthorizationService(ManagementService managementService, PeerUserService peerUserService)
    {
        this.managementService = managementService;
        this.peerUserService = peerUserService;
    }

    public Task requireTask(String taskId)
    {
        requireTaskId(taskId);
        return executeAtomic(commandContext -> requireInternalTask(commandContext, taskId));
    }

    public Task requireActorTask(String taskId, CurrentWorkflowUser actor)
    {
        requireTaskId(taskId);
        if (actor == null || actor.username() == null || actor.username().isBlank())
        {
            throw new ServiceException("无权处理该任务");
        }
        return executeAtomic(commandContext -> requireActorInternalTask(commandContext, taskId, actor));
    }

    public Task requireActorVisibleTask(String taskId, CurrentWorkflowUser actor)
    {
        requireTaskId(taskId);
        if (actor == null || actor.username() == null || actor.username().isBlank())
        {
            throw new ServiceException("无权查看该任务");
        }
        return executeAtomic(commandContext -> requireVisibleInternalTask(commandContext, taskId, actor));
    }

    public void transfer(String taskId, CurrentWorkflowUser actor, String targetUsername)
    {
        requireTaskId(taskId);
        if (actor == null || actor.username() == null || actor.username().isBlank())
        {
            throw new ServiceException("仅当前受理人可以转办任务");
        }
        String canonicalUsername = peerUserService.requireUsername(targetUsername);
        executeAtomic(commandContext -> {
            TaskEntity task = requireInternalTask(commandContext, taskId);
            if (!Objects.equals(task.getAssignee(), actor.username()))
            {
                throw new ServiceException("仅当前受理人可以转办任务");
            }
            CommandContextUtil.getTaskService(commandContext).changeTaskAssignee(task, canonicalUsername);
            return null;
        });
    }

    /**
     * Flowable 8.0.0 is pinned for this module. Internal command services are deliberately isolated here so
     * authorization and assignee mutation share one engine transaction and the task entity revision protects races.
     */
    private TaskEntity requireActorInternalTask(
            CommandContext commandContext,
            String taskId,
            CurrentWorkflowUser actor)
    {
        TaskEntity task = requireInternalTask(commandContext, taskId);
        if (task.getAssignee() != null)
        {
            if (!Objects.equals(task.getAssignee(), actor.username()))
            {
                throw new ServiceException("无权处理该任务");
            }
            return task;
        }

        boolean candidate = CommandContextUtil.getIdentityLinkService(commandContext)
                .findIdentityLinksByTaskId(taskId)
                .stream()
                .filter(link -> IdentityLinkType.CANDIDATE.equals(link.getType()))
                .anyMatch(link -> isCandidate(link, actor));
        if (!candidate)
        {
            throw new ServiceException("无权处理该任务");
        }
        CommandContextUtil.getTaskService(commandContext).changeTaskAssignee(task, actor.username());
        return task;
    }

    private TaskEntity requireVisibleInternalTask(
            CommandContext commandContext,
            String taskId,
            CurrentWorkflowUser actor)
    {
        TaskEntity task = requireInternalTask(commandContext, taskId);
        if (task.getAssignee() != null)
        {
            if (!Objects.equals(task.getAssignee(), actor.username()))
            {
                throw new ServiceException("无权查看该任务");
            }
            return task;
        }

        boolean candidate = CommandContextUtil.getIdentityLinkService(commandContext)
                .findIdentityLinksByTaskId(taskId)
                .stream()
                .filter(link -> IdentityLinkType.CANDIDATE.equals(link.getType()))
                .anyMatch(link -> isCandidate(link, actor));
        if (!candidate)
        {
            throw new ServiceException("无权查看该任务");
        }
        return task;
    }

    private boolean isCandidate(IdentityLinkEntity link, CurrentWorkflowUser actor)
    {
        return Objects.equals(link.getUserId(), actor.username())
                || link.getGroupId() != null && actor.roleKeys().contains(link.getGroupId());
    }

    private TaskEntity requireInternalTask(
            CommandContext commandContext,
            String taskId)
    {
        TaskEntity task = CommandContextUtil.getTaskService(commandContext).getTask(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已结束: " + taskId);
        }
        return task;
    }

    private void requireTaskId(String taskId)
    {
        if (taskId == null || taskId.isBlank())
        {
            throw new ServiceException("任务不存在或已结束: " + taskId);
        }
    }

    private <T> T executeAtomic(Command<T> command)
    {
        try
        {
            return managementService.executeCommand(command);
        }
        catch (FlowableOptimisticLockingException exception)
        {
            throw new ServiceException(TASK_CHANGED_MESSAGE);
        }
    }
}
