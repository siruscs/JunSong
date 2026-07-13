package com.junsong.finance.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.junsong.finance.domain.vo.StockReconciliationRowVO;

/**
 * 库存健康检查Mapper接口。
 *
 * <p>所有方法均按租户隔离，{@code tenantId} 为必填项；{@code deptIds} 为 {@code null} 或空时表示该租户下全部门店。</p>
 *
 * @author junsong
 */
public interface StockHealthMapper {

    /** 流水表总条数（租户 + 门店范围）。 */
    Long countLedger(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /** 快照表总条数（租户 + 门店范围）。 */
    Long countSnapshot(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /** 存在负结存流水的商品数（按 dept+product 去重）。 */
    Long countNegativeStockProducts(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /**
     * 有当前库存结存(quantity != 0)但无任何库存流水的商品数（数据不一致）。
     * 正常情况下库存均由流水产生，出现该情况说明结存与流水脱钩，需 WARN。
     */
    Long countPositionsWithoutLedger(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /**
     * 昨日有结存(quantity != 0)但缺少昨日快照的门店商品数。
     * 说明每日快照未生成或未覆盖，需 WARN（R7-E 规则 SNAPSHOT_MISSING）。
     */
    Long countPositionsMissingYesterdaySnapshot(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /**
     * 当日快照存在但 closing(quantity) 与当前结存 position.quantity 不一致的门店商品数。
     * 说明快照与实时结存脱钩，需 WARN（R7-E 规则 SNAPSHOT_POSITION_MISMATCH）。
     */
    Long countSnapshotPositionMismatchToday(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    // ==================== 对账明细查询（只读） ====================

    /**
     * 结存存在(quantity != 0)但无任何流水记录的明细行。
     *
     * @param tenantId 租户ID（必填）
     * @param deptIds  门店ID列表，null/空表示全部门店
     * @return 异常明细行列表
     */
    List<StockReconciliationRowVO> findPositionsWithoutLedger(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /**
     * 流水累计 change_quantity 与结存 position.quantity 不一致的明细行。
     * 仅包含有流水记录但合计不匹配的行（与 POSITION_WITHOUT_LEDGER 互斥）。
     *
     * @param tenantId 租户ID（必填）
     * @param deptIds  门店ID列表，null/空表示全部门店
     * @return 异常明细行列表
     */
    List<StockReconciliationRowVO> findLedgerPositionMismatch(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /**
     * 快照恒等式不成立（opening + in - out != closing）的明细行。
     *
     * @param tenantId 租户ID（必填）
     * @param deptIds  门店ID列表，null/空表示全部门店
     * @return 异常明细行列表
     */
    List<StockReconciliationRowVO> findSnapshotEquationMismatch(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    /**
     * 最新快照期末结存与当前结存 position.quantity 不一致的明细行。
     *
     * @param tenantId 租户ID（必填）
     * @param deptIds  门店ID列表，null/空表示全部门店
     * @return 异常明细行列表
     */
    List<StockReconciliationRowVO> findLatestSnapshotMismatch(@Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);
}
