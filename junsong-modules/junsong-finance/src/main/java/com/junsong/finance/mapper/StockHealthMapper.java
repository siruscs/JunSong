package com.junsong.finance.mapper;

/**
 * 库存健康检查Mapper接口。
 *
 * @author junsong
 */
public interface StockHealthMapper {

    /** 流水表总条数。 */
    Long countLedger();

    /** 快照表总条数。 */
    Long countSnapshot();

    /** 存在负结存流水的商品数（按 dept+product 去重）。 */
    Long countNegativeStockProducts();

    /**
     * 有当前库存结存(quantity != 0)但无任何库存流水的商品数（数据不一致）。
     * 正常情况下库存均由流水产生，出现该情况说明结存与流水脱钩，需 WARN。
     */
    Long countPositionsWithoutLedger();

    /**
     * 昨日有结存(quantity != 0)但缺少昨日快照的门店商品数。
     * 说明每日快照未生成或未覆盖，需 WARN（R7-E 规则 SNAPSHOT_MISSING）。
     */
    Long countPositionsMissingYesterdaySnapshot();

    /**
     * 当日快照存在但 closing(quantity) 与当前结存 position.quantity 不一致的门店商品数。
     * 说明快照与实时结存脱钩，需 WARN（R7-E 规则 SNAPSHOT_POSITION_MISMATCH）。
     */
    Long countSnapshotPositionMismatchToday();
}