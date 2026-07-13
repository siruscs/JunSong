package com.junsong.finance.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.junsong.finance.domain.vo.StockLedgerRowVO;
import com.junsong.finance.domain.vo.StockReportItemVO;
import com.junsong.finance.domain.vo.StockReportQuery;
import com.junsong.finance.domain.vo.StockReportSummaryVO;
import com.junsong.finance.domain.vo.StockValueReportItemVO;
import com.junsong.finance.domain.vo.StockValueReportVO;

/**
 * 经营库存报表 Mapper。
 *
 * <p>所有查询均以 {@code tenantId} 做租户隔离；门店范围由 {@link StockReportQuery#getDeptIds()}
 * 限定；日期区间使用半开区间 {@code [startDate, endDate+1day)} 以避免对
 * {@code create_time} 使用 {@code DATE()} 函数而丢失索引。</p>
 *
 * @author junsong
 */
public interface StockReportMapper {

    /**
     * 查询库存报表汇总指标（覆盖全部授权范围内商品，不受 keyword/status 过滤影响）。
     *
     * @param tenantId 租户ID
     * @param query    查询参数（deptIds / startDate / endDate）
     * @return 汇总指标
     */
    StockReportSummaryVO selectStockReportSummary(@Param("tenantId") Long tenantId,
                                                  @Param("query") StockReportQuery query);

    /**
     * 分页查询库存报表明细行（受 keyword/status 过滤）。
     *
     * @param tenantId 租户ID
     * @param query    查询参数
     * @return 当前页明细
     */
    List<StockReportItemVO> selectStockReportItems(@Param("tenantId") Long tenantId,
                                                   @Param("query") StockReportQuery query);

    /**
     * 统计库存报表明细行总数（受 keyword/status 过滤，不含分页）。
     *
     * @param tenantId 租户ID
     * @param query    查询参数
     * @return 总条数
     */
    long countStockReportItems(@Param("tenantId") Long tenantId,
                               @Param("query") StockReportQuery query);

    /**
     * 查询全部库存报表明细行（受 keyword/status 过滤，不含分页）。
     * 仅供导出使用，不应暴露给分页查询。
     *
     * @param tenantId 租户ID
     * @param query    查询参数
     * @return 全部明细
     */
    List<StockReportItemVO> selectAllStockReportItems(@Param("tenantId") Long tenantId,
                                                       @Param("query") StockReportQuery query);

    /**
     * 查询某门店某商品在指定日期区间内的库存流水明细（下钻）。
     *
     * @param tenantId  租户ID
     * @param deptId    门店ID
     * @param productId 商品ID
     * @param startDate 区间开始（含）
     * @param endDate   区间结束（含）
     * @return 流水明细列表
     */
    List<StockLedgerRowVO> selectStockLedgerRows(@Param("tenantId") Long tenantId,
                                                 @Param("deptId") Long deptId,
                                                 @Param("productId") Long productId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 查询库存价值报表汇总（第二期财务计价）。
     * 聚合 fin_stock_cost_ledger 的成本流水：
     *   期初金额 = startDate 之前所有成本变动的净累计；
     *   入库金额 = COST_IN - COST_REVERSE_OUT（区间内）；
     *   销售成本 = COST_OUT - COST_REVERSE_IN（区间内）；
     *   调整 = COST_ADJUST（区间内）；
     *   期末金额 = 期初 + 入库 - 销售成本 + 调整。
     * 销售收入从 fin_sale_record 聚合（不含赠品收入）。
     *
     * @param tenantId 租户ID
     * @param query    查询参数（deptIds / startDate / endDate）
     * @return 价值汇总；金额字段在无成本流水时为 0
     */
    StockValueReportVO selectStockValueSummary(@Param("tenantId") Long tenantId,
                                                @Param("query") StockReportQuery query);

    /**
     * 分页查询库存价值报表单商品行（第二期财务计价）。
     * 每行包含期末数量、平均单位成本、期末金额、区间入库金额、销售成本、销售收入和毛利。
     *
     * @param tenantId 租户ID
     * @param query    查询参数
     * @return 单商品价值明细
     */
    List<StockValueReportItemVO> selectStockValueItems(@Param("tenantId") Long tenantId,
                                                        @Param("query") StockReportQuery query);

    /**
     * 检查租户在指定门店范围内是否已初始化成本层。
     * 用于服务层判定 costReady 标志：无成本层行时禁止展示金额。
     *
     * @param tenantId 租户ID
     * @param deptIds  授权门店ID集合
     * @return 存在至少一行成本层记录时返回 true
     */
    boolean existsCostLayerForTenant(@Param("tenantId") Long tenantId,
                                     @Param("deptIds") List<Long> deptIds);

    /**
     * 统计授权范围内有库存流水但缺少成本层的商品数量。
     * 用于 costReady 覆盖校验：返回大于 0 表示部分商品未初始化成本层，
     * 价值报表会遗漏这些商品，costReady 应为 false。
     *
     * @param tenantId 租户ID
     * @param deptIds  授权门店ID集合
     * @return 缺少成本层的商品数量（0 表示全覆盖）
     */
    int countStockProductsWithoutCostLayer(@Param("tenantId") Long tenantId,
                                           @Param("deptIds") List<Long> deptIds);
}
