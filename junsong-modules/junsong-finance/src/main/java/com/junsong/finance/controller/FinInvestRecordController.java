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
import com.junsong.finance.domain.FinInvestRecord;
import com.junsong.finance.service.IFinInvestRecordService;

@RestController
@RequestMapping("/investRecord")
public class FinInvestRecordController extends BaseController
{
    @Autowired
    private IFinInvestRecordService finInvestRecordService;

    @RequiresPermissions("finance:investRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(FinInvestRecord finInvestRecord)
    {
        startPage();
        List<FinInvestRecord> list = finInvestRecordService.selectFinInvestRecordList(finInvestRecord);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:investRecord:query")
    @GetMapping("/{investId}")
    public AjaxResult getInfo(@PathVariable Long investId)
    {
        return success(finInvestRecordService.selectFinInvestRecordByInvestId(investId));
    }

    @RequiresPermissions("finance:investRecord:add")
    @Log(title = "投资来源记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinInvestRecord finInvestRecord)
    {
        finInvestRecord.setDeptId(SecurityUtils.getDeptId());
        finInvestRecord.setCreateBy(SecurityUtils.getUsername());
        return toAjax(finInvestRecordService.insertFinInvestRecord(finInvestRecord));
    }

    @RequiresPermissions("finance:investRecord:edit")
    @Log(title = "投资来源记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinInvestRecord finInvestRecord)
    {
        finInvestRecord.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finInvestRecordService.updateFinInvestRecord(finInvestRecord));
    }

    @RequiresPermissions("finance:investRecord:remove")
    @Log(title = "投资来源记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{investIds}")
    public AjaxResult remove(@PathVariable Long[] investIds)
    {
        return toAjax(finInvestRecordService.deleteFinInvestRecordByInvestIds(investIds));
    }
}
