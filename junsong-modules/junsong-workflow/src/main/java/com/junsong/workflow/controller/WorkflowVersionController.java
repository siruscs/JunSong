package com.junsong.workflow.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.junsong.common.core.domain.R;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/definition")
public class WorkflowVersionController
{
    @Autowired
    private RepositoryService repositoryService;

    @PreAuthorize("@ss.hasPermi('workflow:definition:list')")
    @GetMapping("/{processKey}/versions")
    public R<List<Map<String, Object>>> versions(@PathVariable String processKey)
    {
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .orderByProcessDefinitionVersion().desc()
                .list();
        List<Map<String, Object>> result = defs.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("definitionId", d.getId());
            m.put("version", d.getVersion());
            m.put("name", d.getName());
            m.put("key", d.getKey());
            m.put("suspended", d.isSuspended());
            m.put("deploymentId", d.getDeploymentId());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:edit')")
    @PostMapping("/{definitionId}/suspend")
    public R<Void> suspendVersion(@PathVariable String definitionId)
    {
        repositoryService.suspendProcessDefinitionById(definitionId);
        return R.ok();
    }

    @PreAuthorize("@ss.hasPermi('workflow:definition:edit')")
    @PostMapping("/{definitionId}/activate")
    public R<Void> activateVersion(@PathVariable String definitionId)
    {
        repositoryService.activateProcessDefinitionById(definitionId);
        return R.ok();
    }
}
