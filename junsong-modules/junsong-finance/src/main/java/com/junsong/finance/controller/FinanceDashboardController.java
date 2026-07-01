package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.FinanceAlertVO;
import com.junsong.finance.domain.vo.FinanceReviewTaskVO;
import com.junsong.finance.domain.vo.FinanceOperationDashboardVO;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceCashflowReportService;
import com.junsong.finance.service.IFinanceReportService;
import com.junsong.finance.service.alert.FinanceAlertNotifier;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class FinanceDashboardController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(FinanceDashboardController.class);

    @Autowired
    private IFinanceReportService financeReportService;

    @Autowired
    private IFinanceCashflowReportService cashflowReportService;

    @Autowired
    private FinanceAlertNotifier alertNotifier;

    @RequiresPermissions("finance:dashboard:operation")
    @PostMapping("/operation")
    public AjaxResult getOperationDashboard(@RequestBody ReportQueryParams params) {
        FinanceOperationDashboardVO dashboard = financeReportService.getOperationDashboard(params);
        return AjaxResult.success(dashboard);
    }

    @RequiresPermissions("finance:dashboard:operation")
    @PostMapping("/cashflow")
    public AjaxResult getCashflow(@RequestBody ReportQueryParams params) {
        return AjaxResult.success(cashflowReportService.getCashflowDashboard(params));
    }

    @RequiresPermissions("finance:dashboard:alerts")
    @PostMapping("/alerts")
    public AjaxResult getAlerts(@RequestBody ReportQueryParams params) {
        List<FinanceAlertVO> alerts = financeReportService.getAlerts(params);

        // Best-effort notification for HIGH alerts — never affects the API response
        try {
            List<FinanceDiagnosisResult> highResults = new ArrayList<>();
            for (FinanceAlertVO a : alerts) {
                if ("HIGH".equals(a.getAlertLevel())) {
                    FinanceDiagnosisResult r = new FinanceDiagnosisResult();
                    r.setRuleId(a.getAlertType());
                    r.setAlertLevel(a.getAlertLevel());
                    r.setDeptId(a.getDeptId());
                    r.setTitle(a.getTitle());
                    r.setReason(a.getReason());
                    r.setTargetRoute(a.getTargetRoute());
                    highResults.add(r);
                }
            }
            if (!highResults.isEmpty()) {
                alertNotifier.notifyHighAlerts(highResults);
            }
        } catch (Exception e) {
            log.warn("预警通知发送失败，不影响接口返回", e);
        }

        return AjaxResult.success(alerts);
    }

    @RequiresPermissions("finance:dashboard:reviewTasks")
    @PostMapping("/review-tasks")
    public AjaxResult getReviewTasks(@RequestBody ReportQueryParams params) {
        List<FinanceReviewTaskVO> tasks = financeReportService.getReviewTasks(params);
        return AjaxResult.success(tasks);
    }
}
