package com.junsong.workflow.controller;

import java.util.List;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.controller.dto.definition.DefinitionSummaryResp;
import com.junsong.workflow.controller.dto.definition.DefinitionXmlResp;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionReq;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionResp;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionReq;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionResp;
import com.junsong.workflow.service.definition.WorkflowDefinitionService;
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

    public ProcessDefinitionController(WorkflowDefinitionService workflowDefinitionService)
    {
        this.workflowDefinitionService = workflowDefinitionService;
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

    @PreAuthorize("@ss.hasPermi('workflow:definition:export')")
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
    @PostMapping("/deploy")
    public R<DeployDefinitionResp> deploy(@RequestBody DeployDefinitionReq request)
    {
        return R.ok(workflowDefinitionService.deploy(request));
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:edit')")
    @PostMapping("/{id}/suspend")
    public R<Void> suspend(@PathVariable("id") String id)
    {
        workflowDefinitionService.suspend(id);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:edit')")
    @PostMapping("/{id}/activate")
    public R<Void> activate(@PathVariable("id") String id)
    {
        workflowDefinitionService.activate(id);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:remove')")
    @DeleteMapping("/deployment/{deploymentId}")
    public R<Void> deleteDeployment(
            @PathVariable("deploymentId") String deploymentId,
            @RequestParam(defaultValue = "false") Boolean cascade)
    {
        workflowDefinitionService.deleteDeployment(deploymentId, cascade);
        return R.ok();
    }
}
