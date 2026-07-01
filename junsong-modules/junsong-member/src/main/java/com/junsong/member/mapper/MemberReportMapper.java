package com.junsong.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 会员报表 Mapper
 */
@Mapper
public interface MemberReportMapper
{
    /** 统计当前部门未删除会员总数 */
    int countTotalMembers(@Param("deptIds") List<Long> deptIds);

    /** 统计当前部门今日新增会员数 */
    int countTodayNewMembers(@Param("deptIds") List<Long> deptIds);

    /** 统计当前部门有积分/兑换/秒杀活动的活跃会员数 */
    int countActiveMembers(@Param("deptIds") List<Long> deptIds);

    /** 按日期统计新增会员趋势（每天一行） */
    List<Map<String, Object>> selectMemberGrowthTrend(@Param("deptIds") List<Long> deptIds,
                                                       @Param("startTime") String startTime,
                                                       @Param("endTime") String endTime);

    /** 按状态统计会员分布 */
    List<Map<String, Object>> selectMemberStatusStats(@Param("deptIds") List<Long> deptIds);

    /** 秒杀总场次 */
    int countTotalSeckills(@Param("deptIds") List<Long> deptIds);

    /** 秒杀参与人数（去重） */
    int countSeckillParticipants(@Param("deptIds") List<Long> deptIds);

    /** 秒杀总收入 */
    BigDecimal sumSeckillRevenue(@Param("deptIds") List<Long> deptIds);

    /** 按秒杀活动统计（名称、参与人数、收入） */
    List<Map<String, Object>> selectSeckillStats(@Param("deptIds") List<Long> deptIds);

    /** 按部门统计秒杀 */
    List<Map<String, Object>> selectSeckillDeptStats(@Param("deptIds") List<Long> deptIds);

    /** 会员经营贡献 - 会员销售总额 */
    BigDecimal sumMemberSales(@Param("deptIds") List<Long> deptIds, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /** 会员经营贡献 - 非会员销售总额 */
    BigDecimal sumNonMemberSales(@Param("deptIds") List<Long> deptIds, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /** 会员经营贡献 - 新增会员趋势 */
    List<com.junsong.member.domain.vo.MemberContributionTrendVO> selectContributionTrend(@Param("deptIds") List<Long> deptIds, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /** 会员经营贡献 - 秒杀活动贡献 */
    List<com.junsong.member.domain.vo.MemberActivityContributionVO> selectActivityContributions(@Param("deptIds") List<Long> deptIds);

    /** 会员经营贡献 - 积分兑换成本 */
    BigDecimal sumPointsRedemptionCost(@Param("deptIds") List<Long> deptIds);

    /** 会员复购数 */
    int countRepurchaseMembers(@Param("deptIds") List<Long> deptIds);

    /** R2: 会员销售笔数（remark 含 'member' 的记录数） */
    int countMemberSaleRecords(@Param("deptIds") List<Long> deptIds, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /** R2: 有 2 笔及以上会员销售的 sale_no 数（代理复购指标） */
    int countMembersWithMultipleSales(@Param("deptIds") List<Long> deptIds, @Param("startTime") String startTime, @Param("endTime") String endTime);
}
