package com.junsong.workflow.service.instance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.controller.ProcessInstanceController.StartInstanceReq;
import com.junsong.workflow.mapper.WfNotificationMapper;
import com.junsong.workflow.mapper.WfSysUserMapper;
import com.junsong.workflow.security.CurrentWorkflowUser;
import com.junsong.workflow.security.CurrentWorkflowUserFacade;
import com.junsong.workflow.security.ProcessAuthorizationService;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

/**
 * 流程实例业务服务：发起 / 查询 / 终止 / 详情 / 运行中任务
 * 封装 Flowable RuntimeService / RepositoryService / HistoryService / TaskService 调用，
 * Controller 层不直接依赖引擎 API。
 *
 * @author junsong
 */
@Service
public class WorkflowInstanceService
{
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final CurrentWorkflowUserFacade currentWorkflowUserFacade;
    private final ProcessAuthorizationService processAuthorizationService;
    private final WfNotificationMapper notificationMapper;
    private final WfSysUserMapper sysUserMapper;

    public WorkflowInstanceService(
            RuntimeService runtimeService,
            RepositoryService repositoryService,
            HistoryService historyService,
            TaskService taskService,
            CurrentWorkflowUserFacade currentWorkflowUserFacade,
            ProcessAuthorizationService processAuthorizationService,
            WfNotificationMapper notificationMapper,
            WfSysUserMapper sysUserMapper)
    {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.taskService = taskService;
        this.currentWorkflowUserFacade = currentWorkflowUserFacade;
        this.processAuthorizationService = processAuthorizationService;
        this.notificationMapper = notificationMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 发起一个流程实例
     */
    public R<Map<String, Object>> start(StartInstanceReq req)
    {
        if (req == null)
        {
            return R.fail("请求体不能为空");
        }
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        ProcessDefinition def;
        if (req.processDefinitionId != null && !req.processDefinitionId.isBlank())
        {
            // 指定流程定义ID，发起特定版本
            def = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(req.processDefinitionId)
                    .singleResult();
            if (def == null)
            {
                return R.fail("找不到流程定义: " + req.processDefinitionId);
            }
        }
        else
        {
            // 默认使用最新版本
            if (req.processKey == null || req.processKey.isBlank())
            {
                return R.fail("processKey 不能为空");
            }
            def = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(req.processKey)
                    .latestVersion()
                    .singleResult();
            if (def == null)
            {
                return R.fail("找不到流程定义: " + req.processKey);
            }
        }
        if (def.isSuspended())
        {
            return R.fail("流程定义已挂起: " + def.getKey());
        }
        Map<String, Object> vars = req.variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(req.variables);
        vars.put("initiator", actor.username());
        Authentication.setAuthenticatedUserId(actor.username());
        try
        {
            ProcessInstance pi;
            if (req.businessKey != null && !req.businessKey.isBlank())
            {
                pi = runtimeService.startProcessInstanceById(def.getId(), req.businessKey, vars);
            }
            else
            {
                pi = runtimeService.startProcessInstanceById(def.getId(), vars);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("processInstanceId", pi.getId());
            result.put("processDefinitionId", pi.getProcessDefinitionId());
            result.put("processDefinitionKey", def.getKey());
            result.put("processDefinitionName", def.getName());
            result.put("businessKey", pi.getBusinessKey());
            result.put("startTime", pi.getStartTime());

            // 流程启动后，检查是否有待办任务，通知任务负责人
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(pi.getId())
                    .list();
            for (Task task : tasks)
            {
                if (task.getAssignee() != null && !task.getAssignee().isBlank())
                {
                    Long assigneeUserId = sysUserMapper.selectUserIdByUserName(task.getAssignee());
                    if (assigneeUserId != null)
                    {
                        notificationMapper.insertNotification(
                                assigneeUserId,
                                "新的流程待办任务",
                                "您有一个新的【" + (def.getName() != null ? def.getName() : def.getKey()) + "】流程待办任务需要处理",
                                "wf_todo",
                                "/workflow/task",
                                task.getId());
                    }
                }
            }

            return R.ok(result);
        }
        finally
        {
            Authentication.setAuthenticatedUserId(null);
        }
    }

    /**
     * 查询流程实例（运行中）
     */
    public R<List<Map<String, Object>>> list(String processKey, String businessKey)
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        var q = runtimeService.createProcessInstanceQuery();
        q.startedBy(actor.username());
        if (processKey != null && !processKey.isBlank())
        {
            q.processDefinitionKey(processKey);
        }
        if (businessKey != null && !businessKey.isBlank())
        {
            q.processInstanceBusinessKey(businessKey);
        }
        List<ProcessInstance> list = q.orderByProcessInstanceId().desc().list();
        List<Map<String, Object>> rows = list.stream().map(this::toRow).toList();
        return R.ok(rows);
    }

    /**
     * 流程实例详情（含运行/历史）
     */
    public R<Map<String, Object>> detail(String id)
    {
        processAuthorizationService.requireVisibleInstance(id, currentWorkflowUserFacade.current());
        Map<String, Object> result = new LinkedHashMap<>();
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
        if (pi != null)
        {
            result.put("running", true);
            result.put("instance", toRow(pi));
        }
        else
        {
            HistoricProcessInstance hi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(id).singleResult();
            if (hi != null)
            {
                result.put("running", false);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("processInstanceId", hi.getId());
                m.put("processDefinitionId", hi.getProcessDefinitionId());
                m.put("processDefinitionKey", hi.getProcessDefinitionKey());
                m.put("processDefinitionName", hi.getProcessDefinitionName());
                m.put("businessKey", hi.getBusinessKey());
                m.put("startTime", hi.getStartTime());
                m.put("endTime", hi.getEndTime());
                m.put("startUserId", hi.getStartUserId());
                m.put("durationMs", hi.getDurationInMillis());
                result.put("instance", m);
            }
            else
            {
                return R.fail("流程实例不存在: " + id);
            }
        }
        return R.ok(result);
    }

    /**
     * 终止流程实例
     */
    public R<Void> terminate(String id, String reason)
    {
        processAuthorizationService.requireTerminableInstance(id, currentWorkflowUserFacade.current());
        runtimeService.deleteProcessInstance(id, reason == null ? "用户主动终止" : reason);
        return R.ok();
    }

    /**
     * 查询流程实例的运行中任务
     */
    public R<List<Map<String, Object>>> runningTasks(String processInstanceId)
    {
        processAuthorizationService.requireVisibleInstance(processInstanceId, currentWorkflowUserFacade.current());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).orderByTaskCreateTime().desc().list();
        List<Map<String, Object>> rows = tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getId());
            m.put("taskName", t.getName());
            m.put("description", t.getDescription());
            m.put("assignee", t.getAssignee());
            m.put("createTime", t.getCreateTime());
            m.put("dueDate", t.getDueDate());
            return m;
        }).toList();
        return R.ok(rows);
    }

    /**
     * 发起人撤回流程（下一节点未处理前）
     */
    public R<Void> withdraw(String processInstanceId)
    {
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hpi == null)
        {
            return R.fail("流程实例不存在");
        }
        // 只有发起人可以撤回
        if (!actor.username().equals(hpi.getStartUserId()))
        {
            return R.fail("只有发起人可以撤回流程");
        }
        // 检查是否有任务已被处理
        long doneCount = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId).finished().count();
        if (doneCount > 0)
        {
            return R.fail("流程已有审批记录，无法撤回");
        }
        // 撤回：删除流程实例
        runtimeService.deleteProcessInstance(processInstanceId, "发起人撤回");
        // 通知相关审批人
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId).list();
        for (Task t : tasks)
        {
            if (t.getAssignee() != null)
            {
                Long userId = sysUserMapper.selectUserIdByUserName(t.getAssignee());
                if (userId != null)
                {
                    notificationMapper.insertNotification(
                            userId, "流程已撤回",
                            actor.username() + " 撤回了流程",
                            "wf_withdraw", "/workflow/instance", processInstanceId);
                }
            }
        }
        return R.ok();
    }

    private Map<String, Object> toRow(ProcessInstance pi)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("processInstanceId", pi.getId());
        m.put("processDefinitionId", pi.getProcessDefinitionId());
        m.put("processDefinitionKey", pi.getProcessDefinitionKey());
        m.put("processDefinitionName", pi.getProcessDefinitionName());
        m.put("businessKey", pi.getBusinessKey());
        m.put("startTime", pi.getStartTime());
        m.put("startUserId", pi.getStartUserId());
        m.put("suspended", pi.isSuspended());
        return m;
    }
}
