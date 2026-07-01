package com.junsong.member.service.impl;

import com.junsong.member.domain.vo.MemberPointsOperationSummaryVO;
import com.junsong.member.domain.vo.MemberPointsRiskRowVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 积分经营摘要服务单测：验证负债估算、高积分会员排序和手机号脱敏。
 * 使用手写 fake（子类覆盖 package-private 查询方法），不使用 Mockito。
 */
class MemberPointsOperationServiceImplTest {

    private TestableMemberPointsOperationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TestableMemberPointsOperationServiceImpl();
    }

    // ── 纯逻辑：负债估算 ──

    @Test
    void computeLiability_100PointsEquals1Yuan() {
        assertEquals(new BigDecimal("1.00"), service.computeLiability(100));
    }

    @Test
    void computeLiability_5000PointsEquals50Yuan() {
        assertEquals(new BigDecimal("50.00"), service.computeLiability(5000));
    }

    @Test
    void computeLiability_zeroPointsReturnsZero() {
        assertEquals(BigDecimal.ZERO, service.computeLiability(0));
    }

    @Test
    void computeLiability_negativePointsReturnsZero() {
        assertEquals(BigDecimal.ZERO, service.computeLiability(-100));
    }

    @Test
    void computeLiability_roundsToTwoDecimals() {
        // 150 points = 1.50 yuan
        assertEquals(new BigDecimal("1.50"), service.computeLiability(150));
    }

    // ── 纯逻辑：手机号脱敏 ──

    @Test
    void maskPhone_11DigitsMasksMiddle4() {
        assertEquals("138****8888", service.maskPhone("13812348888"));
    }

    @Test
    void maskPhone_nullReturnsEmpty() {
        assertEquals("", service.maskPhone(null));
    }

    @Test
    void maskPhone_shortNumberReturnsEmpty() {
        assertEquals("", service.maskPhone("12345"));
    }

    // ── 汇总：整合查询结果 ──

    @Test
    void summary_combinesAllMetrics() {
        service.totalAvailablePoints = 30000L;  // 300.00 yuan
        service.redeemedCost = new BigDecimal("1200.00");
        service.highPointsMembers = Arrays.asList(
                makeRiskRow(1L, "M001", "张三", "13800001111", 5000L),
                makeRiskRow(2L, "M002", "李四", "13900002222", 3000L)
        );

        MemberPointsOperationSummaryVO vo = service.getPointsOperationSummary(null);

        assertEquals(30000L, vo.getTotalAvailablePoints());
        assertEquals(new BigDecimal("300.00"), vo.getEstimatedPointsLiabilityAmount());
        assertEquals(new BigDecimal("1200.00"), vo.getRedeemedCostAmount());
        assertEquals(2L, vo.getHighPointsMemberCount());
        assertEquals("100积分=1元，仅用于经营估算", vo.getPointsLiabilityFormula());
    }

    @Test
    void highPointsMembers_sortedByAvailablePointsDesc() {
        // Service returns them in the order from queryHighPointsMembers (already sorted by SQL)
        service.highPointsMembers = Arrays.asList(
                makeRiskRow(1L, "M001", "高积分", "13800001111", 8000L),
                makeRiskRow(2L, "M002", "中积分", "13900002222", 3000L),
                makeRiskRow(3L, "M003", "低积分", "13700003333", 1500L)
        );

        MemberPointsOperationSummaryVO vo = service.getPointsOperationSummary(null);

        List<MemberPointsRiskRowVO> members = vo.getHighPointsMembers();
        assertEquals(3, members.size());
        // Verify descending order
        assertTrue(members.get(0).getAvailablePoints() >= members.get(1).getAvailablePoints());
        assertTrue(members.get(1).getAvailablePoints() >= members.get(2).getAvailablePoints());
    }

    @Test
    void highPointsMembers_phoneMasked() {
        service.highPointsMembers = Collections.singletonList(
                makeRiskRow(1L, "M001", "张三", "13812348888", 5000L)
        );

        MemberPointsOperationSummaryVO vo = service.getPointsOperationSummary(null);

        assertEquals("138****8888", vo.getHighPointsMembers().get(0).getMaskedPhone());
    }

    @Test
    void summary_emptyDataReturnsZeros() {
        service.totalAvailablePoints = 0L;
        service.redeemedCost = BigDecimal.ZERO;
        service.highPointsMembers = Collections.emptyList();

        MemberPointsOperationSummaryVO vo = service.getPointsOperationSummary(null);

        assertEquals(0L, vo.getTotalAvailablePoints());
        assertEquals(BigDecimal.ZERO, vo.getEstimatedPointsLiabilityAmount());
        assertEquals(BigDecimal.ZERO, vo.getRedeemedCostAmount());
        assertEquals(0L, vo.getHighPointsMemberCount());
        assertTrue(vo.getHighPointsMembers().isEmpty());
    }

    @Test
    void summary_eachMemberHasEstimatedLiability() {
        service.highPointsMembers = Collections.singletonList(
                makeRiskRow(1L, "M001", "张三", "13800001111", 5000L)
        );

        MemberPointsOperationSummaryVO vo = service.getPointsOperationSummary(null);

        // 5000 points = 50.00 yuan
        assertEquals(new BigDecimal("50.00"),
                vo.getHighPointsMembers().get(0).getEstimatedLiability());
    }

    // ── Helper ──

    private MemberPointsRiskRowVO makeRiskRow(Long id, String no, String name,
                                               String phone, long points) {
        MemberPointsRiskRowVO row = new MemberPointsRiskRowVO();
        row.setMemberId(id);
        row.setMemberNo(no);
        row.setMemberName(name);
        row.setMaskedPhone(service.maskPhone(phone));
        row.setAvailablePoints(points);
        row.setEstimatedLiability(service.computeLiability(points));
        return row;
    }

    // ── Testable subclass ──

    static class TestableMemberPointsOperationServiceImpl extends MemberPointsOperationServiceImpl {
        long totalAvailablePoints = 0L;
        BigDecimal redeemedCost = BigDecimal.ZERO;
        List<MemberPointsRiskRowVO> highPointsMembers = Collections.emptyList();

        @Override
        List<Long> loadAllowedDeptIds() {
            return Collections.emptyList(); // admin = no restriction
        }

        @Override
        long queryTotalAvailablePoints(List<Long> deptIds) {
            return totalAvailablePoints;
        }

        @Override
        BigDecimal queryRedeemedCost(List<Long> deptIds) {
            return redeemedCost;
        }

        @Override
        List<MemberPointsRiskRowVO> queryHighPointsMembers(List<Long> deptIds) {
            return new ArrayList<>(highPointsMembers);
        }
    }
}
