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

    // ==================== MP 同步：多租户 + 多部门聚合查询 ====================

    /**
     * 会员模块聚合统计（按租户 + 部门范围）。
     * 返回字段：totalMembers, todayMembers, activeMembers, silentMembers
     *
     * 活跃会员：近 30 天内有积分记录/兑换/秒杀记录的会员
     * 沉默会员：超过 30 天无上述行为的会员
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表（空列表表示不限）
     * @return 4 个会员统计指标
     */
    Map<String, Object> queryMemberOverview(@Param("tenantId") Long tenantId,
                                             @Param("deptIds") List<Long> deptIds);

    /**
     * 成长体系聚合统计（按租户 + 部门范围）。
     * 返回字段：todaySignInCount, avgGrowthValue, pendingGrowthActions,
     * completedGrowthActions, totalGrowthActions
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表
     * @return 5 个成长体系指标
     */
    Map<String, Object> queryGrowthOverview(@Param("tenantId") Long tenantId,
                                             @Param("deptIds") List<Long> deptIds);

    /**
     * 积分运营聚合统计（按租户 + 部门范围）。
     * 返回字段：totalAvailablePoints, todayPointsIssued, todayPointsConsumed,
     * pendingExchangeCount
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表
     * @return 4 个积分运营指标
     */
    Map<String, Object> queryPointsOverview(@Param("tenantId") Long tenantId,
                                             @Param("deptIds") List<Long> deptIds);

    /**
     * 会员等级分布（按 mem_member.card_type 分组）。
     * 返回字段：levelName, count
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表
     * @return 等级分布列表
     */
    List<Map<String, Object>> queryLevelDistribution(@Param("tenantId") Long tenantId,
                                                      @Param("deptIds") List<Long> deptIds);

    /**
     * 会员分层分布（按活跃度即时计算）。
     * 返回字段：segmentName, count
     * 分层口径：高价值（成长值>=1000）、活跃（30天内有行为）、沉默（30天无行为）
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表
     * @return 分层分布列表
     */
    List<Map<String, Object>> querySegmentDistribution(@Param("tenantId") Long tenantId,
                                                        @Param("deptIds") List<Long> deptIds);

    /**
     * 活动表现聚合统计（按租户 + 部门范围）。
     * 返回字段：activeSeckillCount, todayActivityMembers, todayActivityAmount
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表
     * @return 3 个活动指标
     */
    Map<String, Object> queryActivityOverview(@Param("tenantId") Long tenantId,
                                               @Param("deptIds") List<Long> deptIds);

    /**
     * 财务今日聚合统计（按租户 + 部门范围）。
     * 返回字段：todaySale, todayExpense, unverifiedExpense, unverifiedAdvance
     *
     * @param tenantId 租户ID
     * @param deptIds  授权部门ID列表
     * @return 4 个财务指标
     */
    Map<String, Object> queryFinanceOverview(@Param("tenantId") Long tenantId,
                                              @Param("deptIds") List<Long> deptIds);
}
