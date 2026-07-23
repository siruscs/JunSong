package com.junsong.finance.service;

import java.time.LocalDate;

/**
 * 库存每日快照生成服务（R7-E 前置）。
 *
 * 数据来源为历史 fin_stock_ledger 与前一日 fin_stock_snapshot，
 * 生成可追溯的 fin_stock_snapshot 每日快照。
 * 不开放完整库存经营报表。
 *
 * @author junsong
 */
public interface IStockSnapshotService {

    /**
     * 重建某门店某日的库存快照。
     *
     * 历史日期按前一日快照和当日分类流水重放，不使用当前结存倒填。
     *
     * @param tenantId 租户ID
     * @param snapshotDate 快照日期
     * @param deptId 门店ID
     * @return 生成的快照条数
     */
    int rebuildDailySnapshot(Long tenantId, LocalDate snapshotDate, Long deptId);
}
