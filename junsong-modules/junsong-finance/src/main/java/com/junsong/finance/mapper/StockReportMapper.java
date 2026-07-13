package com.junsong.finance.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.junsong.finance.domain.vo.StockLedgerRowVO;
import com.junsong.finance.domain.vo.StockReportItemVO;
import com.junsong.finance.domain.vo.StockReportQuery;
import com.junsong.finance.domain.vo.StockReportSummaryVO;

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
}
