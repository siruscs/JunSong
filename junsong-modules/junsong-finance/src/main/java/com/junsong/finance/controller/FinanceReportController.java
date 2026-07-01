package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.service.IFinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class FinanceReportController extends BaseController {

    @Autowired
    private IFinanceReportService financeReportService;

    @RequiresPermissions("finance:report:expense")
    @PostMapping("/expense")
    public AjaxResult getExpenseReport(@RequestBody ReportQueryParams params) {
        ExpenseReportVO report = financeReportService.getExpenseReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:cost")
    @PostMapping("/cost")
    public AjaxResult getCostReport(@RequestBody ReportQueryParams params) {
        CostReportVO report = financeReportService.getCostReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profitShare")
    @PostMapping("/profitShare")
    public AjaxResult getProfitShareReport(@RequestBody ReportQueryParams params) {
        ProfitShareReportVO report = financeReportService.getProfitShareReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:sale")
    @PostMapping("/sale")
    public AjaxResult getSaleReport(@RequestBody ReportQueryParams params) {
        SaleReportVO report = financeReportService.getSaleReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profit")
    @PostMapping("/profit")
    public AjaxResult getProfitReport(@RequestBody ReportQueryParams params) {
        ProfitReportVO report = financeReportService.getProfitReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:stock")
    @PostMapping("/stock")
    public AjaxResult getStockReport(@RequestBody ReportQueryParams params) {
        StockReportVO report = financeReportService.getStockReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profit")
    @PostMapping("/profit/operating")
    public AjaxResult getOperatingProfitReport(@RequestBody ReportQueryParams params) {
        OperatingProfitReportVO report = financeReportService.getOperatingProfitReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:expense")
    @PostMapping("/expense/anomalies")
    public AjaxResult getExpenseAnomalyReport(@RequestBody ReportQueryParams params) {
        ExpenseAnomalyReportVO report = financeReportService.getExpenseAnomalyReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:sale")
    @PostMapping("/sale/operation")
    public AjaxResult getSalesOperationReport(@RequestBody ReportQueryParams params) {
        SalesOperationReportVO report = financeReportService.getSalesOperationReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profitShare")
    @PostMapping("/profitShare/settlement")
    public AjaxResult getProfitShareSettlement(@RequestBody ReportQueryParams params) {
        ProfitShareSettlementDashboardVO report = financeReportService.getProfitShareSettlement(params);
        return AjaxResult.success(report);
    }
}