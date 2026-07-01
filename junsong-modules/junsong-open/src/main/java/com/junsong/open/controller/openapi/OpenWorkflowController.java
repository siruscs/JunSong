package com.junsong.open.controller.openapi;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;


/**
 * 工作流即服务(Workflow as a Service)开放API
 *
 * 对外暴露工作流引擎能力，第三方应用可通过API发起流程、查询实例、处理任务
 *
 * @author junsong
 */
@RestController
@RequestMapping("/workflow")
public class OpenWorkflowController extends BaseController
{
    @Autowired
    private RestTemplate restTemplate;

    private static final String WORKFLOW_BASE = "http://junsong-modules-workflow:9207";

    /**
     * 查询已部署的流程定义列表
     */
    @GetMapping("/definitions")
    public String listDefinitions(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String processKey)
    {
        String url = WORKFLOW_BASE + "/definition/list?pageNum=" + pageNum + "&pageSize=" + pageSize;
        if (processKey != null)
        {
            url += "&processKey=" + processKey;
        }
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 获取流程定义详情
     */
    @GetMapping("/definitions/{id}")
    public AjaxResult getDefinition(@PathVariable("id") String id)
    {
        return restTemplate.getForObject(WORKFLOW_BASE + "/definition/" + id, AjaxResult.class);
    }

    /**
     * 获取流程图(含高亮节点)
     */
    @GetMapping("/definitions/{id}/diagram")
    public AjaxResult getDiagram(@PathVariable("id") String id)
    {
        return restTemplate.getForObject(WORKFLOW_BASE + "/definition/" + id + "/diagram", AjaxResult.class);
    }

    /**
     * 发起流程实例
     *
     * @param params 包含 processKey/businessKey/variables
     */
    @PostMapping("/instances")
    public AjaxResult startInstance(@RequestBody Map<String, Object> params)
    {
        return restTemplate.postForObject(WORKFLOW_BASE + "/instance/start", params, AjaxResult.class);
    }

    /**
     * 查询流程实例列表
     */
    @GetMapping("/instances")
    public String listInstances(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String businessKey)
    {
        String url = WORKFLOW_BASE + "/instance/list?pageNum=" + pageNum + "&pageSize=" + pageSize;
        if (processKey != null) url += "&processKey=" + processKey;
        if (businessKey != null) url += "&businessKey=" + businessKey;
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 获取流程实例详情
     */
    @GetMapping("/instances/{id}")
    public AjaxResult getInstance(@PathVariable("id") String id)
    {
        return restTemplate.getForObject(WORKFLOW_BASE + "/instance/" + id, AjaxResult.class);
    }

    /**
     * 查询待办任务列表
     */
    @GetMapping("/tasks/todo")
    public String todoTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        String url = WORKFLOW_BASE + "/task/todo?pageNum=" + pageNum + "&pageSize=" + pageSize;
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 查询已办任务列表
     */
    @GetMapping("/tasks/done")
    public String doneTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        String url = WORKFLOW_BASE + "/task/done?pageNum=" + pageNum + "&pageSize=" + pageSize;
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 审批通过
     */
    @PostMapping("/tasks/{taskId}/approve")
    public AjaxResult approveTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params)
    {
        return restTemplate.postForObject(WORKFLOW_BASE + "/task/" + taskId + "/approve", params, AjaxResult.class);
    }

    /**
     * 驳回任务
     */
    @PostMapping("/tasks/{taskId}/reject")
    public AjaxResult rejectTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params)
    {
        return restTemplate.postForObject(WORKFLOW_BASE + "/task/" + taskId + "/reject", params, AjaxResult.class);
    }

    /**
     * 转办任务
     */
    @PostMapping("/tasks/{taskId}/transfer")
    public AjaxResult transferTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params)
    {
        return restTemplate.postForObject(WORKFLOW_BASE + "/task/" + taskId + "/transfer", params, AjaxResult.class);
    }

    /**
     * 获取流程历史流转记录
     */
    @GetMapping("/history/instances/{processInstanceId}/activities")
    public AjaxResult getHistoryActivities(@PathVariable("processInstanceId") String processInstanceId)
    {
        return restTemplate.getForObject(WORKFLOW_BASE + "/history/instance/" + processInstanceId + "/activities", AjaxResult.class);
    }

    /**
     * 获取审批意见
     */
    @GetMapping("/history/instances/{processInstanceId}/comments")
    public AjaxResult getHistoryComments(@PathVariable("processInstanceId") String processInstanceId)
    {
        return restTemplate.getForObject(WORKFLOW_BASE + "/history/instance/" + processInstanceId + "/comments", AjaxResult.class);
    }

    /**
     * 流程分析 - 节点耗时统计
     */
    @GetMapping("/analytics/node-duration")
    public AjaxResult nodeDurationAnalytics(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime)
    {
        String url = WORKFLOW_BASE + "/analytics/node-duration?";
        if (processKey != null) url += "processKey=" + processKey + "&";
        if (startTime != null) url += "startTime=" + startTime + "&";
        if (endTime != null) url += "endTime=" + endTime;
        return restTemplate.getForObject(url, AjaxResult.class);
    }
}
