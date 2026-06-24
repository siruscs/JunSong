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
import com.junsong.finance.domain.FinCostAccounting;
import com.junsong.finance.domain.vo.CostAccountingRealTimeVO;
import com.junsong.finance.service.IFinCostAccountingService;

@RestController
@RequestMapping("/costAccounting")
public class FinCostAccountingController extends BaseController
{
    @Autowired
    private IFinCostAccountingService finCostAccountingService;

    @RequiresPermissions("finance:costAccounting:list")
    @GetMapping("/list")
    public TableDataInfo list(FinCostAccounting finCostAccounting)
    {
        startPage();
        List<FinCostAccounting> list = finCostAccountingService.selectFinCostAccountingList(finCostAccounting);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:costAccounting:query")
    @GetMapping("/realTime")
    public AjaxResult getRealTimeStats(Long deptId)
    {
        return success(finCostAccountingService.getRealTimeStats(deptId));
    }

    @RequiresPermissions("finance:costAccounting:query")
    @GetMapping("/preview")
    public AjaxResult previewCostAccounting(String startDate, String endDate, Long deptId)
    {
        CostAccountingRealTimeVO preview = finCostAccountingService.getPreviewStats(startDate, endDate, deptId);
        return success(preview);
    }

    @RequiresPermissions("finance:costAccounting:query")
    @GetMapping("/checkUnverifiedExpense")
    public AjaxResult checkUnverifiedExpense(Long deptId)
    {
        boolean hasUnverified = finCostAccountingService.checkUnverifiedExpense(deptId);
        AjaxResult result = success();
        result.put("hasUnverified", hasUnverified);
        return result;
    }

    @RequiresPermissions("finance:costAccounting:export")
    @Log(title = "成本核算", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinCostAccounting finCostAccounting)
    {
        List<FinCostAccounting> list = finCostAccountingService.selectFinCostAccountingList(finCostAccounting);
        ExcelUtil<FinCostAccounting> util = new ExcelUtil<FinCostAccounting>(FinCostAccounting.class);
        util.exportExcel(response, list, "成本核算数据");
    }

    @RequiresPermissions("finance:costAccounting:query")
    @GetMapping(value = "/detail/{accountingId}")
    public AjaxResult getInfo(@PathVariable Long accountingId)
    {
        return success(finCostAccountingService.selectFinCostAccountingByAccountingId(accountingId));
    }

    @RequiresPermissions("finance:costAccounting:add")
    @Log(title = "成本核算", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinCostAccounting finCostAccounting)
    {
        finCostAccounting.setDeptId(SecurityUtils.getDeptId());
        if (!finCostAccountingService.checkAccountingNoUnique(finCostAccounting))
        {
            return error("新增成本核算'" + finCostAccounting.getAccountingNo() + "'失败，核算单号已存在");
        }
        return toAjax(finCostAccountingService.insertFinCostAccounting(finCostAccounting));
    }

    @RequiresPermissions("finance:costAccounting:edit")
    @Log(title = "成本核算", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinCostAccounting finCostAccounting)
    {
        finCostAccounting.setDeptId(SecurityUtils.getDeptId());
        if (!finCostAccountingService.checkAccountingNoUnique(finCostAccounting))
        {
            return error("修改成本核算'" + finCostAccounting.getAccountingNo() + "'失败，核算单号已存在");
        }
        return toAjax(finCostAccountingService.updateFinCostAccounting(finCostAccounting));
    }

    @RequiresPermissions("finance:costAccounting:remove")
    @Log(title = "成本核算", businessType = BusinessType.DELETE)
    @DeleteMapping("/{accountingIds}")
    public AjaxResult remove(@PathVariable Long[] accountingIds)
    {
        return toAjax(finCostAccountingService.deleteFinCostAccountingByAccountingIds(accountingIds));
    }
}
