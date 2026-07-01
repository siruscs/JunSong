package com.junsong.workflow.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.workflow.domain.WfNodeFieldPermission;
import com.junsong.workflow.service.fieldperm.WorkflowFieldPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/field-permission")
public class WorkflowFieldPermissionController extends BaseController
{
    @Autowired
    private WorkflowFieldPermissionService fieldPermissionService;

    @PreAuthorize("@ss.hasPermi('workflow:fieldPermission:list')")
    @GetMapping("/list")
    public TableDataInfo list(WfNodeFieldPermission permission)
    {
        startPage();
        List<WfNodeFieldPermission> list = fieldPermissionService.list(permission).getData();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('workflow:fieldPermission:list')")
    @GetMapping
    public R<List<Map<String, Object>>> getByNode(
            @RequestParam String processDefinitionKey,
            @RequestParam String activityId)
    {
        List<WfNodeFieldPermission> list = fieldPermissionService.getByNode(processDefinitionKey, activityId).getData();
        List<Map<String, Object>> result = list.stream().map(p -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("fieldKey", p.getFieldKey());
            m.put("fieldLabel", p.getFieldLabel());
            m.put("permission", p.getPermission());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @PreAuthorize("@ss.hasPermi('workflow:fieldPermission:add')")
    @PostMapping
    public R<Void> add(@RequestBody WfNodeFieldPermission permission)
    {
        return fieldPermissionService.add(permission);
    }

    @PreAuthorize("@ss.hasPermi('workflow:fieldPermission:edit')")
    @PutMapping
    public R<Void> update(@RequestBody WfNodeFieldPermission permission)
    {
        return fieldPermissionService.update(permission);
    }

    @PreAuthorize("@ss.hasPermi('workflow:fieldPermission:remove')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id)
    {
        return fieldPermissionService.delete(id);
    }
}
