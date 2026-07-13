package com.junsong.finance.mapper;

import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.domain.FinStockSnapshot;
import com.junsong.finance.domain.vo.DailyFlowView;
import com.junsong.finance.domain.vo.FinStockPositionView;

/**
 * 库存流水Mapper接口。
 * 采用"当前库存表 fin_stock_position 行锁 + 流水对账"模型，保证并发安全与可追溯。
 *
 * @author junsong
 */
public interface FinStockLedgerMapper {

    /**
     * 幂等创建当前库存行（INSERT IGNORE），保证后续 FOR UPDATE 一定有行可锁。
     *
     * @param deptId 门店ID
     * @param productId 商品ID
     * @return 影响行数
     */
    int insertPositionIfAbsent(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId, @Param("productId") Long productId);

    /**
     * 加行锁查询当前库存数量（SELECT ... FOR UPDATE）。
     *
     * @param deptId 门店ID
     * @param productId 商品ID
     * @return 当前库存，行不存在时返回 null
     */
    Integer selectPositionQuantityForUpdate(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId, @Param("productId") Long productId);

    /**
     * 更新当前库存数量。
     *
     * @param deptId 门店ID
     * @param productId 商品ID
     * @param quantity 新库存
     * @return 影响行数
     */
    int updatePositionQuantity(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId, @Param("productId") Long productId,
                               @Param("quantity") Integer quantity);

    /**
     * 汇总某业务单据对某商品已记录的净流水（含正向与反向），用于差额对账。
     *
     * @param referenceType 关联单据类型
     * @param referenceId 关联单据ID
     * @param productId 商品ID
     * @return 已记录净额，无记录返回 0 或 null
     */
    Integer sumRecordedNet(@Param("tenantId") Long tenantId,
                           @Param("referenceType") String referenceType,
                           @Param("referenceId") Long referenceId,
                           @Param("productId") Long productId);

    /**
     * 查询某业务单据已产生过流水的商品ID列表（去重），用于修改/删除时对账反向冲销。
     *
     * @param referenceType 关联单据类型
     * @param referenceId 关联单据ID
     * @return 商品ID列表
     */
    List<Long> selectRecordedProductIds(@Param("tenantId") Long tenantId,
                                        @Param("referenceType") String referenceType,
                                        @Param("referenceId") Long referenceId);

    /**
     * 插入一笔库存流水。
     *
     * @param ledger 库存流水
     * @return 结果
     */
    int insertFinStockLedger(FinStockLedger ledger);

    /**
     * 查询某门店全部当前结存行（含商品名称，取自最近一笔流水）。
     * 用于每日快照生成的数据来源。
     *
     * @param deptId 门店ID
     * @return 结存行列表
     */
    /**
     * 汇总某门店某商品某日的入库/出库数量（取自 fin_stock_ledger）。
     * 正向流水计入 inQuantity，反向流水绝对值计入 outQuantity。
     * 无流水时返回单行 0/0（聚合查询特性）。
     *
     * @param snapshotDate 快照日期
     * @param deptId 门店ID
     * @param productId 商品ID
     * @return 当日流水量视图
     */
    DailyFlowView sumDailyFlow(@Param("tenantId") Long tenantId,
                                @Param("snapshotDate") LocalDate snapshotDate,
                                @Param("deptId") Long deptId,
                                @Param("productId") Long productId);

    List<Long> selectSnapshotProductIds(@Param("tenantId") Long tenantId,
                                         @Param("snapshotDate") LocalDate snapshotDate,
                                         @Param("deptId") Long deptId);

    FinStockSnapshot selectPreviousSnapshot(@Param("tenantId") Long tenantId,
                                             @Param("snapshotDate") LocalDate snapshotDate,
                                             @Param("deptId") Long deptId,
                                             @Param("productId") Long productId);

    FinStockLedger selectFirstDailyLedger(@Param("tenantId") Long tenantId,
                                           @Param("snapshotDate") LocalDate snapshotDate,
                                           @Param("deptId") Long deptId,
                                           @Param("productId") Long productId);

    FinStockLedger selectLastLedgerBeforeDate(@Param("tenantId") Long tenantId,
                                               @Param("snapshotDate") LocalDate snapshotDate,
                                               @Param("deptId") Long deptId,
                                               @Param("productId") Long productId);

    /**
     * 幂等写入库存快照：基于唯一键 (tenant_id, snapshot_date, dept_id, product_id) 做 upsert。
     * 同日同门店同商品重复执行将 UPDATE 而非产生重复行。
     *
     * @param snapshot 库存快照
     * @return 影响行数（1=新增，2=更新）
     */
    int upsertSnapshot(FinStockSnapshot snapshot);

    /**
     * 查询某销售单对某商品的原 SALE_OUT 流水固化的单位成本。
     * 用于销售冲销时按原成本回补成本层。
     *
     * @param tenantId 租户ID
     * @param referenceId 销售单ID
     * @param productId 商品ID
     * @return 原固化单位成本，无记录返回 null
     */
    java.math.BigDecimal selectSaleOutUnitCost(@Param("tenantId") Long tenantId,
                                                @Param("referenceId") Long referenceId,
                                                @Param("productId") Long productId);

    /**
     * R21：查询所有有当前结存的门店ID列表（去重），供库存每日快照批量重建。
     *
     * @return 门店ID列表
     */
    List<FinStockPositionView> selectAllTenantDeptScopesWithPosition();
}
