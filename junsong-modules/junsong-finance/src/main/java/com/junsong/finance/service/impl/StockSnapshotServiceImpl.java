package com.junsong.finance.service.impl;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.finance.domain.FinStockSnapshot;
import com.junsong.finance.domain.vo.DailyFlowView;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IStockSnapshotService;

/**
 * 库存每日快照生成服务实现。
 *
 * 按历史流水顺序重放快照，历史日期不读取当前结存。
 *
 * @author junsong
 */
@Service
public class StockSnapshotServiceImpl implements IStockSnapshotService {

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuildDailySnapshot(Long tenantId, LocalDate snapshotDate, Long deptId) {
        if (tenantId == null || snapshotDate == null || deptId == null) {
            throw new IllegalArgumentException("库存快照租户、日期和门店不能为空");
        }
        List<Long> productIds = finStockLedgerMapper.selectSnapshotProductIds(tenantId, snapshotDate, deptId);
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Long productId : productIds) {
            FinStockSnapshot previous = finStockLedgerMapper.selectPreviousSnapshot(tenantId, snapshotDate, deptId, productId);
            FinStockLedger first = finStockLedgerMapper.selectFirstDailyLedger(tenantId, snapshotDate, deptId, productId);
            FinStockLedger lastBefore = previous == null && first == null
                    ? finStockLedgerMapper.selectLastLedgerBeforeDate(tenantId, snapshotDate, deptId, productId) : null;
            int opening = previous != null ? nz(previous.getQuantity())
                    : first != null ? nz(first.getBeforeQuantity())
                    : lastBefore != null ? nz(lastBefore.getAfterQuantity()) : 0;
            DailyFlowView flow = finStockLedgerMapper.sumDailyFlow(tenantId, snapshotDate, deptId, productId);
            int inQuantity = flow != null ? nz(flow.getInQuantity()) : 0;
            int outQuantity = flow != null ? nz(flow.getOutQuantity()) : 0;
            int closing = Math.subtractExact(Math.addExact(opening, inQuantity), outQuantity);

            FinStockSnapshot snapshot = new FinStockSnapshot();
            snapshot.setTenantId(tenantId);
            snapshot.setSnapshotDate(snapshotDate);
            snapshot.setDeptId(deptId);
            snapshot.setProductId(productId);
            snapshot.setProductName(first != null ? first.getProductName()
                    : previous != null ? previous.getProductName()
                    : lastBefore != null ? lastBefore.getProductName() : null);
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
