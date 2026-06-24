package com.junsong.workflow.controller;

import java.util.List;
import java.util.Map;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.service.task.WorkflowTaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController
{
    private final WorkflowTaskService workflowTaskService;

    public TaskController(WorkflowTaskService workflowTaskService)
    {
        this.workflowTaskService = workflowTaskService;
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/todo")
    public R<List<Map<String, Object>>> todo()
    {
        return workflowTaskService.todo();
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/done")
    public R<List<Map<String, Object>>> done()
    {
        return workflowTaskService.done();
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/applied")
    public R<List<Map<String, Object>>> applied()
    {
        return workflowTaskService.applied();
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/{taskId}")
    public R<Map<String, Object>> detail(@PathVariable("taskId") String taskId)
    {
        return workflowTaskService.detail(taskId);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @PostMapping("/{taskId}/claim")
    public R<Void> claim(@PathVariable("taskId") String taskId)
    {
        return workflowTaskService.claim(taskId);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:approve')")
    @PostMapping("/{taskId}/approve")
    public R<Void> approve(
            @PathVariable("taskId") String taskId,
            @RequestBody(required = false) ApproveReq request)
    {
        return workflowTaskService.approve(taskId, request);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:reject')")
    @PostMapping("/{taskId}/reject")
    public R<Void> reject(
            @PathVariable("taskId") String taskId,
            @RequestBody(required = false) RejectReq request)
    {
        return workflowTaskService.reject(taskId, request);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @PostMapping("/{taskId}/transfer")
    public R<Void> transfer(
            @PathVariable("taskId") String taskId,
            @RequestParam("toUser") String toUser)
    {
        return workflowTaskService.transfer(taskId, toUser);
    }

    public static class ApproveReq
    {
        public String comment;
        public Map<String, Object> variables;
    }

    public static class RejectReq
    {
        public String comment;
    }
}
