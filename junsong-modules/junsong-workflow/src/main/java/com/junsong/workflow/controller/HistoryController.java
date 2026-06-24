package com.junsong.workflow.controller;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.service.history.WorkflowHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 历史流转：流程实例历史 / 节点流转 / 评论 / 用于流程跟踪图
 *
 * @author junsong
 */
@RestController
@RequestMapping("/history")
public class HistoryController
{
    private final WorkflowHistoryService workflowHistoryService;

    public HistoryController(WorkflowHistoryService workflowHistoryService)
    {
        this.workflowHistoryService = workflowHistoryService;
    }

    /**
     * 流程实例的节点流转历史（按时间正序）
     * GET /workflow/history/instance/{processInstanceId}/activities
     */
    @PreAuthorize("@ss.hasPermi('workflow:history:list')")
    @GetMapping("/instance/{processInstanceId}/activities")
    public R<List<Map<String, Object>>> activities(@PathVariable("processInstanceId") String processInstanceId)
    {
        return workflowHistoryService.activities(processInstanceId);
    }

    /**
     * 流程实例的所有评论（审批意见）
     */
    @PreAuthorize("@ss.hasPermi('workflow:history:list')")
    @GetMapping("/instance/{processInstanceId}/comments")
    public R<List<Map<String, Object>>> comments(@PathVariable("processInstanceId") String processInstanceId)
    {
        return workflowHistoryService.comments(processInstanceId);
    }

    /**
     * 已完结流程实例查询（运营看板用）
     */
    @PreAuthorize("@ss.hasPermi('workflow:history:list')")
    @GetMapping("/instances")
    public R<List<Map<String, Object>>> finishedInstances(@RequestParam(required = false) String processKey,
                                                          @RequestParam(defaultValue = "100") Integer limit)
    {
        return workflowHistoryService.finishedInstances(processKey, limit);
    }
}
