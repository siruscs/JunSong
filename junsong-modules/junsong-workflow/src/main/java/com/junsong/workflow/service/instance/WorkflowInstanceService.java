package com.junsong.workflow.service.instance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.controller.ProcessInstanceController.StartInstanceReq;
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

    public WorkflowInstanceService(
            RuntimeService runtimeService,
            RepositoryService repositoryService,
            HistoryService historyService,
            TaskService taskService,
            CurrentWorkflowUserFacade currentWorkflowUserFacade,
            ProcessAuthorizationService processAuthorizationService)
    {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.taskService = taskService;
        this.currentWorkflowUserFacade = currentWorkflowUserFacade;
        this.processAuthorizationService = processAuthorizationService;
    }

    /**
     * 发起一个流程实例
     */
    public R<Map<String, Object>> start(StartInstanceReq req)
    {
        if (req == null || req.processKey == null || req.processKey.isBlank())
        {
            return R.fail("processKey 不能为空");
        }
        CurrentWorkflowUser actor = currentWorkflowUserFacade.current();
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(req.processKey)
                .latestVersion()
                .singleResult();
        if (def == null)
        {
            return R.fail("找不到流程定义: " + req.processKey);
        }
        if (def.isSuspended())
        {
            return R.fail("流程定义已挂起: " + req.processKey);
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
