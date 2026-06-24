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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.service.IFinInvestorPaymentService;

@RestController
@RequestMapping("/investorPayment")
public class FinInvestorPaymentController extends BaseController
{
    @Autowired
    private IFinInvestorPaymentService finInvestorPaymentService;

    @RequiresPermissions("finance:investorPayment:list")
    @GetMapping("/list")
    public TableDataInfo list(FinInvestorPayment finInvestorPayment)
    {
        startPage();
        List<FinInvestorPayment> list = finInvestorPaymentService.selectFinInvestorPaymentList(finInvestorPayment);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:investorPayment:query")
    @GetMapping("/summary")
    public AjaxResult getSummary(@RequestParam(value = "deptId", required = false) Long deptId)
    {
        return success(finInvestorPaymentService.getInvestorPaymentSummary(deptId));
    }

    @RequiresPermissions("finance:investorPayment:export")
    @Log(title = "投资人返款", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinInvestorPayment finInvestorPayment)
    {
        List<FinInvestorPayment> list = finInvestorPaymentService.selectFinInvestorPaymentList(finInvestorPayment);
        ExcelUtil<FinInvestorPayment> util = new ExcelUtil<FinInvestorPayment>(FinInvestorPayment.class);
        util.exportExcel(response, list, "投资人返款数据");
    }

    @RequiresPermissions("finance:investorPayment:query")
    @GetMapping(value = "/{paymentId}")
    public AjaxResult getInfo(@PathVariable Long paymentId)
    {
        return success(finInvestorPaymentService.selectFinInvestorPaymentByPaymentId(paymentId));
    }

    @RequiresPermissions("finance:investorPayment:add")
    @Log(title = "投资人返款", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinInvestorPayment finInvestorPayment)
    {
        finInvestorPayment.setDeptId(SecurityUtils.getDeptId());
        if (!finInvestorPaymentService.checkPaymentNoUnique(finInvestorPayment))
        {
            return error("新增投资人返款'" + finInvestorPayment.getPaymentNo() + "'失败，返款单号已存在");
        }
        return toAjax(finInvestorPaymentService.insertFinInvestorPayment(finInvestorPayment));
    }

    @RequiresPermissions("finance:investorPayment:edit")
    @Log(title = "投资人返款", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinInvestorPayment finInvestorPayment)
    {
        finInvestorPayment.setDeptId(SecurityUtils.getDeptId());
        if (!finInvestorPaymentService.checkPaymentNoUnique(finInvestorPayment))
        {
            return error("修改投资人返款'" + finInvestorPayment.getPaymentNo() + "'失败，返款单号已存在");
        }
        return toAjax(finInvestorPaymentService.updateFinInvestorPayment(finInvestorPayment));
    }

    @RequiresPermissions("finance:investorPayment:remove")
    @Log(title = "投资人返款", businessType = BusinessType.DELETE)
	@DeleteMapping("/{paymentIds}")
    public AjaxResult remove(@PathVariable Long[] paymentIds)
    {
        return toAjax(finInvestorPaymentService.deleteFinInvestorPaymentByPaymentIds(paymentIds));
    }
}
