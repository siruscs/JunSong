package com.junsong.workflow.controller;

import com.junsong.workflow.service.health.WorkflowHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工作流引擎健康检查
 * 用于验证 Flowable 引擎是否成功启动并完成自建表。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/health")
public class WorkflowHealthController {

    private final WorkflowHealthService workflowHealthService;

    public WorkflowHealthController(WorkflowHealthService workflowHealthService) {
        this.workflowHealthService = workflowHealthService;
    }

    @GetMapping("/engine")
    public Map<String, Object> engine() {
        return workflowHealthService.engine();
    }
}
