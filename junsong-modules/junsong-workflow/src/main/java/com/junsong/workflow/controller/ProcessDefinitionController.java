package com.junsong.workflow.controller;

import java.util.ArrayList;
import java.util.List;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.controller.dto.definition.DefinitionSummaryResp;
import com.junsong.workflow.controller.dto.definition.DefinitionXmlResp;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionReq;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionResp;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionReq;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionResp;
import com.junsong.workflow.service.definition.WorkflowDefinitionService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/definition")
public class ProcessDefinitionController
{
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ProcessEngine processEngine;

    public ProcessDefinitionController(WorkflowDefinitionService workflowDefinitionService,
                                       ProcessEngine processEngine)
    {
        this.workflowDefinitionService = workflowDefinitionService;
        this.processEngine = processEngine;
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:list')")
    @GetMapping("/list")
    public R<List<DefinitionSummaryResp>> list(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "true") Boolean latestOnly)
    {
        return R.ok(workflowDefinitionService.list(key, keyword, latestOnly, category));
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:list')")
    @GetMapping("/categories")
    public R<List<String>> categories()
    {
        return R.ok(workflowDefinitionService.listCategories());
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:list')")
    @GetMapping("/{id}")
    public R<DefinitionSummaryResp> detail(@PathVariable("id") String id)
    {
        return R.ok(workflowDefinitionService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:list')")
    @GetMapping("/{id}/diagram")
    public ResponseEntity<byte[]> diagram(@PathVariable("id") String id)
    {
        try
        {
            // 高亮当前活动节点：查询该定义下所有运行中实例的当前活动节点
            List<String> highLightedActivities = new ArrayList<>();
            List<ProcessInstance> instances = processEngine.getRuntimeService()
                    .createProcessInstanceQuery()
                    .processDefinitionId(id)
                    .list();
            for (ProcessInstance instance : instances)
            {
                highLightedActivities.addAll(
                        processEngine.getRuntimeService().getActiveActivityIds(instance.getId()));
            }
            byte[] bytes = workflowDefinitionService.generateDiagram(id, highLightedActivities, List.of());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        }
        catch (ServiceException exception)
        {
            // 流程图无法生成（无图形信息）时返回 404
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:export') or @ss.hasPermi('workflow:task:list') or @ss.hasPermi('workflow:instance:start')")
    @GetMapping("/{id}/xml")
    public R<DefinitionXmlResp> xml(@PathVariable("id") String id)
    {
        return R.ok(workflowDefinitionService.loadXml(id));
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:deploy')")
    @PostMapping("/validate")
    public R<ValidateDefinitionResp> validate(@RequestBody ValidateDefinitionReq request)
    {
        return R.ok(workflowDefinitionService.validate(request));
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:deploy')")
    @Idempotent(scene = "workflow:definition:deploy", highRisk = true, ttlSeconds = 2592000)
    @PostMapping("/deploy")
    public R<DeployDefinitionResp> deploy(@RequestBody DeployDefinitionReq request)
    {
        return R.ok(workflowDefinitionService.deploy(request));
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:edit')")
    @Idempotent(scene = "workflow:definition:suspend")
    @PostMapping("/{id}/suspend")
    public R<Void> suspend(@PathVariable("id") String id)
    {
        workflowDefinitionService.suspend(id);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:edit')")
    @Idempotent(scene = "workflow:definition:activate")
    @PostMapping("/{id}/activate")
    public R<Void> activate(@PathVariable("id") String id)
    {
        workflowDefinitionService.activate(id);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:remove')")
    @Idempotent(scene = "workflow:definition:delete")
    @DeleteMapping("/{id}")
    public R<Void> deleteDefinition(
            @PathVariable("id") String id,
            @RequestParam(defaultValue = "false") Boolean cascade)
    {
        workflowDefinitionService.deleteDefinition(id, cascade);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:remove')")
    @Idempotent(scene = "workflow:definition:delete-deployment")
    @DeleteMapping("/deployment/{deploymentId}")
    public R<Void> deleteDeployment(
            @PathVariable("deploymentId") String deploymentId,
            @RequestParam(defaultValue = "false") Boolean cascade)
    {
        workflowDefinitionService.deleteDeployment(deploymentId, cascade);
        return R.ok();
    }
}
