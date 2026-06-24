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
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinInvestRecord;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinSalePayment;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.vo.AccountingPeriodDetailVO;
import com.junsong.finance.service.IFinAdvanceService;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinExpenseService;
import com.junsong.finance.service.IFinInvestRecordService;
import com.junsong.finance.service.IFinInvestorPaymentService;
import com.junsong.finance.mapper.FinProfitShareDetailMapper;
import com.junsong.finance.service.IFinProfitShareRecordService;
import com.junsong.finance.service.IFinPurchaseService;
import com.junsong.finance.service.IFinSalePaymentService;
import com.junsong.finance.service.IFinSaleRecordService;

@RestController
@RequestMapping("/accountingPeriod")
public class FinAccountingPeriodController extends BaseController
{
    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    @Autowired
    private IFinExpenseService finExpenseService;

    @Autowired
    private IFinAdvanceService finAdvanceService;

    @Autowired
    private IFinPurchaseService finPurchaseService;

    @Autowired
    private IFinInvestRecordService finInvestRecordService;

    @Autowired
    private IFinSaleRecordService finSaleRecordService;

    @Autowired
    private IFinSalePaymentService finSalePaymentService;

    @Autowired
    private IFinProfitShareRecordService finProfitShareRecordService;

    @Autowired
    private FinProfitShareDetailMapper finProfitShareDetailMapper;

    @Autowired
    private IFinInvestorPaymentService finInvestorPaymentService;

    @RequiresPermissions("finance:accountingPeriod:list")
    @GetMapping("/list")
    public TableDataInfo list(FinAccountingPeriod finAccountingPeriod)
    {
        startPage();
        List<FinAccountingPeriod> list = finAccountingPeriodService.selectFinAccountingPeriodList(finAccountingPeriod);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:accountingPeriod:query")
    @GetMapping("/current")
    public AjaxResult current()
    {
        Long deptId = SecurityUtils.getDeptId();
        return success(finAccountingPeriodService.selectCurrentPeriodByDeptId(deptId));
    }

    @RequiresPermissions("finance:accountingPeriod:query")
    @GetMapping("/current/{deptId}")
    public AjaxResult current(@PathVariable Long deptId)
    {
        return success(finAccountingPeriodService.selectCurrentPeriodByDeptId(deptId));
    }

    @RequiresPermissions("finance:accountingPeriod:query")
    @GetMapping("/{periodId}")
    public AjaxResult getInfo(@PathVariable Long periodId)
    {
        return success(finAccountingPeriodService.selectFinAccountingPeriodByPeriodId(periodId));
    }

    @RequiresPermissions("finance:accountingPeriod:query")
    @GetMapping("/detail/{periodId}")
    public AjaxResult detail(@PathVariable Long periodId)
    {
        FinAccountingPeriod period = finAccountingPeriodService.selectFinAccountingPeriodByPeriodId(periodId);
        if (period == null)
        {
            return error("核算周期不存在");
        }

        AccountingPeriodDetailVO detail = new AccountingPeriodDetailVO();
        detail.setPeriod(period);

        FinExpense expenseQuery = new FinExpense();
        expenseQuery.setDeptId(period.getDeptId());
        expenseQuery.setPeriodId(period.getPeriodId());
        detail.setExpenses(finExpenseService.selectFinExpenseList(expenseQuery));

        FinAdvance advanceQuery = new FinAdvance();
        advanceQuery.setDeptId(period.getDeptId());
        advanceQuery.setPeriodId(period.getPeriodId());
        detail.setAdvances(finAdvanceService.selectFinAdvanceList(advanceQuery));

        FinPurchase purchaseQuery = new FinPurchase();
        purchaseQuery.setDeptId(period.getDeptId());
        purchaseQuery.setPeriodId(period.getPeriodId());
        detail.setPurchases(finPurchaseService.selectFinPurchaseList(purchaseQuery));

        FinInvestRecord investQuery = new FinInvestRecord();
        investQuery.setDeptId(period.getDeptId());
        investQuery.setPeriodId(period.getPeriodId());
        detail.setInvestRecords(finInvestRecordService.selectFinInvestRecordList(investQuery));

        FinSaleRecord saleQuery = new FinSaleRecord();
        saleQuery.setDeptId(period.getDeptId());
        saleQuery.setPeriodId(period.getPeriodId());
        detail.setSales(finSaleRecordService.selectFinSaleRecordList(saleQuery));

        FinSalePayment salePaymentQuery = new FinSalePayment();
        salePaymentQuery.setDeptId(period.getDeptId());
        salePaymentQuery.setPeriodId(period.getPeriodId());
        detail.setSalePayments(finSalePaymentService.selectFinSalePaymentList(salePaymentQuery));

        FinProfitShareRecord shareQuery = new FinProfitShareRecord();
        shareQuery.setDeptId(period.getDeptId());
        shareQuery.setPeriodId(period.getPeriodId());
        List<FinProfitShareRecord> profitShares = finProfitShareRecordService.selectFinProfitShareRecordList(shareQuery);
        for (FinProfitShareRecord share : profitShares)
        {
            share.setDetails(finProfitShareDetailMapper.selectFinProfitShareDetailByShareId(share.getShareId()));
        }
        detail.setProfitShares(profitShares);

        FinInvestorPayment paymentQuery = new FinInvestorPayment();
        paymentQuery.setDeptId(period.getDeptId());
        paymentQuery.setPeriodId(period.getPeriodId());
        detail.setInvestorPayments(finInvestorPaymentService.selectFinInvestorPaymentList(paymentQuery));

        return success(detail);
    }

    @RequiresPermissions("finance:accountingPeriod:init")
    @Log(title = "财务核算周期-初始化", businessType = BusinessType.INSERT)
    @PostMapping("/current/{deptId}/init")
    public AjaxResult initCurrent(@PathVariable Long deptId)
    {
        return success(finAccountingPeriodService.initCurrentPeriod(deptId));
    }

    @RequiresPermissions("finance:accountingPeriod:query")
    @Log(title = "财务核算周期-试算回本", businessType = BusinessType.UPDATE)
    @PostMapping("/current/{deptId}/trialBreakEven")
    public AjaxResult trialBreakEven(@PathVariable Long deptId)
    {
        return success(finAccountingPeriodService.trialBreakEven(deptId));
    }

    @RequiresPermissions("finance:accountingPeriod:carryForward")
    @Log(title = "财务核算周期-结转", businessType = BusinessType.UPDATE)
    @PostMapping("/current/{deptId}/carryForward")
    public AjaxResult carryForward(@PathVariable Long deptId)
    {
        return success(finAccountingPeriodService.carryForward(deptId));
    }

    @RequiresPermissions("finance:accountingPeriod:rollback")
    @Log(title = "财务核算周期-结转回退", businessType = BusinessType.UPDATE)
    @PostMapping("/current/{deptId}/rollbackCarryForward")
    public AjaxResult rollbackCarryForward(@PathVariable Long deptId)
    {
        return success(finAccountingPeriodService.rollbackCarryForward(deptId));
    }

    @RequiresPermissions("finance:accountingPeriod:add")
    @Log(title = "财务核算周期", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinAccountingPeriod finAccountingPeriod)
    {
        finAccountingPeriod.setCreateBy(SecurityUtils.getUsername());
        return toAjax(finAccountingPeriodService.insertFinAccountingPeriod(finAccountingPeriod));
    }

    @RequiresPermissions("finance:accountingPeriod:edit")
    @Log(title = "财务核算周期", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinAccountingPeriod finAccountingPeriod)
    {
        finAccountingPeriod.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finAccountingPeriodService.updateFinAccountingPeriod(finAccountingPeriod));
    }

    @RequiresPermissions("finance:accountingPeriod:remove")
    @Log(title = "财务核算周期", businessType = BusinessType.DELETE)
    @DeleteMapping("/{periodIds}")
    public AjaxResult remove(@PathVariable Long[] periodIds)
    {
        return toAjax(finAccountingPeriodService.deleteFinAccountingPeriodByPeriodIds(periodIds));
    }
}
