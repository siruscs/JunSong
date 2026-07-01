package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;
import com.junsong.member.mapper.MemberReportMapper;
import com.junsong.member.service.IMemberReportService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberReportServiceImpl implements IMemberReportService {

    @Autowired
    private MemberReportMapper memberReportMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public MemberReportVO getMemberReport(MemberReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        MemberReportVO vo = new MemberReportVO();
        vo.setTotalMemberCount(memberReportMapper.countTotalMembers(deptIds));
        vo.setTodayNewMemberCount(memberReportMapper.countTodayNewMembers(deptIds));
        vo.setActiveMemberCount(memberReportMapper.countActiveMembers(deptIds));

        String startTime = formatDate(params.getStartTime());
        String endTime = formatDate(params.getEndTime());
        vo.setMemberGrowthStats(memberReportMapper.selectMemberGrowthTrend(deptIds, startTime, endTime));
        vo.setMemberTypeStats(memberReportMapper.selectMemberStatusStats(deptIds));
        return vo;
    }

    @Override
    public SeckillReportVO getSeckillReport(MemberReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        SeckillReportVO vo = new SeckillReportVO();
        vo.setTotalSeckillCount(memberReportMapper.countTotalSeckills(deptIds));
        vo.setTotalParticipantCount(memberReportMapper.countSeckillParticipants(deptIds));
        vo.setTotalRevenue(memberReportMapper.sumSeckillRevenue(deptIds));
        vo.setSeckillStats(memberReportMapper.selectSeckillStats(deptIds));
        vo.setSeckillDeptStats(memberReportMapper.selectSeckillDeptStats(deptIds));
        return vo;
    }

    @Override
    public com.junsong.member.domain.vo.MemberContributionReportVO getContributionReport(com.junsong.member.domain.vo.MemberReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();
        String startTime = formatDate(params.getStartTime());
        String endTime = formatDate(params.getEndTime());

        com.junsong.member.domain.vo.MemberContributionReportVO vo = new com.junsong.member.domain.vo.MemberContributionReportVO();
        vo.setNewMemberCount(memberReportMapper.countTodayNewMembers(deptIds));
        vo.setActiveMemberCount(memberReportMapper.countActiveMembers(deptIds));
        vo.setRepurchaseCount(memberReportMapper.countRepurchaseMembers(deptIds));

        BigDecimal memberSales = memberReportMapper.sumMemberSales(deptIds, startTime, endTime);
        BigDecimal nonMemberSales = memberReportMapper.sumNonMemberSales(deptIds, startTime, endTime);
        BigDecimal totalSales = memberSales.add(nonMemberSales);
        vo.setMemberSales(memberSales != null ? memberSales : BigDecimal.ZERO);
        vo.setNonMemberSales(nonMemberSales != null ? nonMemberSales : BigDecimal.ZERO);
        vo.setMemberSalesRatio(totalSales.compareTo(BigDecimal.ZERO) > 0
                ? vo.getMemberSales().multiply(new BigDecimal("100")).divide(totalSales, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // R2: 会员销售笔数 & 平均客单价
        int memberSaleCount = memberReportMapper.countMemberSaleRecords(deptIds, startTime, endTime);
        vo.setMemberSaleCount(memberSaleCount);
        vo.setAvgMemberSaleAmount(memberSaleCount > 0
                ? vo.getMemberSales().divide(new BigDecimal(memberSaleCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // R2: 新会员首购率（代理指标：会员销售笔数 / 新增会员数 * 100，上限 100%）
        int newMemberCount = vo.getNewMemberCount();
        if (newMemberCount > 0) {
            BigDecimal rate = new BigDecimal(memberSaleCount)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(newMemberCount), 2, java.math.RoundingMode.HALF_UP);
            vo.setNewMemberFirstPurchaseRate(rate.min(new BigDecimal("100")));
        }

        // R2: 复购会员数 & 复购率
        int repeatPurchaseCount = memberReportMapper.countMembersWithMultipleSales(deptIds, startTime, endTime);
        vo.setRepeatPurchaseCount(repeatPurchaseCount);
        if (memberSaleCount > 0) {
            vo.setRepeatPurchaseRate(new BigDecimal(repeatPurchaseCount)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(memberSaleCount), 2, java.math.RoundingMode.HALF_UP));
        }

        // R2: 口径说明
        vo.setDataNote("会员销售通过 fin_sale_record.remark 含 'member' 识别，非直接 member_id 关联；"
                + "首购率为代理指标（会员销售笔数/新增会员数），复购率为 sale_no 维度 2 笔及以上占比；"
                + "积分兑换成本按 mem_points_goods.goods_value * quantity 计算，不等同现金费用；"
                + "秒杀成本当前未录入（=0），秒杀利润 = 秒杀收入。");

        BigDecimal pointsCost = memberReportMapper.sumPointsRedemptionCost(deptIds);
        vo.setPointsRedemptionCost(pointsCost != null ? pointsCost : BigDecimal.ZERO);

        vo.setTrends(memberReportMapper.selectContributionTrend(deptIds, startTime, endTime));
        vo.setActivityContributions(memberReportMapper.selectActivityContributions(deptIds));

        // Seckill totals
        BigDecimal seckillRevenue = memberReportMapper.sumSeckillRevenue(deptIds);
        vo.setSeckillSales(seckillRevenue != null ? seckillRevenue : BigDecimal.ZERO);
        vo.setSeckillCost(BigDecimal.ZERO);
        vo.setSeckillProfit(vo.getSeckillSales());

        return vo;
    }

    private String formatDate(java.util.Date date) {
        if (date == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * 哨兵部门 ID：非 admin 且无任何授权部门时使用，
     * 保证 Mapper 的 IN (-1) 永远匹配不到真实数据，避免全量泄露。
     */
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    private void applyDataScope(MemberReportQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long currentDeptId = SecurityUtils.getDeptId();
            allowed = currentDeptId != null ? Collections.singletonList(currentDeptId) : SENTINEL_DEPT_IDS;
        }
        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            params.setDeptIds(new ArrayList<>(allowed));
            return;
        }
        List<Long> finalAllowed = allowed;
        List<Long> filtered = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());
        params.setDeptIds(filtered.isEmpty() ? new ArrayList<>(allowed) : filtered);
    }

    private List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(SysDept::getDeptId)
                    .filter(deptId -> deptId != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
