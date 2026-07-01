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

    @PreAuthorize("@ss.hasPermi('workflow:task:approve')")
    @PostMapping("/batch-approve")
    public R<Map<String, Object>> batchApprove(@RequestBody BatchApproveReq request)
    {
        return workflowTaskService.batchApprove(request.taskIds, request.comment);
    }

    public static class BatchApproveReq
    {
        public List<String> taskIds;
        public String comment;
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

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/{taskId}/reject-targets")
    public R<List<Map<String, Object>>> rejectTargets(@PathVariable("taskId") String taskId)
    {
        return workflowTaskService.rejectTargets(taskId);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @PostMapping("/{taskId}/transfer")
    public R<Void> transfer(
            @PathVariable("taskId") String taskId,
            @RequestParam("toUser") String toUser)
    {
        return workflowTaskService.transfer(taskId, toUser);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @PostMapping("/{taskId}/delegate")
    public R<Void> delegate(
            @PathVariable("taskId") String taskId,
            @RequestBody DelegateReq request)
    {
        return workflowTaskService.delegate(taskId, request.toUser);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @PostMapping("/{taskId}/resolve")
    public R<Void> resolve(@PathVariable("taskId") String taskId)
    {
        return workflowTaskService.resolveTask(taskId);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:urge')")
    @PostMapping("/{taskId}/urge")
    public R<Void> urge(
            @PathVariable("taskId") String taskId,
            @RequestBody(required = false) UrgeReq request)
    {
        return workflowTaskService.urge(taskId, request);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:cc')")
    @PostMapping("/{taskId}/cc")
    public R<Void> cc(
            @PathVariable("taskId") String taskId,
            @RequestBody CcReq request)
    {
        return workflowTaskService.cc(taskId, request);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:addsign')")
    @PostMapping("/{taskId}/addsign")
    public R<Void> addsign(
            @PathVariable("taskId") String taskId,
            @RequestBody AddSignReq request)
    {
        return workflowTaskService.addsign(taskId, request);
    }

    public static class ApproveReq
    {
        public String comment;
        public Map<String, Object> variables;
        public List<Map<String, String>> attachments;
    }

    public static class RejectReq
    {
        public String comment;
        public String targetActivityId;
        public String targetType;
        public List<Map<String, String>> attachments;
    }

    public static class UrgeReq
    {
        public String comment;
    }

    public static class CcReq
    {
        public List<String> toUsers;
    }

    public static class AddSignReq
    {
        public String addSignUser;
        public String type;
    }

    public static class DelegateReq
    {
        public String toUser;
    }
}
