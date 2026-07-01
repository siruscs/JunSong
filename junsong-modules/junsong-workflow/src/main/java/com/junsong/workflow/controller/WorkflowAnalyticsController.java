package com.junsong.workflow.controller;

import java.util.List;
import java.util.Map;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.service.analytics.WorkflowAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class WorkflowAnalyticsController
{
    @Autowired
    private WorkflowAnalyticsService analyticsService;

    @PreAuthorize("@ss.hasPermi('workflow:analytics:list')")
    @GetMapping("/node-duration")
    public R<List<Map<String, Object>>> nodeDuration(@RequestParam String processDefinitionKey)
    {
        return analyticsService.nodeDurationStats(processDefinitionKey);
    }

    @PreAuthorize("@ss.hasPermi('workflow:analytics:list')")
    @GetMapping("/user-efficiency")
    public R<List<Map<String, Object>>> userEfficiency()
    {
        return analyticsService.userEfficiencyStats();
    }

    @PreAuthorize("@ss.hasPermi('workflow:analytics:list')")
    @GetMapping("/process-duration")
    public R<Map<String, Object>> processDuration(@RequestParam String processDefinitionKey)
    {
        return analyticsService.processDurationStats(processDefinitionKey);
    }
}
