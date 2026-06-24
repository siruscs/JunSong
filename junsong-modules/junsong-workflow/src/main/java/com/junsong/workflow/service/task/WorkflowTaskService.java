package com.junsong.workflow.service.task;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.junsong.common.core.domain.R;
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
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

/**
 * 任务业务服务：待办 / 已办 / 已申请 / 详情 / 签收 / 审批 / 驳回 / 转办
 * 封装 Flowable TaskService / RuntimeService / HistoryService 调用，
 * Controller 层不直接依赖引擎 API。
 *
 * @author junsong
 */
@Service
public class WorkflowTaskService
{
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskAuthorizationService taskAuthorizationService;
    private final CurrentWorkflowUserFacade currentWorkflowUserFacade;
    private final List<WorkflowBusinessSyncHandler> workflowBusinessSyncHandlers;
    private final ConfigurablePostActionHandler postActionHandler;

    public WorkflowTaskService(
            TaskService taskService,
            RuntimeService runtimeService,
            HistoryService historyService,
            CurrentWorkflowUserFacade currentWorkflowUserFacade,
            TaskAuthorizationService taskAuthorizationService,
            List<WorkflowBusinessSyncHandler> workflowBusinessSyncHandlers,
            ConfigurablePostActionHandler postActionHandler)
    {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.currentWorkflowUserFacade = currentWorkflowUserFacade;
        this.taskAuthorizationService = taskAuthorizationService;
        this.workflowBusinessSyncHandlers = workflowBusinessSyncHandlers;
        this.postActionHandler = postActionHandler;
    }

    /**
     * 待办任务（指派给我 / 候选人是我 / 候选组包含我的角色）
     */
    public R<List<Map<String, Object>>> todo()
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        LinkedHashMap<String, Task> uniqueTasks = new LinkedHashMap<>();
        taskService.createTaskQuery()
                .taskAssignee(actor.username())
                .orderByTaskCreateTime()
                .desc()
                .list()
                .forEach(task -> uniqueTasks.put(task.getId(), task));
        taskService.createTaskQuery()
                .taskCandidateUser(actor.username())
                .orderByTaskCreateTime()
                .desc()
                .list()
                .forEach(task -> uniqueTasks.putIfAbsent(task.getId(), task));
        if (!actor.roleKeys().isEmpty())
        {
            taskService.createTaskQuery()
                    .taskCandidateGroupIn(actor.roleKeys())
                    .orderByTaskCreateTime()
                    .desc()
                    .list()
                    .forEach(task -> uniqueTasks.putIfAbsent(task.getId(), task));
        }
        Set<String> processInstanceIds = uniqueTasks.values().stream()
                .map(Task::getProcessInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ProcessInstance> processMap = buildProcessInstanceMap(processInstanceIds);
        return R.ok(uniqueTasks.values().stream().map(task -> toRow(task, processMap)).toList());
    }

    /**
     * 已办任务
     */
    public R<List<Map<String, Object>>> done()
    {
        String username = currentWorkflowUserFacade.current().username();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(username)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        Set<String> processInstanceIds = tasks.stream()
                .map(HistoricTaskInstance::getProcessInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, HistoricProcessInstance> processMap = buildHistoricProcessInstanceMap(processInstanceIds);
        return R.ok(tasks.stream().map(task -> toHistoricRow(task, processMap)).toList());
    }

    /**
     * 我发起的流程
     */
    public R<List<Map<String, Object>>> applied()
    {
        String username = currentWorkflowUserFacade.current().username();
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .startedBy(username)
                .orderByProcessInstanceStartTime()
                .desc()
                .list();
        return R.ok(instances.stream().map(this::toAppliedRow).toList());
    }

    /**
     * 任务详情（含流程变量）
     */
    public R<Map<String, Object>> detail(String taskId)
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorVisibleTask(taskId, actor);
        Set<String> processInstanceIds = task.getProcessInstanceId() == null
                ? Set.of()
                : Set.of(task.getProcessInstanceId());
        Map<String, ProcessInstance> processMap = buildProcessInstanceMap(processInstanceIds);
        Map<String, Object> result = toRow(task, processMap);
        result.put("variables", taskService.getVariables(taskId));
        return R.ok(result);
    }

    /**
     * 签收任务（校验权限）
     */
    public R<Void> claim(String taskId)
    {
        taskAuthorizationService.requireActorTask(taskId, currentWorkflowUserFacade.current());
        return R.ok();
    }

    /**
     * 审批通过
     */
    public R<Void> approve(String taskId, ApproveReq request)
    {
        ApproveReq req = request == null ? new ApproveReq() : request;
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorTask(taskId, actor);
        String processDefinitionId = task.getProcessDefinitionId();
        String currentTaskName = task.getName();
        if (req.comment != null && !req.comment.isBlank())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), "approve", req.comment.trim());
        }
        Map<String, Object> currentVariables = taskService.getVariables(taskId);
        taskService.complete(taskId, req.variables == null ? Map.of() : req.variables);
        findSyncHandler(processDefinitionId)
                .ifPresent(handler -> handler.afterApprove(
                        currentTaskName,
                        task.getProcessInstanceId(),
                        actor.username(),
                        currentVariables == null ? Map.of() : currentVariables));
        return R.ok();
    }

    /**
     * 驳回
     */
    public R<Void> reject(String taskId, RejectReq request)
    {
        RejectReq req = request == null ? new RejectReq() : request;
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorTask(taskId, actor);
        String processDefinitionId = task.getProcessDefinitionId();
        String comment = req.comment == null ? "" : req.comment.trim();
        if (!comment.isBlank())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), "reject", comment);
        }
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "驳回: " + comment);
        findSyncHandler(processDefinitionId)
                .ifPresent(handler -> handler.afterReject(task.getProcessInstanceId(), actor.username()));
        // 触发配置化后置动作
        postActionHandler.onAfterReject(task.getProcessInstanceId(), actor.username());
        return R.ok();
    }

    /**
     * 转办
     */
    public R<Void> transfer(String taskId, String toUser)
    {
        taskAuthorizationService.transfer(taskId, currentWorkflowUserFacade.current(), toUser);
        return R.ok();
    }

    private Map<String, ProcessInstance> buildProcessInstanceMap(Set<String> processInstanceIds)
    {
        if (processInstanceIds.isEmpty())
        {
            return Map.of();
        }
        return runtimeService.createProcessInstanceQuery()
                .processInstanceIds(processInstanceIds)
                .list()
                .stream()
                .collect(Collectors.toMap(ProcessInstance::getId, p -> p));
    }

    private Map<String, HistoricProcessInstance> buildHistoricProcessInstanceMap(Set<String> processInstanceIds)
    {
        if (processInstanceIds.isEmpty())
        {
            return Map.of();
        }
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIds)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, p -> p));
    }

    private Map<String, Object> toRow(Task task, Map<String, ProcessInstance> processMap)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("taskId", task.getId());
        row.put("taskName", task.getName());
        row.put("description", task.getDescription());
        row.put("assignee", task.getAssignee());
        row.put("owner", task.getOwner());
        row.put("createTime", task.getCreateTime());
        row.put("dueDate", task.getDueDate());
        row.put("processInstanceId", task.getProcessInstanceId());
        row.put("processDefinitionId", task.getProcessDefinitionId());
        ProcessInstance process = processMap.get(task.getProcessInstanceId());
        if (process != null)
        {
            row.put("processDefinitionKey", process.getProcessDefinitionKey());
            row.put("businessKey", process.getBusinessKey());
        }
        return row;
    }

    private Map<String, Object> toHistoricRow(HistoricTaskInstance task, Map<String, HistoricProcessInstance> processMap)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("taskId", task.getId());
        row.put("taskName", task.getName());
        row.put("assignee", task.getAssignee());
        row.put("startTime", task.getStartTime());
        row.put("endTime", task.getEndTime());
        row.put("durationMs", task.getDurationInMillis());
        row.put("processInstanceId", task.getProcessInstanceId());
        row.put("processDefinitionId", task.getProcessDefinitionId());
        row.put("deleteReason", task.getDeleteReason());
        HistoricProcessInstance process = processMap.get(task.getProcessInstanceId());
        if (process != null)
        {
            row.put("processDefinitionKey", process.getProcessDefinitionKey());
            row.put("businessKey", process.getBusinessKey());
        }
        return row;
    }

    private Map<String, Object> toAppliedRow(HistoricProcessInstance process)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("processInstanceId", process.getId());
        row.put("processDefinitionKey", process.getProcessDefinitionKey());
        row.put("processDefinitionName", process.getProcessDefinitionName());
        row.put("businessKey", process.getBusinessKey());
        row.put("startTime", process.getStartTime());
        row.put("endTime", process.getEndTime());
        row.put("durationMs", process.getDurationInMillis());
        row.put("running", process.getEndTime() == null);
        return row;
    }

    private Optional<WorkflowBusinessSyncHandler> findSyncHandler(String processDefinitionId)
    {
        return workflowBusinessSyncHandlers.stream()
                .filter(handler -> handler.supports(processDefinitionId))
                .findFirst();
    }
}
