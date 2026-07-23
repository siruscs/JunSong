package com.junsong.finance.task;

import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IStockSnapshotService;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import com.junsong.finance.domain.vo.FinStockPositionView;

/**
 * R21 库存每日快照任务。
 * 复用 R7-E IStockSnapshotService#rebuildDailySnapshot，按有结存的门店列表批量重建。
 * 单门店失败不掩盖其他门店结果，返回 PARTIAL。不自动调整负库存。
 */
@Component
public class StockDailySnapshotTask
{
    public static final String JOB_CODE = "R21_STOCK_DAILY_SNAPSHOT";
    public static final String JOB_NAME = "库存每日快照任务";

    @Autowired
    private IStockSnapshotService stockSnapshotService;

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    public R21TaskResult execute()
    {
        try {
            List<FinStockPositionView> scopes = finStockLedgerMapper.selectAllTenantDeptScopesWithPosition();
            if (scopes == null || scopes.isEmpty()) {
                return R21TaskResult.skipped("No stores with stock position found");
            }
            LocalDate today = LocalDate.now();
            int totalRows = 0;
            int failedStores = 0;
            String lastError = null;
            for (FinStockPositionView scope : scopes) {
                try {
                    totalRows += stockSnapshotService.rebuildDailySnapshot(scope.getTenantId(), today, scope.getDeptId());
                } catch (Exception ex) {
                    failedStores++;
                    lastError = "tenant/store " + scope.getTenantId() + "/" + scope.getDeptId() + ": " + ex.getMessage();
                }
            }
            if (failedStores > 0 && failedStores < scopes.size()) {
                return R21TaskResult.partial(totalRows,
                        "Stock snapshot partial: " + totalRows + " rows, " + failedStores + "/" + scopes.size() + " stores failed",
                        lastError);
            }
            if (failedStores > 0) {
                return R21TaskResult.failed(new RuntimeException(lastError));
            }
            if (totalRows == 0) {
                return R21TaskResult.skipped("No stock snapshot rows generated");
            }
            return R21TaskResult.success(totalRows,
                    "Generated stock snapshots: " + totalRows + " rows across " + scopes.size() + " tenant/store scopes");
        } catch (Exception ex) {
            return R21TaskResult.failed(ex);
        }
    }
}
