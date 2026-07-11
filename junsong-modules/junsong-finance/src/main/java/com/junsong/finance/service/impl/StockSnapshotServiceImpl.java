package com.junsong.finance.service.impl;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.finance.domain.FinStockSnapshot;
import com.junsong.finance.domain.vo.DailyFlowView;
import com.junsong.finance.domain.vo.FinStockPositionView;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IStockSnapshotService;

/**
 * 库存每日快照生成服务实现。
 *
 * 采用"position 驱动 + 当日流水补充 + 幂等 upsert"模型：
 * - closing = position.quantity（当前结存）
 * - in/out = fin_stock_ledger 当日正向/反向流水合计
 * - opening = closing - in + out（由 closing 反推）
 * - upsert 基于唯一键 (snapshot_date, dept_id, product_id)，重跑不产生重复行
 *
 * @author junsong
 */
@Service
public class StockSnapshotServiceImpl implements IStockSnapshotService {

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuildDailySnapshot(LocalDate snapshotDate, Long deptId) {
        List<FinStockPositionView> positions = finStockLedgerMapper.selectPositionsByDept(deptId);
        if (positions == null || positions.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (FinStockPositionView position : positions) {
            DailyFlowView flow = finStockLedgerMapper.sumDailyFlow(snapshotDate, deptId, position.getProductId());
            int inQuantity = flow != null ? nz(flow.getInQuantity()) : 0;
            int outQuantity = flow != null ? nz(flow.getOutQuantity()) : 0;
            int closing = nz(position.getQuantity());
            int opening = closing - inQuantity + outQuantity;

            FinStockSnapshot snapshot = new FinStockSnapshot();
            snapshot.setSnapshotDate(snapshotDate);
            snapshot.setDeptId(deptId);
            snapshot.setProductId(position.getProductId());
            snapshot.setProductName(position.getProductName());
            snapshot.setQuantity(closing);
            snapshot.setOpeningQuantity(opening);
            snapshot.setInQuantity(inQuantity);
            snapshot.setOutQuantity(outQuantity);
            finStockLedgerMapper.upsertSnapshot(snapshot);
            count++;
        }
        return count;
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }
}
