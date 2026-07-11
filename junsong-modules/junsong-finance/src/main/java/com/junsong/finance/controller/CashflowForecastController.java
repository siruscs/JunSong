package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.CashflowForecastQueryParams;
import com.junsong.finance.service.ICashflowForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CashflowForecastController extends BaseController {

    @Autowired
    private ICashflowForecastService cashflowForecastService;

    @RequiresPermissions("finance:cashflowForecast:view")
    @PostMapping("/cashflow-forecast/dashboard")
    public AjaxResult dashboard(@RequestBody(required = false) CashflowForecastQueryParams params) {
        return AjaxResult.success(cashflowForecastService.getDashboard(params == null ? new CashflowForecastQueryParams() : params));
    }

    @RequiresPermissions("finance:cashflowForecast:snapshot")
    @PostMapping("/cashflow-forecast/snapshot")
    public AjaxResult snapshot(@RequestBody(required = false) CashflowForecastQueryParams params) {
        int count = cashflowForecastService.createSnapshot(params == null ? new CashflowForecastQueryParams() : params);
        return AjaxResult.success("成功生成 " + count + " 条预测快照", count);
    }
}
