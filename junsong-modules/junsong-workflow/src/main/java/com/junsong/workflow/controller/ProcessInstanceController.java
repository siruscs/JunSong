package com.junsong.workflow.controller;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.service.instance.WorkflowInstanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程实例：发起 / 查询 / 终止 / 详情
 *
 * @author junsong
 */
@RestController
@RequestMapping("/instance")
public class ProcessInstanceController
{
    private final WorkflowInstanceService workflowInstanceService;

    public ProcessInstanceController(WorkflowInstanceService workflowInstanceService)
    {
        this.workflowInstanceService = workflowInstanceService;
    }

    /**
     * 发起一个流程实例
     * Body 示例：
     * {
     *   "processKey": "leave_apply",
     *   "businessKey": "LEAVE-2026-0001",
     *   "variables": { "days": 5, "leaderUser": "lisi", "deptHeadUser": "wangwu" }
     * }
     */
    @PreAuthorize("@ss.hasPermi('workflow:instance:start')")
    @PostMapping("/start")
    public R<Map<String, Object>> start(@RequestBody StartInstanceReq req)
    {
        return workflowInstanceService.start(req);
    }

    /**
     * 查询流程实例（运行中）
     * GET /workflow/instance/list?processKey=&businessKey=
     */
    @PreAuthorize("@ss.hasPermi('workflow:instance:list')")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String processKey,
                                             @RequestParam(required = false) String businessKey)
    {
        return workflowInstanceService.list(processKey, businessKey);
    }

    /**
     * 流程实例详情（含运行/历史）
     */
    @PreAuthorize("@ss.hasPermi('workflow:instance:list')")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable("id") String id)
    {
        return workflowInstanceService.detail(id);
    }

    /**
     * 终止流程实例
     */
    @PreAuthorize("@ss.hasPermi('workflow:instance:terminate')")
    @PostMapping("/{id}/terminate")
    public R<Void> terminate(@PathVariable("id") String id, @RequestParam(required = false) String reason)
    {
        return workflowInstanceService.terminate(id, reason);
    }

    /**
     * 查询流程实例的运行中任务（用于追踪候选组任务 / 节点流转）
     * GET /workflow/instance/tasks/running?processInstanceId=xxx
     */
    @PreAuthorize("@ss.hasPermi('workflow:instance:list')")
    @GetMapping("/tasks/running")
    public R<List<Map<String, Object>>> runningTasks(@RequestParam String processInstanceId)
    {
        return workflowInstanceService.runningTasks(processInstanceId);
    }

    /**
     * 启动请求体
     */
    public static class StartInstanceReq {
        public String processKey;
        public String businessKey;
        public Map<String, Object> variables;
    }
}
