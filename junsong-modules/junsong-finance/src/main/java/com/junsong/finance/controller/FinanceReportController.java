package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.service.IFinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class FinanceReportController extends BaseController {

    @Autowired
    private IFinanceReportService financeReportService;

    @PostMapping("/expense")
    public AjaxResult getExpenseReport(@RequestBody ReportQueryParams params) {
        ExpenseReportVO report = financeReportService.getExpenseReport(params);
        return AjaxResult.success(report);
    }

    @PostMapping("/cost")
    public AjaxResult getCostReport(@RequestBody ReportQueryParams params) {
        CostReportVO report = financeReportService.getCostReport(params);
        return AjaxResult.success(report);
    }

    @PostMapping("/profitShare")
    public AjaxResult getProfitShareReport(@RequestBody ReportQueryParams params) {
        ProfitShareReportVO report = financeReportService.getProfitShareReport(params);
        return AjaxResult.success(report);
    }

    @PostMapping("/sale")
    public AjaxResult getSaleReport(@RequestBody ReportQueryParams params) {
        SaleReportVO report = financeReportService.getSaleReport(params);
        return AjaxResult.success(report);
    }

    @PostMapping("/profit")
    public AjaxResult getProfitReport(@RequestBody ReportQueryParams params) {
        ProfitReportVO report = financeReportService.getProfitReport(params);
        return AjaxResult.success(report);
    }

    @PostMapping("/stock")
    public AjaxResult getStockReport(@RequestBody ReportQueryParams params) {
        StockReportVO report = financeReportService.getStockReport(params);
        return AjaxResult.success(report);
    }
}