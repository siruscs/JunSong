package com.junsong.workflow.service.task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.junsong.common.core.domain.R;
import com.junsong.workflow.controller.TaskController.ApproveReq;
import com.junsong.workflow.controller.TaskController.RejectReq;
import com.junsong.workflow.controller.TaskController.UrgeReq;
import com.junsong.workflow.controller.TaskController.CcReq;
import com.junsong.workflow.controller.TaskController.AddSignReq;
import com.junsong.workflow.domain.WfTaskAddSign;
import com.junsong.workflow.domain.WfTaskAttachment;
import com.junsong.workflow.lowcode.sync.ConfigurablePostActionHandler;
import com.junsong.workflow.mapper.WfNotificationMapper;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.mapper.LcBizInstanceMapper;
import com.junsong.workflow.mapper.WfSysUserDelegateMapper;
import com.junsong.workflow.mapper.WfSysUserMapper;
import com.junsong.workflow.mapper.WfTaskAddSignMapper;
import com.junsong.workflow.mapper.WfTaskAttachmentMapper;
import com.junsong.workflow.security.CurrentWorkflowUser;
import com.junsong.workflow.security.CurrentWorkflowUserFacade;
import com.junsong.workflow.security.TaskAuthorizationService;
import com.junsong.workflow.service.sync.WorkflowBusinessSyncHandler;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RepositoryService repositoryService;
    private final TaskAuthorizationService taskAuthorizationService;
    private final CurrentWorkflowUserFacade currentWorkflowUserFacade;
    private final List<WorkflowBusinessSyncHandler> workflowBusinessSyncHandlers;
    private final ConfigurablePostActionHandler postActionHandler;
    private final WfNotificationMapper notificationMapper;
    private final WfSysUserMapper sysUserMapper;
    private final WfSysUserDelegateMapper sysUserDelegateMapper;
    private final WfTaskAttachmentMapper taskAttachmentMapper;
    private final WfTaskAddSignMapper taskAddSignMapper;
    private final LcBizInstanceMapper lcBizInstanceMapper;

    public WorkflowTaskService(
            TaskService taskService,
            RuntimeService runtimeService,
            HistoryService historyService,
            RepositoryService repositoryService,
            CurrentWorkflowUserFacade currentWorkflowUserFacade,
            TaskAuthorizationService taskAuthorizationService,
            List<WorkflowBusinessSyncHandler> workflowBusinessSyncHandlers,
            ConfigurablePostActionHandler postActionHandler,
            WfNotificationMapper notificationMapper,
            WfSysUserMapper sysUserMapper,
            WfSysUserDelegateMapper sysUserDelegateMapper,
            WfTaskAttachmentMapper taskAttachmentMapper,
            WfTaskAddSignMapper taskAddSignMapper,
            LcBizInstanceMapper lcBizInstanceMapper)
    {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.repositoryService = repositoryService;
        this.currentWorkflowUserFacade = currentWorkflowUserFacade;
        this.taskAuthorizationService = taskAuthorizationService;
        this.workflowBusinessSyncHandlers = workflowBusinessSyncHandlers;
        this.postActionHandler = postActionHandler;
        this.notificationMapper = notificationMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserDelegateMapper = sysUserDelegateMapper;
        this.taskAttachmentMapper = taskAttachmentMapper;
        this.taskAddSignMapper = taskAddSignMapper;
        this.lcBizInstanceMapper = lcBizInstanceMapper;
    }

    /**
     * 待办任务（指派给我 / 候选人是我 / 候选组包含我的角色 / 委托给我的）
     */
    public R<List<Map<String, Object>>> todo()
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        LinkedHashMap<String, Task> uniqueTasks = new LinkedHashMap<>();

        // 1. 自己的任务
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

        // 2. 委托给当前用户的任务
        Set<String> delegatedTaskIds = new HashSet<>();
        if (sysUserDelegateMapper != null)
        {
            List<WfSysUserDelegateMapper.WfDelegateRecord> delegates = sysUserDelegateMapper.selectActiveByDelegateUserId(actor.userId());
            for (WfSysUserDelegateMapper.WfDelegateRecord d : delegates)
            {
                String delegatorName = sysUserMapper.selectUserNameByUserId(d.userId);
                if (delegatorName != null)
                {
                    List<Task> delegatedTasks = taskService.createTaskQuery()
                            .taskAssignee(delegatorName)
                            .orderByTaskCreateTime()
                            .desc()
                            .list();
                    for (Task t : delegatedTasks)
                    {
                        if ("all".equals(d.delegateType) || isProcessKeyMatch(d, t.getProcessDefinitionId()))
                        {
                            uniqueTasks.putIfAbsent(t.getId(), t);
                            delegatedTaskIds.add(t.getId());
                        }
                    }
                }
            }
        }

        Set<String> processInstanceIds = uniqueTasks.values().stream()
                .map(Task::getProcessInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ProcessInstance> processMap = buildProcessInstanceMap(processInstanceIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : uniqueTasks.values())
        {
            Map<String, Object> row = toRow(task, processMap);
            if (delegatedTaskIds.contains(task.getId()))
            {
                row.put("delegated", true);
                row.put("delegatorName", task.getAssignee());
            }
            else
            {
                row.put("delegated", false);
            }
            result.add(row);
        }
        return R.ok(result);
    }

    private boolean isProcessKeyMatch(WfSysUserDelegateMapper.WfDelegateRecord delegate, String processDefinitionId)
    {
        if (delegate.processKeys == null || delegate.processKeys.isEmpty() || processDefinitionId == null)
        {
            return false;
        }
        String processKey = processDefinitionId.split(":")[0];
        for (String key : delegate.processKeys.split(","))
        {
            if (key.trim().equals(processKey))
            {
                return true;
            }
        }
        return false;
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
     * 任务详情（含流程变量、附件）
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
        Map<String, Object> formVariables = taskService.getVariables(taskId);
        Object rawBusinessForm = formVariables == null ? null
                : formVariables.getOrDefault("formData", formVariables.getOrDefault("businessForm", Map.of()));
        Map<String, Object> businessForm = normalizeBusinessForm(rawBusinessForm);
        if (businessForm.isEmpty() && task.getProcessInstanceId() != null)
        {
            LcBizInstance business = lcBizInstanceMapper.selectByProcessInstanceId(task.getProcessInstanceId());
            if (business != null) businessForm = business.getFormDataMap();
        }
        result.put("businessForm", businessForm);
        if (taskAttachmentMapper != null)
        {
            result.put("attachments", taskAttachmentMapper.selectByTaskId(taskId));
        }

        // 会签进度：查询同一节点下的所有任务实例
        String processInstanceId = task.getProcessInstanceId();
        String taskDefKey = task.getTaskDefinitionKey();
        if (processInstanceId != null && taskDefKey != null)
        {
            List<Task> allInstanceTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(taskDefKey)
                    .list();
            if (allInstanceTasks != null && allInstanceTasks.size() > 1)
            {
                // 是多实例任务
                int total = allInstanceTasks.size();
                int completed = 0;
                List<Map<String, Object>> instanceList = new ArrayList<>();
                for (Task t : allInstanceTasks)
                {
                    Map<String, Object> inst = new LinkedHashMap<>();
                    inst.put("taskId", t.getId());
                    inst.put("assignee", t.getAssignee());
                    inst.put("assigneeName", t.getAssignee());
                    inst.put("completed", false);
                    instanceList.add(inst);
                }
                // 同时查询历史已完成的多实例任务
                List<HistoricTaskInstance> completedTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .taskDefinitionKey(taskDefKey)
                        .finished()
                        .list();
                if (completedTasks != null)
                {
                    completed = completedTasks.size();
                    for (HistoricTaskInstance ht : completedTasks)
                    {
                        Map<String, Object> inst = new LinkedHashMap<>();
                        inst.put("taskId", ht.getId());
                        inst.put("assignee", ht.getAssignee());
                        inst.put("assigneeName", ht.getAssignee());
                        inst.put("completed", true);
                        instanceList.add(inst);
                    }
                }

                Map<String, Object> multiInstance = new LinkedHashMap<>();
                multiInstance.put("total", total + completed);
                multiInstance.put("active", total);
                multiInstance.put("completed", completed);
                multiInstance.put("instances", instanceList);
                result.put("multiInstance", multiInstance);
            }
        }
        return R.ok(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeBusinessForm(Object rawBusinessForm)
    {
        if (rawBusinessForm instanceof Map<?, ?> map)
        {
            return (Map<String, Object>) map;
        }
        if (rawBusinessForm instanceof String text && !text.isBlank())
        {
            try
            {
                Map<String, Object> parsed = JSON.parseObject(text, Map.class);
                return parsed == null ? Map.of() : parsed;
            }
            catch (RuntimeException ignored)
            {
                return Map.of();
            }
        }
        return Map.of();
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
     * 批量审批通过
     */
    public R<Map<String, Object>> batchApprove(List<String> taskIds, String comment)
    {
        int success = 0;
        int fail = 0;
        List<String> failMessages = new java.util.ArrayList<>();
        for (String taskId : taskIds)
        {
            try
            {
                ApproveReq req = new ApproveReq();
                req.comment = comment;
                approve(taskId, req);
                success++;
            }
            catch (Exception e)
            {
                fail++;
                failMessages.add(taskId + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        if (!failMessages.isEmpty())
        {
            result.put("errors", failMessages);
        }
        return R.ok(result);
    }

    /**
     * 审批通过
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> approve(String taskId, ApproveReq request)
    {
        ApproveReq req = request == null ? new ApproveReq() : request;
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorTask(taskId, actor);
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        String currentTaskName = task.getName();
        if (req.comment != null && !req.comment.isBlank())
        {
            taskService.addComment(taskId, processInstanceId, "approve", req.comment.trim());
        }
        Map<String, Object> currentVariables = taskService.getVariables(taskId);
        Map<String, Object> completeVariables = req.variables == null
                ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(req.variables);
        // 兼容已部署的旧盘点流程定义：完成任务前注入默认值，避免旧网关因变量缺失失败。
        if (processDefinitionId != null && processDefinitionId.startsWith("stocktake_apply:"))
        {
            Object existingNeedRecount = currentVariables == null ? null : currentVariables.get("needRecount");
            completeVariables.putIfAbsent("needRecount",
                    existingNeedRecount == null ? Boolean.FALSE : existingNeedRecount);
            Object existingApprover = currentVariables == null ? null : currentVariables.get("approverUsername");
            completeVariables.putIfAbsent("approverUsername",
                    existingApprover == null || existingApprover.toString().isBlank() ? "admin" : existingApprover);
        }
        taskService.complete(taskId, completeVariables);
        findSyncHandler(processDefinitionId)
                .ifPresent(handler -> handler.afterApprove(
                        currentTaskName,
                        processInstanceId,
                        actor.username(),
                        currentVariables == null ? Map.of() : currentVariables));

        // 保存附件
        saveAttachments(taskId, processInstanceId, req.attachments, actor.username(), "approve");

        // 检查是否还有后续任务
        List<Task> nextTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        if (nextTasks.isEmpty())
        {
            findSyncHandler(processDefinitionId)
                    .ifPresent(handler -> handler.afterComplete(processInstanceId, actor.username(), currentVariables == null ? Map.of() : currentVariables));
            // 流程已结束，通知发起人
            notifyInitiator(processInstanceId, "流程已办结",
                    "您的流程已通过所有审批节点，已办结。", "wf_finished", "/workflow/instance");
        }
        else
        {
            // 通知下一个任务负责人
            for (Task nextTask : nextTasks)
            {
                if (nextTask.getAssignee() != null && !nextTask.getAssignee().isBlank())
                {
                    Long assigneeUserId = sysUserMapper.selectUserIdByUserName(nextTask.getAssignee());
                    if (assigneeUserId != null)
                    {
                        notificationMapper.insertNotification(
                                assigneeUserId,
                                "新的流程待办任务",
                                "您有一个新的流程待办任务【" + nextTask.getName() + "】需要处理",
                                "wf_todo",
                                "/workflow/task",
                                nextTask.getId());
                    }
                }
            }
        }
        return R.ok();
    }

    /**
     * 驳回（支持回退到指定节点）
     */
    public R<Void> reject(String taskId, RejectReq request)
    {
        RejectReq req = request == null ? new RejectReq() : request;
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorTask(taskId, actor);
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        String comment = req.comment == null ? "" : req.comment.trim();
        if (comment.isBlank())
        {
            return R.fail("驳回原因不能为空");
        }
        if ("INITIATOR_MODIFY".equalsIgnoreCase(req.targetType))
        {
            taskService.addComment(taskId, processInstanceId, "reject", comment);
            return rejectToInitiatorModify(processInstanceId, processDefinitionId, task, comment, actor, req);
        }

        // 未指定目标时默认退回最近一个已完成节点；只有没有可回退节点时才终止流程。
        String targetActivityId = req.targetActivityId;
        if ("FULL_RESTART".equalsIgnoreCase(req.resubmitMode))
        {
            List<HistoricTaskInstance> allFinished = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId).finished()
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            for (HistoricTaskInstance item : allFinished)
            {
                if (!task.getTaskDefinitionKey().equals(item.getTaskDefinitionKey()))
                {
                    targetActivityId = item.getTaskDefinitionKey();
                    break;
                }
            }
        }
        if (targetActivityId == null || targetActivityId.isBlank())
        {
            var previousQuery = historyService.createHistoricTaskInstanceQuery();
            List<HistoricTaskInstance> previous = previousQuery == null ? List.of() : previousQuery
                    .processInstanceId(processInstanceId).finished().orderByHistoricTaskInstanceEndTime().desc().list();
            for (HistoricTaskInstance item : previous)
            {
                if (!task.getTaskDefinitionKey().equals(item.getTaskDefinitionKey()))
                {
                    targetActivityId = item.getTaskDefinitionKey();
                    break;
                }
            }
        }
        if (targetActivityId != null && !targetActivityId.isBlank())
        {
            if (!isAllowedRejectTarget(processInstanceId, targetActivityId, task.getTaskDefinitionKey()))
            {
                return R.fail("驳回目标节点无效");
            }
            taskService.addComment(taskId, processInstanceId, "reject", comment);
            persistRejectMetadata(processInstanceId, comment, req.resubmitMode, actor.username());
            return rejectTo(processInstanceId, processDefinitionId, task, targetActivityId, comment, actor, req);
        }

        return R.fail("当前节点没有可回退的历史节点，不能直接驳回结束；请驳回给发起人修改或选择合法目标节点");
    }

    private boolean isAllowedRejectTarget(String processInstanceId, String targetActivityId, String currentActivityId)
    {
        if (targetActivityId == null || targetActivityId.isBlank() || targetActivityId.equals(currentActivityId)) return false;
        List<HistoricTaskInstance> history = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId).finished().list();
        return history.stream().anyMatch(item -> targetActivityId.equals(item.getTaskDefinitionKey()));
    }

    private void persistRejectMetadata(String processInstanceId, String reason, String mode, String operator)
    {
        LcBizInstance instance = lcBizInstanceMapper.selectByProcessInstanceId(processInstanceId);
        if (instance == null) return;
        instance.setLastRejectReason(reason);
        instance.setLastRejectMode(mode == null || mode.isBlank() ? "RETURN_TO_NODE" : mode);
        instance.setUpdateBy(operator);
        lcBizInstanceMapper.updateLcBizInstance(instance);
    }

    private R<Void> rejectToInitiatorModify(String processInstanceId, String processDefinitionId, Task currentTask,
                                            String comment, CurrentWorkflowUser actor, RejectReq req)
    {
        saveAttachments(currentTask.getId(), processInstanceId, req.attachments, actor.username(), "reject");

        LcBizInstance instance = lcBizInstanceMapper.selectByProcessInstanceId(processInstanceId);
        if (instance != null)
        {
            instance.setLastRejectReason(comment);
            instance.setLastRejectMode(req.resubmitMode == null || req.resubmitMode.isBlank() ? "FULL_RESTART" : req.resubmitMode);
            instance.setWorkflowStatus("REJECTED");
            instance.setCurrentTaskName("发起人修改");
            instance.setUpdateBy(actor.username());
            lcBizInstanceMapper.updateLcBizInstance(instance);
        }

        runtimeService.deleteProcessInstance(processInstanceId, "驳回给发起人修改: " + comment);
        findSyncHandler(processDefinitionId)
                .ifPresent(handler -> handler.afterReject(processInstanceId, actor.username()));
        postActionHandler.onAfterReject(processInstanceId, actor.username());
        notifyInitiator(processInstanceId, "流程被驳回，请修改后重新提交",
                "您的流程被驳回" + (comment.isBlank() ? "" : "，原因：" + comment) + "，请修改后重新提交。",
                "wf_rejected", "/workflow/instance");
        return R.ok();
    }

    private R<Void> rejectTo(String processInstanceId, String processDefinitionId, Task currentTask,
                             String targetActivityId, String comment, CurrentWorkflowUser actor, RejectReq req)
    {
        // 保存附件
        saveAttachments(currentTask.getId(), processInstanceId, req.attachments, actor.username(), "reject");

        String currentActivityId = currentTask.getTaskDefinitionKey();

        // 查找目标历史任务的 assignee（用于回退后重新分配）
        String targetAssignee = findTargetAssignee(processInstanceId, targetActivityId);

        // 使用 Flowable ChangeActivityStateBuilder 回退节点
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentActivityId, targetActivityId)
                .changeState();

        // 回退后设置任务 assignee（如果历史中有记录）
        List<Task> newTasks = new ArrayList<>();
        if (targetAssignee != null && !targetAssignee.isBlank())
        {
            newTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(targetActivityId)
                    .list();
            for (Task t : newTasks)
            {
                if (t.getAssignee() == null || !t.getAssignee().equals(targetAssignee))
                {
                    taskService.setAssignee(t.getId(), targetAssignee);
                }
            }
        }

        // 通知目标节点负责人
        if (targetAssignee != null && !targetAssignee.isBlank())
        {
            Long assigneeId = sysUserMapper.selectUserIdByUserName(targetAssignee);
            if (assigneeId != null)
            {
                notificationMapper.insertNotification(
                        assigneeId,
                        "流程被驳回，需要重新处理",
                        "您的流程任务被驳回" + (comment.isBlank() ? "" : "，原因：" + comment) + "，请重新处理。",
                        "wf_todo",
                        "/workflow/task",
                        newTasks.isEmpty() ? currentTask.getId() : newTasks.get(0).getId());
            }
        }

        // 通知发起人
        notifyInitiator(processInstanceId, "流程被驳回",
                "您的流程被驳回至上一节点" + (comment.isBlank() ? "" : "，原因：" + comment) + "。", "wf_rejected", "/workflow/instance");

        return R.ok();
    }

    private String findTargetAssignee(String processInstanceId, String targetActivityId)
    {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(targetActivityId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        if (!list.isEmpty())
        {
            return list.get(0).getAssignee();
        }
        return null;
    }

    /**
     * 查询可驳回目标节点
     */
    public R<List<Map<String, Object>>> rejectTargets(String taskId)
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorVisibleTask(taskId, actor);
        String processInstanceId = task.getProcessInstanceId();
        String currentTaskDefKey = task.getTaskDefinitionKey();

        List<Map<String, Object>> targets = new java.util.ArrayList<>();

        // 1. 上一步：最近已完成的前一个任务
        List<HistoricTaskInstance> finishedTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();

        for (HistoricTaskInstance ht : finishedTasks)
        {
            if (!ht.getTaskDefinitionKey().equals(currentTaskDefKey))
            {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("activityId", ht.getTaskDefinitionKey());
                item.put("activityName", ht.getName());
                item.put("type", "previous");
                item.put("typeLabel", "上一步");
                item.put("assignee", ht.getAssignee());
                item.put("endTime", ht.getEndTime());
                targets.add(item);
                break;
            }
        }

        // 2. 发起人：流程中最早的用户任务
        List<HistoricTaskInstance> allTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
        if (!allTasks.isEmpty())
        {
            HistoricTaskInstance first = allTasks.get(0);
            if (!first.getTaskDefinitionKey().equals(currentTaskDefKey))
            {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("activityId", first.getTaskDefinitionKey());
                item.put("activityName", first.getName());
                item.put("type", "initiator");
                item.put("typeLabel", "发起人");
                item.put("assignee", first.getAssignee());
                item.put("startTime", first.getStartTime());
                targets.add(item);
            }
        }

        return R.ok(targets);
    }

    private void saveAttachments(String taskId, String processInstanceId, List<Map<String, String>> attachments, String uploadUser, String actionType)
    {
        if (taskAttachmentMapper == null || attachments == null || attachments.isEmpty())
        {
            return;
        }
        for (Map<String, String> att : attachments)
        {
            if (att == null) continue;
            String fileName = att.get("fileName");
            String fileUrl = att.get("fileUrl");
            if (fileName == null || fileName.isBlank() || fileUrl == null || fileUrl.isBlank())
            {
                continue;
            }
            WfTaskAttachment record = new WfTaskAttachment();
            record.setTaskId(taskId);
            record.setProcessInstanceId(processInstanceId);
            record.setFileName(fileName);
            record.setFileUrl(fileUrl);
            String sizeStr = att.get("fileSize");
            if (sizeStr != null && !sizeStr.isBlank())
            {
                try { record.setFileSize(Long.parseLong(sizeStr)); } catch (NumberFormatException ignored) { }
            }
            record.setUploadUser(uploadUser);
            record.setActionType(actionType);
            taskAttachmentMapper.insert(record);
        }
    }

    private void notifyInitiator(String processInstanceId, String title, String content, String type, String linkUrl)
    {
        try
        {
            HistoricVariableInstance var = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName("initiator")
                    .singleResult();
            if (var != null && var.getValue() != null)
            {
                Long initiatorId = sysUserMapper.selectUserIdByUserName(var.getValue().toString());
                if (initiatorId != null)
                {
                    notificationMapper.insertNotification(
                            initiatorId, title, content, type, linkUrl, processInstanceId);
                }
            }
        }
        catch (Exception e)
        {
            // ignore notification errors
        }
    }

    /**
     * 转办
     */
    public R<Void> transfer(String taskId, String toUser)
    {
        taskAuthorizationService.transfer(taskId, currentWorkflowUserFacade.current(), toUser);
        return R.ok();
    }

    /**
     * 委派任务给指定用户（原受理人成为 owner，任务交由被委派人处理后再 resolve 回原受理人）
     */
    public R<Void> delegate(String taskId, String toUser)
    {
        if (toUser == null || toUser.isBlank())
        {
            return R.fail("请选择委派对象");
        }
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
        {
            return R.fail("任务不存在");
        }
        // 校验当前用户是任务受理人
        if (!actor.username().equals(task.getAssignee()))
        {
            return R.fail("只有任务受理人可以委派任务");
        }
        String originalAssignee = task.getAssignee();
        taskService.delegateTask(taskId, toUser);
        taskService.addComment(taskId, task.getProcessInstanceId(), "delegate",
                originalAssignee + " 委派给 " + toUser);
        return R.ok();
    }

    /**
     * 完成委派处理，任务回到原受理人（owner）
     */
    public R<Void> resolveTask(String taskId)
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
        {
            return R.fail("任务不存在");
        }
        // 校验当前用户是被委派人（当前 assignee）
        if (!actor.username().equals(task.getAssignee()))
        {
            return R.fail("只有被委派人可以完成委派处理");
        }
        String originalAssignee = task.getOwner();
        if (originalAssignee == null || originalAssignee.isBlank())
        {
            return R.fail("该任务未被委派，无法完成委派处理");
        }
        String delegatedUser = task.getAssignee();
        taskService.resolveTask(taskId);
        taskService.addComment(taskId, task.getProcessInstanceId(), "resolve",
                delegatedUser + " 完成委派处理，任务已回到 " + originalAssignee);
        return R.ok();
    }

    /**
     * 催办
     */
    public R<Void> urge(String taskId, UrgeReq request)
    {
        String comment = request != null && request.comment != null ? request.comment.trim() : "";
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
        {
            return R.fail("任务不存在");
        }
        // 检查权限：发起人、管理员或当前任务处理人
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).singleResult();
        boolean canUrge = actor.username().equals(hpi.getStartUserId())
                || actor.hasPermission("*:*:*")
                || actor.username().equals(task.getAssignee());
        if (!canUrge)
        {
            return R.fail("无权催办此任务");
        }

        // 记录催办评论
        String urgeComment = actor.username() + " 发起了催办" + (comment.isBlank() ? "" : "：" + comment);
        taskService.addComment(taskId, task.getProcessInstanceId(), "urge", urgeComment);

        // 通知任务负责人
        if (task.getAssignee() != null)
        {
            Long assigneeId = sysUserMapper.selectUserIdByUserName(task.getAssignee());
            if (assigneeId != null)
            {
                notificationMapper.insertNotification(
                        assigneeId,
                        "流程催办提醒",
                        urgeComment,
                        "wf_urge",
                        "/workflow/task",
                        taskId);
            }
        }
        return R.ok();
    }

    /**
     * 抄送
     */
    public R<Void> cc(String taskId, CcReq request)
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
        {
            return R.fail("任务不存在");
        }
        if (request == null || request.toUsers == null || request.toUsers.isEmpty())
        {
            return R.fail("请选择抄送人");
        }
        for (String toUser : request.toUsers)
        {
            // 通知被抄送人
            Long toUserId = sysUserMapper.selectUserIdByUserName(toUser);
            if (toUserId != null)
            {
                notificationMapper.insertNotification(
                        toUserId,
                        "流程抄送通知",
                        actor.username() + " 抄送了一个流程给您",
                        "wf_cc",
                        "/workflow/instance",
                        task.getProcessInstanceId());
            }
        }
        return R.ok();
    }

    /**
     * 加签（前加签：在当前任务之前插入新任务）
     */
    public R<Void> addsign(String taskId, AddSignReq request)
    {
        if (request == null || request.addSignUser == null || request.addSignUser.isBlank())
        {
            return R.fail("请选择加签人");
        }
        String type = (request.type == null || request.type.isBlank()) ? "before" : request.type.trim().toLowerCase();
        boolean after = "after".equals(type);
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        Task task = taskAuthorizationService.requireActorTask(taskId, actor);

        // 创建新任务（加签任务）
        TaskEntity newTask = (TaskEntity) taskService.newTask();
        newTask.setName(task.getName() + "-加签");
        newTask.setAssignee(request.addSignUser);
        newTask.setProcessInstanceId(task.getProcessInstanceId());
        newTask.setTaskDefinitionKey(task.getTaskDefinitionKey() + "_addsign_" + System.currentTimeMillis());
        newTask.setParentTaskId(task.getId());
        taskService.saveTask(newTask);

        if (after)
        {
            taskService.setOwner(newTask.getId(), actor.username());
        }
        else
        {
            // 将当前任务挂起（owner 保留原 assignee）
            taskService.setOwner(task.getId(), task.getAssignee());
            taskService.setAssignee(task.getId(), null);
        }

        // 保存加签记录
        if (taskAddSignMapper != null)
        {
            WfTaskAddSign record = new WfTaskAddSign();
            record.setOriginalTaskId(taskId);
            record.setAddsignTaskId(newTask.getId());
            record.setAddsignUser(request.addSignUser);
            record.setType(type);
            record.setProcessInstanceId(task.getProcessInstanceId());
            taskAddSignMapper.insert(record);
        }

        // 通知加签人
        Long addSignUserId = sysUserMapper.selectUserIdByUserName(request.addSignUser);
        if (addSignUserId != null)
        {
            notificationMapper.insertNotification(
                    addSignUserId,
                    "新的加签任务",
                    actor.username() + " 给您加签了一个任务【" + task.getName() + "】",
                    "wf_addsign",
                    "/workflow/task",
                    newTask.getId());
        }

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
            row.put("processDefinitionName", process.getProcessDefinitionName());
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
            row.put("processDefinitionName", process.getProcessDefinitionName());
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
        LcBizInstance business = lcBizInstanceMapper.selectByProcessInstanceId(process.getId());
        if (business != null)
        {
            row.put("workflowStatus", business.getWorkflowStatus());
            row.put("currentTaskName", business.getCurrentTaskName());
            row.put("submitter", business.getSubmitter());
        }
        return row;
    }

    private Optional<WorkflowBusinessSyncHandler> findSyncHandler(String processDefinitionId)
    {
        return workflowBusinessSyncHandlers.stream()
                .filter(handler -> handler.supports(processDefinitionId))
                .sorted(java.util.Comparator.comparingInt(WorkflowBusinessSyncHandler::priority).reversed())
                .findFirst();
    }
}
