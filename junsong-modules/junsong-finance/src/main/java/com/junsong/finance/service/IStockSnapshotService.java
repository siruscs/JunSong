package com.junsong.finance.service;

import java.time.LocalDate;

/**
 * 库存每日快照生成服务（R7-E 前置）。
 *
 * 数据来源为当前结存表 fin_stock_position，结合 fin_stock_ledger 当日流水，
 * 生成 fin_stock_snapshot 每日快照，为 R8 开放库存经营报表做底座准备。
 * 不开放完整库存经营报表。
 *
 * @author junsong
 */
public interface IStockSnapshotService {

    /**
     * 重建某门店某日的库存快照。
     *
     * 逻辑：
     * 1. 数据来源 fin_stock_position（当前结存表）
     * 2. closing = position.quantity
     * 3. in/out 取自 fin_stock_ledger 当日流水
     * 4. opening = closing - in + out
     * 5. 幂等 upsert：同日同门店同商品已有快照则 UPDATE，不产生重复行
     * 6. 无 position 返回 0
     *
     * @param snapshotDate 快照日期
     * @param deptId 门店ID
     * @return 生成的快照条数（等于该门店的 position 行数）
     */
    int rebuildDailySnapshot(LocalDate snapshotDate, Long deptId);
}
