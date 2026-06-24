package com.junsong.finance.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.service.IFinAdvanceService;

@RestController
@RequestMapping("/advance")
public class FinAdvanceController extends BaseController
{
    @Autowired
    private IFinAdvanceService finAdvanceService;

    @RequiresPermissions("finance:advance:list")
    @GetMapping("/list")
    public TableDataInfo list(FinAdvance finAdvance)
    {
        startPage();
        List<FinAdvance> list = finAdvanceService.selectFinAdvanceList(finAdvance);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:advance:export")
    @Log(title = "借支记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinAdvance finAdvance)
    {
        List<FinAdvance> list = finAdvanceService.selectFinAdvanceList(finAdvance);
        ExcelUtil<FinAdvance> util = new ExcelUtil<FinAdvance>(FinAdvance.class);
        util.exportExcel(response, list, "借支记录数据");
    }

    @RequiresPermissions("finance:advance:query")
    @GetMapping(value = "/{advanceId}")
    public AjaxResult getInfo(@PathVariable Long advanceId)
    {
        return success(finAdvanceService.selectFinAdvanceByAdvanceId(advanceId));
    }

    @RequiresPermissions("finance:advance:add")
    @Log(title = "借支记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinAdvance finAdvance)
    {
        finAdvance.setDeptId(SecurityUtils.getDeptId());
        if (!finAdvanceService.checkAdvanceNoUnique(finAdvance))
        {
            return error("新增借支记录'" + finAdvance.getAdvanceNo() + "'失败，借支单号已存在");
        }
        return toAjax(finAdvanceService.insertFinAdvance(finAdvance));
    }

    @RequiresPermissions("finance:advance:edit")
    @Log(title = "借支记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinAdvance finAdvance)
    {
        finAdvance.setDeptId(SecurityUtils.getDeptId());
        if (!finAdvanceService.checkAdvanceNoUnique(finAdvance))
        {
            return error("修改借支记录'" + finAdvance.getAdvanceNo() + "'失败，借支单号已存在");
        }
        return toAjax(finAdvanceService.updateFinAdvance(finAdvance));
    }

    @RequiresPermissions("finance:advance:remove")
    @Log(title = "借支记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{advanceIds}")
    public AjaxResult remove(@PathVariable Long[] advanceIds)
    {
        return toAjax(finAdvanceService.deleteFinAdvanceByAdvanceIds(advanceIds));
    }

    @RequiresPermissions("finance:advance:edit")
    @Log(title = "核销借支", businessType = BusinessType.UPDATE)
    @PutMapping("/verify/{advanceId}")
    public AjaxResult verify(@PathVariable Long advanceId)
    {
        return toAjax(finAdvanceService.verifyAdvance(advanceId, SecurityUtils.getUsername()));
    }
}
