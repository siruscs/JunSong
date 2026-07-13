package com.junsong.finance.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.StockHealthIssueVO;
import com.junsong.finance.domain.vo.StockHealthVO;
import com.junsong.finance.domain.vo.StockReconciliationResultVO;
import com.junsong.finance.domain.vo.StockReconciliationRowVO;
import com.junsong.finance.mapper.StockHealthMapper;
import com.junsong.finance.service.IStockHealthService;

/**
 * 库存底座健康检查服务实现。
 *
 * 规则：
 * - BLOCKED：存在负结存流水（NEGATIVE_STOCK）。
 * - WARN：
 *   - POSITION_WITHOUT_LEDGER：结存与流水脱钩（R6 保留）。
 *   - SNAPSHOT_EMPTY：流水有数据但快照表为空（R6 保留）。
 *   - SNAPSHOT_MISSING：昨日有结存但缺少昨日快照（R7-E 新增）。
 *   - SNAPSHOT_POSITION_MISMATCH：当日快照 closing 与 position 不一致（R7-E 新增）。
 * - HEALTHY：无阻断项且无告警项。
 * BLOCKED 优先级高于 WARN。
 *
 * <p>对账方法 {@link #reconcileStock(Long, List)} 为只读，不修改任何数据。
 * 所有读写均按 (tenantId, deptId, productId) 隔离；tenantId 为 null 时 fail-closed。</p>
 *
 * @author junsong
 */
@Service
public class StockHealthServiceImpl implements IStockHealthService {

    @Autowired
    private StockHealthMapper stockHealthMapper;

    @Override
    public StockHealthVO checkHealth(Long tenantId, List<Long> deptIds) {
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止库存健康检查");
        }

        long ledgerCount = nz(stockHealthMapper.countLedger(tenantId, deptIds));
        long snapshotCount = nz(stockHealthMapper.countSnapshot(tenantId, deptIds));
        long negativeCount = nz(stockHealthMapper.countNegativeStockProducts(tenantId, deptIds));
        long positionsWithoutLedger = nz(stockHealthMapper.countPositionsWithoutLedger(tenantId, deptIds));
        long snapshotMissing = nz(stockHealthMapper.countPositionsMissingYesterdaySnapshot(tenantId, deptIds));
        long snapshotMismatch = nz(stockHealthMapper.countSnapshotPositionMismatchToday(tenantId, deptIds));

        StockHealthVO vo = new StockHealthVO();
        vo.setLedgerCount(ledgerCount);
        vo.setSnapshotCount(snapshotCount);
        vo.setNegativeStockProductCount(negativeCount);
        vo.setProductsWithoutLedgerCount(positionsWithoutLedger);
        vo.setSnapshotMissingCount(snapshotMissing);
        vo.setSnapshotMismatchCount(snapshotMismatch);

        List<StockHealthIssueVO> issues = new ArrayList<>();
        boolean blocked = false;

        if (negativeCount > 0) {
            blocked = true;
            issues.add(new StockHealthIssueVO(
                    "NEGATIVE_STOCK", "HIGH", "存在负库存流水",
                    "有 " + negativeCount + " 个门店商品出现负结存，库存底座不可信，需人工核查。"));
        }

        if (positionsWithoutLedger > 0) {
            issues.add(new StockHealthIssueVO(
                    "POSITION_WITHOUT_LEDGER", "MEDIUM", "库存结存与流水脱钩",
                    "有 " + positionsWithoutLedger + " 个商品有库存结存但缺少流水记录，需核查数据一致性。"));
        }

        if (ledgerCount > 0 && snapshotCount == 0) {
            issues.add(new StockHealthIssueVO(
                    "SNAPSHOT_EMPTY", "MEDIUM", "库存快照为空",
                    "流水表已有数据但快照表为空，库存结存尚未落快照。"));
        }

        if (snapshotMissing > 0) {
            issues.add(new StockHealthIssueVO(
                    "SNAPSHOT_MISSING", "MEDIUM", "缺少昨日库存快照",
                    "有 " + snapshotMissing + " 个门店商品昨日有结存但未生成库存快照，需核查每日快照任务是否正常运行。"));
        }

        if (snapshotMismatch > 0) {
            issues.add(new StockHealthIssueVO(
                    "SNAPSHOT_POSITION_MISMATCH", "MEDIUM", "快照与结存不一致",
                    "有 " + snapshotMismatch + " 个门店商品当日快照与当前结存数量不一致，需核查快照刷新时机与数据一致性。"));
        }

        if (blocked) {
            vo.setStatus("BLOCKED");
        } else if (!issues.isEmpty()) {
            vo.setStatus("WARN");
        } else {
            vo.setStatus("HEALTHY");
        }

        vo.setIssues(issues);
        return vo;
    }

    @Override
    public StockReconciliationResultVO reconcileStock(Long tenantId, List<Long> deptIds) {
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止库存对账");
        }

        List<StockReconciliationRowVO> rows = new ArrayList<>();
        rows.addAll(nzList(stockHealthMapper.findPositionsWithoutLedger(tenantId, deptIds)));
        rows.addAll(nzList(stockHealthMapper.findLedgerPositionMismatch(tenantId, deptIds)));
        rows.addAll(nzList(stockHealthMapper.findSnapshotEquationMismatch(tenantId, deptIds)));
        rows.addAll(nzList(stockHealthMapper.findLatestSnapshotMismatch(tenantId, deptIds)));

        Map<String, Integer> anomalyCounts = new LinkedHashMap<>();
        for (StockReconciliationRowVO row : rows) {
            anomalyCounts.merge(row.getAnomalyCode(), 1, Integer::sum);
        }

        StockReconciliationResultVO result = new StockReconciliationResultVO();
        result.setRows(rows);
        result.setTotalAnomalyCount(rows.size());
        result.setAnomalyCounts(anomalyCounts);
        result.setStatus(rows.isEmpty() ? "HEALTHY" : "WARN");
        return result;
    }

    private long nz(Long value) {
        return value != null ? value : 0L;
    }

    private List<StockReconciliationRowVO> nzList(List<StockReconciliationRowVO> value) {
        return value != null ? value : new ArrayList<>();
    }
}
