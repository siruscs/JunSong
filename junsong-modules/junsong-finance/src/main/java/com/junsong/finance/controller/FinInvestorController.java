package com.junsong.finance.controller;

import java.util.List;
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
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinInvestor;
import com.junsong.finance.service.IFinInvestorService;

@RestController
@RequestMapping("/investor")
public class FinInvestorController extends BaseController
{
    @Autowired
    private IFinInvestorService finInvestorService;

    @RequiresPermissions("finance:investor:list")
    @GetMapping("/list")
    public TableDataInfo list(FinInvestor finInvestor)
    {
        startPage();
        List<FinInvestor> list = finInvestorService.selectFinInvestorList(finInvestor);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:investor:query")
    @GetMapping("/{investorId}")
    public AjaxResult getInfo(@PathVariable Long investorId)
    {
        return success(finInvestorService.selectFinInvestorByInvestorId(investorId));
    }

    @RequiresPermissions("finance:investor:add")
    @Log(title = "投资人", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinInvestor finInvestor)
    {
        finInvestor.setDeptId(SecurityUtils.getDeptId());
        finInvestor.setCreateBy(SecurityUtils.getUsername());
        return toAjax(finInvestorService.insertFinInvestor(finInvestor));
    }

    @RequiresPermissions("finance:investor:edit")
    @Log(title = "投资人", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinInvestor finInvestor)
    {
        finInvestor.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finInvestorService.updateFinInvestor(finInvestor));
    }

    @RequiresPermissions("finance:investor:remove")
    @Log(title = "投资人", businessType = BusinessType.DELETE)
    @DeleteMapping("/{investorIds}")
    public AjaxResult remove(@PathVariable Long[] investorIds)
    {
        return toAjax(finInvestorService.deleteFinInvestorByInvestorIds(investorIds));
    }
}
