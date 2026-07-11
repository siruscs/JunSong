package com.junsong.finance.task;

import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IStockSnapshotService;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

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
            List<Long> deptIds = finStockLedgerMapper.selectAllDeptIdsWithPosition();
            if (deptIds == null || deptIds.isEmpty()) {
                return R21TaskResult.skipped("No stores with stock position found");
            }
            LocalDate today = LocalDate.now();
            int totalRows = 0;
            int failedStores = 0;
            String lastError = null;
            for (Long deptId : deptIds) {
                try {
                    totalRows += stockSnapshotService.rebuildDailySnapshot(today, deptId);
                } catch (Exception ex) {
                    failedStores++;
                    lastError = "store " + deptId + ": " + ex.getMessage();
                }
            }
            if (failedStores > 0 && failedStores < deptIds.size()) {
                return R21TaskResult.partial(totalRows,
                        "Stock snapshot partial: " + totalRows + " rows, " + failedStores + "/" + deptIds.size() + " stores failed",
                        lastError);
            }
            if (failedStores > 0) {
                return R21TaskResult.failed(new RuntimeException(lastError));
            }
            if (totalRows == 0) {
                return R21TaskResult.skipped("No stock snapshot rows generated");
            }
            return R21TaskResult.success(totalRows,
                    "Generated stock snapshots: " + totalRows + " rows across " + deptIds.size() + " stores");
        } catch (Exception ex) {
            return R21TaskResult.failed(ex);
        }
    }
}
