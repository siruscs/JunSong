package com.junsong.finance.controller;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.finance.task.CashflowForecastSnapshotTask;
import com.junsong.finance.task.OperationMemoDraftTask;
import com.junsong.finance.task.StockDailySnapshotTask;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * R21 财务调度任务内部接口（供 system 模块通过 Feign 调用）。
 */
@RestController
@RequestMapping("/finance/inner/scheduler")
public class FinanceSchedulerInnerController
{
    @Autowired
    private CashflowForecastSnapshotTask cashflowForecastSnapshotTask;

    @Autowired
    private StockDailySnapshotTask stockDailySnapshotTask;

    @Autowired
    private OperationMemoDraftTask operationMemoDraftTask;

    @InnerAuth
    @PostMapping("/cashflow-snapshot")
    public R<R21TaskResult> cashflowSnapshot(@RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(cashflowForecastSnapshotTask.execute());
    }

    @InnerAuth
    @PostMapping("/stock-snapshot")
    public R<R21TaskResult> stockSnapshot(@RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(stockDailySnapshotTask.execute());
    }

    @InnerAuth
    @PostMapping("/memo-draft")
    public R<R21TaskResult> memoDraft(@RequestParam(defaultValue = "DAILY") String periodType,
                                      @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(operationMemoDraftTask.execute(periodType));
    }
}
