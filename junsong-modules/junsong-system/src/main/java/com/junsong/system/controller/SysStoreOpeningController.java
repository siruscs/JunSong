package com.junsong.system.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysStoreOpening;
import com.junsong.system.service.ISysStoreOpeningService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/storeOpening")
public class SysStoreOpeningController extends BaseController
{
    @Autowired
    private ISysStoreOpeningService storeOpeningService;

    @RequiresPermissions("system:storeOpening:list")
    @GetMapping("/list")
    public TableDataInfo list(SysStoreOpening query)
    {
        startPage();
        List<SysStoreOpening> list = storeOpeningService.selectStoreOpeningList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("system:storeOpening:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(storeOpeningService.selectStoreOpeningById(id));
    }

    @PreAuthorize("@ss.hasPermi('system:storeOpening:query')")
    @GetMapping("/order/{orderNo}")
    public AjaxResult getByOrderNo(@PathVariable("orderNo") String orderNo)
    {
        return success(storeOpeningService.selectStoreOpeningByOrderNo(orderNo));
    }

    @RequiresPermissions("system:storeOpening:add")
    @Log(title = "门店开业申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody SysStoreOpening storeOpening)
    {
        storeOpening.setCreateBy(SecurityUtils.getUsername());
        storeOpeningService.insertStoreOpening(storeOpening);
        return success(storeOpeningService.selectStoreOpeningById(storeOpening.getId()));
    }

    @RequiresPermissions("system:storeOpening:edit")
    @Log(title = "门店开业申请", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody SysStoreOpening storeOpening)
    {
        storeOpening.setUpdateBy(SecurityUtils.getUsername());
        storeOpeningService.updateStoreOpening(storeOpening);
        return success(storeOpeningService.selectStoreOpeningById(storeOpening.getId()));
    }

    @RequiresPermissions("system:storeOpening:remove")
    @Log(title = "门店开业申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        return toAjax(storeOpeningService.deleteStoreOpeningById(id));
    }

    @RequiresPermissions("system:storeOpening:submit")
    @Log(title = "门店开业申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable("id") Long id, @RequestBody SubmitPayload payload)
    {
        return toAjax(storeOpeningService.markSubmitted(
                id,
                payload.processInstanceId,
                payload.processDefinitionKey,
                payload.currentTaskName,
                SecurityUtils.getUsername()));
    }

    @RequiresPermissions("system:storeOpening:withdraw")
    @Log(title = "门店开业申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/withdraw")
    public AjaxResult withdraw(@PathVariable("id") Long id)
    {
        return toAjax(storeOpeningService.withdraw(id, SecurityUtils.getUsername()));
    }

    @InnerAuth
    @PostMapping("/workflow/sync")
    public R<Boolean> syncWorkflowStatus(@RequestBody WorkflowSyncPayload payload)
    {
        int rows = storeOpeningService.syncWorkflowStatus(
                payload.processInstanceId,
                payload.workflowStatus,
                payload.currentTaskName,
                payload.operator == null ? "workflow" : payload.operator);
        return R.ok(rows > 0);
    }

    public static class SubmitPayload
    {
        public String processInstanceId;
        public String processDefinitionKey;
        public String currentTaskName;
    }

    public static class WorkflowSyncPayload
    {
        public String processInstanceId;
        public String workflowStatus;
        public String currentTaskName;
        public String operator;
    }
}
