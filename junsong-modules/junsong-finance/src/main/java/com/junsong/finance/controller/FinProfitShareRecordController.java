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
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.service.IFinProfitShareRecordService;

@RestController
@RequestMapping("/profitShare")
public class FinProfitShareRecordController extends BaseController
{
    @Autowired
    private IFinProfitShareRecordService finProfitShareRecordService;

    @RequiresPermissions("finance:profitShare:list")
    @GetMapping("/list")
    public TableDataInfo list(FinProfitShareRecord finProfitShareRecord)
    {
        startPage();
        List<FinProfitShareRecord> list = finProfitShareRecordService.selectFinProfitShareRecordList(finProfitShareRecord);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:profitShare:query")
    @GetMapping("/{shareId}")
    public AjaxResult getInfo(@PathVariable Long shareId)
    {
        return success(finProfitShareRecordService.selectFinProfitShareRecordByShareId(shareId));
    }

    @RequiresPermissions("finance:profitShare:add")
    @Log(title = "分润结转", businessType = BusinessType.INSERT)
    @PostMapping("/carryForward/{periodId}")
    public AjaxResult carryForward(@PathVariable Long periodId)
    {
        return success(finProfitShareRecordService.carryForwardPeriod(periodId));
    }

    @RequiresPermissions("finance:profitShare:add")
    @Log(title = "分润记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinProfitShareRecord finProfitShareRecord)
    {
        finProfitShareRecord.setDeptId(SecurityUtils.getDeptId());
        finProfitShareRecord.setCreateBy(SecurityUtils.getUsername());
        return toAjax(finProfitShareRecordService.insertFinProfitShareRecord(finProfitShareRecord));
    }

    @RequiresPermissions("finance:profitShare:edit")
    @Log(title = "分润记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinProfitShareRecord finProfitShareRecord)
    {
        finProfitShareRecord.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finProfitShareRecordService.updateFinProfitShareRecord(finProfitShareRecord));
    }

    @RequiresPermissions("finance:profitShare:remove")
    @Log(title = "分润记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{shareIds}")
    public AjaxResult remove(@PathVariable Long[] shareIds)
    {
        return toAjax(finProfitShareRecordService.deleteFinProfitShareRecordByShareIds(shareIds));
    }
}
