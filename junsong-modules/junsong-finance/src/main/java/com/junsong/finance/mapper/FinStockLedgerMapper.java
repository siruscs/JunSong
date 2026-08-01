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
    java.math.BigDecimal selectPositionQuantityForUpdate(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId, @Param("productId") Long productId);

    /**
     * 无锁查询当前库存数量（盘点冻结期望数量时使用，不持有行锁）。
     *
     * 用于盘点单创建时读取期望数量 —— 盘点单从创建到过账可能跨越数小时或数天，
     * 不应长时间持有 position 行锁。过账阶段（Task 6）才使用
     * selectPositionQuantityForUpdate 锁定行。
     *
     * @param deptId 门店ID
     * @param productId 商品ID
     * @return 当前库存，行不存在时返回 null（视为 0）
     */
    java.math.BigDecimal selectPositionQuantity(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId, @Param("productId") Long productId);

    /**
     * 更新当前库存数量。
     *
     * @param deptId 门店ID
     * @param productId 商品ID
     * @param quantity 新库存
     * @return 影响行数
     */
    int updatePositionQuantity(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId, @Param("productId") Long productId,
                               @Param("quantity") java.math.BigDecimal quantity);

    /**
     * 汇总某业务单据对某商品已记录的净流水（含正向与反向），用于差额对账。
     *
     * @param referenceType 关联单据类型
     * @param referenceId 关联单据ID
     * @param productId 商品ID
     * @return 已记录净额，无记录返回 0 或 null
     */
    java.math.BigDecimal sumRecordedNet(@Param("tenantId") Long tenantId,
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
     * 按库存流水业务幂等键查询已存在流水。
     *
     * @param tenantId 租户ID
     * @param idempotencyKey 幂等键
     * @return 已存在流水，无记录返回 null
     */
    FinStockLedger selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                          @Param("idempotencyKey") String idempotencyKey);

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

    /**
     * 更新库存流水的单位成本字段（用于销售出库先写流水后固化成本的两步流程）。
     *
     * @param ledgerId 流水ID
     * @param unitCost 固化的单位成本
     * @return 影响行数
     */
    int updateLedgerUnitCost(@Param("ledgerId") Long ledgerId, @Param("unitCost") java.math.BigDecimal unitCost);

    /**
     * 按 reference_no 查询已存在的盘点流水数量（用于盘点幂等校验）。
     *
     * @param tenantId 租户ID
     * @param referenceNo 盘点单号
     * @return 已存在流水数量
     */
    int countByReferenceNo(@Param("tenantId") Long tenantId, @Param("referenceNo") String referenceNo);

    /**
     * 汇总盘点冻结时间之后某门店某商品的库存净变动（用于过账时重算 adjustedExpected）。
     *
     * 查询 fin_stock_ledger 表中 create_time >= freezeTime 的净流水合计。
     * 盘点从创建到过账可能跨越数小时或数天，过账时需重算冻结后的变动以还原"盘点时刻的期望"。
     *
     * @param tenantId 租户ID
     * @param deptId 门店ID
     * @param productId 商品ID
     * @param freezeTime 盘点冻结时间
     * @return 净变动数量（无记录返回 0）
     */
    java.math.BigDecimal sumMovementAfterFreeze(@Param("tenantId") Long tenantId,
                                    @Param("deptId") Long deptId,
                                    @Param("productId") Long productId,
                                    @Param("freezeTime") java.util.Date freezeTime);

    /**
     * 统计某时间点之后某商品的非盘点类库存流水数量（用于冲销下游使用检查）。
     *
     * 排除 STOCK_TAKE_GAIN / STOCK_TAKE_LOSS / STOCK_TAKE_REVERSE 类型，
     * 因为这些是盘点自身的流水，不算下游使用。
     *
     * @param tenantId 租户ID
     * @param deptId 门店ID
     * @param productId 商品ID
     * @param afterTime 起始时间（盘点过账时间）
     * @return 下游流水数量
     */
    int countDownstreamLedgersAfterTime(@Param("tenantId") Long tenantId,
                                         @Param("deptId") Long deptId,
                                         @Param("productId") Long productId,
                                         @Param("afterTime") java.util.Date afterTime);
}
