package com.junsong.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface MemMpDashboardMapper {

    long queryCount(@Param("deptId") Long deptId, @Param("metric") String metric);

    BigDecimal queryDecimal(@Param("deptId") Long deptId, @Param("metric") String metric);

    BigDecimal queryDecimalWithDate(@Param("deptId") Long deptId, @Param("date") String date, @Param("metric") String metric);

    /**
     * 批量查询趋势数据（一次查询返回日期范围内每天的汇总）
     *
     * @param deptId    部门ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd，含当天次日）
     * @return 每天一行，包含 stat_date/new_members/daily_expense/daily_sale
     */
    List<Map<String, Object>> queryTrendBatch(@Param("deptId") Long deptId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);
}
