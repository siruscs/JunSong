package com.junsong.finance.mapper;

import com.junsong.finance.domain.vo.CashflowDashboardVO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 轻量现金流看板 Mapper
 *
 * @author junsong
 */
public interface CashflowDashboardMapper {

    /**
     * 查询现金流汇总（流入、流出、待结算金额与数量）
     *
     * @param deptIds   授权部门ID列表（空则不过滤）
     * @param startTime 时间范围起
     * @param endTime   时间范围止
     * @return 汇总 VO
     */
    CashflowDashboardVO selectCashflowSummary(@Param("deptIds") List<Long> deptIds,
                                             @Param("startTime") Date startTime,
                                             @Param("endTime") Date endTime);

    /**
     * 查询现金流日趋势
     *
     * @param deptIds   授权部门ID列表
     * @param startTime 时间范围起
     * @param endTime   时间范围止
     * @return 趋势行列表
     */
    List<CashflowDashboardVO.CashflowTrendRowVO> selectCashflowTrendRows(@Param("deptIds") List<Long> deptIds,
                                                                         @Param("startTime") Date startTime,
                                                                         @Param("endTime") Date endTime);

    /**
     * 查询待结算明细列表
     *
     * @param deptIds 授权部门ID列表
     * @return 待结算明细
     */
    List<CashflowDashboardVO.CashflowPendingItemVO> selectPendingItems(@Param("deptIds") List<Long> deptIds);

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return 1 存在 / 0 不存在
     */
    int checkTableExists(@Param("tableName") String tableName);
}
