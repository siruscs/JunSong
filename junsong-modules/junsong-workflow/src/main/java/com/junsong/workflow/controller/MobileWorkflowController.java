package com.junsong.workflow.controller;

import java.util.List;
import java.util.Map;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.service.task.WorkflowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 移动办公 API（小程序/移动端专用）
 */
@RestController
@RequestMapping("/mobile")
public class MobileWorkflowController
{
    @Autowired
    private WorkflowTaskService workflowTaskService;

    @PreAuthorize("@ss.hasPermi('workflow:mobile:todo')")
    @GetMapping("/todo")
    public R<List<Map<String, Object>>> todo()
    {
        return workflowTaskService.todo();
    }

    @PreAuthorize("@ss.hasPermi('workflow:mobile:done')")
    @GetMapping("/done")
    public R<List<Map<String, Object>>> done()
    {
        return workflowTaskService.done();
    }

    @PreAuthorize("@ss.hasPermi('workflow:mobile:applied')")
    @GetMapping("/applied")
    public R<List<Map<String, Object>>> applied()
    {
        return workflowTaskService.applied();
    }

    @PreAuthorize("@ss.hasPermi('workflow:mobile:approve')")
    @PostMapping("/task/{taskId}/approve")
    public R<Void> approve(@PathVariable String taskId, @RequestBody(required = false) TaskController.ApproveReq request)
    {
        return workflowTaskService.approve(taskId, request);
    }

    @PreAuthorize("@ss.hasPermi('workflow:mobile:reject')")
    @PostMapping("/task/{taskId}/reject")
    public R<Void> reject(@PathVariable String taskId, @RequestBody(required = false) TaskController.RejectReq request)
    {
        return workflowTaskService.reject(taskId, request);
    }

    @PreAuthorize("@ss.hasPermi('workflow:mobile:detail')")
    @GetMapping("/task/{taskId}")
    public R<Map<String, Object>> detail(@PathVariable String taskId)
    {
        return workflowTaskService.detail(taskId);
    }
}
