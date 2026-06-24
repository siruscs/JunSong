package com.junsong.workflow.security;

import java.util.Objects;

import com.junsong.common.core.exception.ServiceException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

@Service
public class ProcessAuthorizationService
{
    private static final String QUERY_ALL_PERMISSION = "workflow:instance:queryAll";
    private static final String TERMINATE_PERMISSION = "workflow:instance:terminate";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;

    public ProcessAuthorizationService(
            RuntimeService runtimeService,
            HistoryService historyService,
            TaskService taskService)
    {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.taskService = taskService;
    }

    public void requireVisibleInstance(String processInstanceId, CurrentWorkflowUser actor)
    {
        requireActor(actor, "无权查看该流程实例");
        requireProcessInstanceId(processInstanceId);
        if (actor.hasPermission(QUERY_ALL_PERMISSION))
        {
            return;
        }

        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtimeInstance == null && historicInstance == null)
        {
            throw new ServiceException("流程实例不存在: " + processInstanceId);
        }
        if (matchesStarter(actor.username(), runtimeInstance, historicInstance) || isAssignee(actor.username(), processInstanceId))
        {
            return;
        }
        throw new ServiceException("无权查看该流程实例");
    }

    public void requireTerminableInstance(String processInstanceId, CurrentWorkflowUser actor)
    {
        requireActor(actor, "仅流程发起人可以终止流程实例");
        requireProcessInstanceId(processInstanceId);
        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtimeInstance == null)
        {
            throw new ServiceException("流程实例不存在或已结束: " + processInstanceId);
        }
        if (actor.hasPermission(TERMINATE_PERMISSION) || Objects.equals(runtimeInstance.getStartUserId(), actor.username()))
        {
            return;
        }
        throw new ServiceException("仅流程发起人可以终止流程实例");
    }

    private boolean matchesStarter(
            String username,
            ProcessInstance runtimeInstance,
            HistoricProcessInstance historicInstance)
    {
        return runtimeInstance != null && Objects.equals(runtimeInstance.getStartUserId(), username)
                || historicInstance != null && Objects.equals(historicInstance.getStartUserId(), username);
    }

    private boolean isAssignee(String username, String processInstanceId)
    {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(username)
                .count() > 0
                || historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .taskAssignee(username)
                        .count() > 0;
    }

    private void requireProcessInstanceId(String processInstanceId)
    {
        if (processInstanceId == null || processInstanceId.isBlank())
        {
            throw new ServiceException("流程实例不存在: " + processInstanceId);
        }
    }

    private void requireActor(CurrentWorkflowUser actor, String message)
    {
        if (actor == null || actor.username() == null || actor.username().isBlank())
        {
            throw new ServiceException(message);
        }
    }
}
