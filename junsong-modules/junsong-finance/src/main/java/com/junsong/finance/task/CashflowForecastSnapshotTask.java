package com.junsong.finance.task;

import com.junsong.finance.domain.vo.CashflowForecastQueryParams;
import com.junsong.finance.service.ICashflowForecastService;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * R21 现金流预测快照任务。
 * 复用 R16 ICashflowForecastService#createSnapshot，不新增预测算法。
 */
@Component
public class CashflowForecastSnapshotTask
{
    public static final String JOB_CODE = "R21_CASHFLOW_FORECAST_SNAPSHOT";
    public static final String JOB_NAME = "现金流预测快照任务";

    @Autowired
    private ICashflowForecastService cashflowForecastService;

    /**
     * 执行现金流预测快照。
     *
     * @return 任务执行结果（SUCCESS/SKIPPED/FAILED）
     */
    public R21TaskResult execute()
    {
        try {
            CashflowForecastQueryParams params = new CashflowForecastQueryParams();
            int rows = cashflowForecastService.createSnapshot(params);
            if (rows == 0) {
                return R21TaskResult.skipped("No cashflow forecast snapshot rows generated");
            }
            return R21TaskResult.success(rows, "Generated cashflow forecast snapshots: " + rows);
        } catch (Exception ex) {
            return R21TaskResult.failed(ex);
        }
    }
}
