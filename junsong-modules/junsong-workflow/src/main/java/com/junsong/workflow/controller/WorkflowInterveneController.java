package com.junsong.workflow.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.junsong.common.core.domain.R;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/instance")
public class WorkflowInterveneController
{
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;

    @PreAuthorize("@ss.hasPermi('workflow:instance:intervene')")
    @PostMapping("/{processInstanceId}/jump")
    public R<Void> jump(
            @PathVariable String processInstanceId,
            @RequestParam String targetActivityId)
    {
        // 获取当前活动节点
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list();
        if (executions.isEmpty())
        {
            return R.fail("流程实例不存在或已结束");
        }
        String currentActivityId = null;
        for (Execution e : executions)
        {
            if (e.getActivityId() != null)
            {
                currentActivityId = e.getActivityId();
                break;
            }
        }
        if (currentActivityId == null)
        {
            return R.fail("无法获取当前活动节点");
        }
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentActivityId, targetActivityId)
                .changeState();
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:instance:intervene')")
    @PostMapping("/{processInstanceId}/suspend")
    public R<Void> suspend(@PathVariable String processInstanceId)
    {
        runtimeService.suspendProcessInstanceById(processInstanceId);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:instance:intervene')")
    @PostMapping("/{processInstanceId}/activate")
    public R<Void> activate(@PathVariable String processInstanceId)
    {
        runtimeService.activateProcessInstanceById(processInstanceId);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:instance:intervene')")
    @GetMapping("/{processInstanceId}/activity-history")
    public R<List<Map<String, Object>>> activityHistory(@PathVariable String processInstanceId)
    {
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().desc()
                .list();
        List<Map<String, Object>> result = list.stream().map(a -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("activityId", a.getActivityId());
            m.put("activityName", a.getActivityName());
            m.put("activityType", a.getActivityType());
            m.put("startTime", a.getStartTime());
            m.put("endTime", a.getEndTime());
            m.put("assignee", a.getAssignee());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }
}
