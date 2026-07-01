package com.junsong.workflow.controller;

import java.util.List;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.workflow.domain.WfNodeTimeout;
import com.junsong.workflow.service.timeout.WorkflowTimeoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/timeout")
public class WorkflowTimeoutController extends BaseController
{
    @Autowired
    private WorkflowTimeoutService timeoutService;

    @PreAuthorize("@ss.hasPermi('workflow:timeout:list')")
    @GetMapping("/list")
    public TableDataInfo list(WfNodeTimeout timeout)
    {
        startPage();
        List<WfNodeTimeout> list = timeoutService.list(timeout).getData();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('workflow:timeout:list')")
    @GetMapping("/{id}")
    public R<WfNodeTimeout> getById(@PathVariable Long id)
    {
        return timeoutService.getById(id);
    }

    @PreAuthorize("@ss.hasPermi('workflow:timeout:add')")
    @PostMapping
    public R<Void> add(@RequestBody WfNodeTimeout timeout)
    {
        return timeoutService.add(timeout);
    }

    @PreAuthorize("@ss.hasPermi('workflow:timeout:edit')")
    @PutMapping
    public R<Void> update(@RequestBody WfNodeTimeout timeout)
    {
        return timeoutService.update(timeout);
    }

    @PreAuthorize("@ss.hasPermi('workflow:timeout:remove')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id)
    {
        return timeoutService.delete(id);
    }
}
