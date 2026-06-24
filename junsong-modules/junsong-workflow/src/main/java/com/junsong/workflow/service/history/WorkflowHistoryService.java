package com.junsong.workflow.service.history;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.security.CurrentWorkflowUserFacade;
import com.junsong.workflow.security.ProcessAuthorizationService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.springframework.stereotype.Service;

/**
 * 历史流转业务服务：流程实例历史 / 节点流转 / 评论
 * 封装 Flowable HistoryService / TaskService 调用，
 * Controller 层不直接依赖引擎 API。
 *
 * @author junsong
 */
@Service
public class WorkflowHistoryService
{
    private final HistoryService historyService;
    private final TaskService taskService;
    private final CurrentWorkflowUserFacade currentWorkflowUserFacade;
    private final ProcessAuthorizationService processAuthorizationService;

    public WorkflowHistoryService(
            HistoryService historyService,
            TaskService taskService,
            CurrentWorkflowUserFacade currentWorkflowUserFacade,
            ProcessAuthorizationService processAuthorizationService)
    {
        this.historyService = historyService;
        this.taskService = taskService;
        this.currentWorkflowUserFacade = currentWorkflowUserFacade;
        this.processAuthorizationService = processAuthorizationService;
    }

    /**
     * 流程实例的节点流转历史（按时间正序）
     */
    public R<List<Map<String, Object>>> activities(String processInstanceId)
    {
        processAuthorizationService.requireVisibleInstance(processInstanceId, currentWorkflowUserFacade.current());
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        List<Map<String, Object>> rows = list.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("activityId", a.getActivityId());
            m.put("activityName", a.getActivityName());
            m.put("activityType", a.getActivityType());
            m.put("assignee", a.getAssignee());
            m.put("startTime", a.getStartTime());
            m.put("endTime", a.getEndTime());
            m.put("durationMs", a.getDurationInMillis());
            return m;
        }).toList();
        return R.ok(rows);
    }

    /**
     * 流程实例的所有评论（审批意见）
     */
    public R<List<Map<String, Object>>> comments(String processInstanceId)
    {
        processAuthorizationService.requireVisibleInstance(processInstanceId, currentWorkflowUserFacade.current());
        List<Comment> list = taskService.getProcessInstanceComments(processInstanceId);
        List<Map<String, Object>> rows = list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("type", c.getType());
            m.put("userId", c.getUserId());
            m.put("time", c.getTime());
            m.put("message", c.getFullMessage());
            return m;
        }).toList();
        return R.ok(rows);
    }

    /**
     * 已完结流程实例查询（运营看板用）
     */
    public R<List<Map<String, Object>>> finishedInstances(String processKey, Integer limit)
    {
        String username = currentWorkflowUserFacade.current().username();
        var q = historyService.createHistoricProcessInstanceQuery().finished();
        if (processKey != null && !processKey.isBlank())
        {
            q.processDefinitionKey(processKey);
        }
        q.startedBy(username);
        List<HistoricProcessInstance> list = q.orderByProcessInstanceEndTime().desc().listPage(0, limit);
        List<Map<String, Object>> rows = list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("processInstanceId", p.getId());
            m.put("processDefinitionKey", p.getProcessDefinitionKey());
            m.put("processDefinitionName", p.getProcessDefinitionName());
            m.put("businessKey", p.getBusinessKey());
            m.put("startTime", p.getStartTime());
            m.put("endTime", p.getEndTime());
            m.put("durationMs", p.getDurationInMillis());
            m.put("initiator", p.getStartUserId());
            return m;
        }).toList();
        return R.ok(rows);
    }
}
