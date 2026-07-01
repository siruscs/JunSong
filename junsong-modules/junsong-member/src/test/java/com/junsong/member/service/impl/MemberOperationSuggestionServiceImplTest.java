package com.junsong.member.service.impl;

import com.junsong.member.domain.vo.MemberOperationMetrics;
import com.junsong.member.domain.vo.MemberOperationSuggestionVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会员经营建议服务单测：每条规则至少一个用例，并验证健康态不生成假建议。
 */
class MemberOperationSuggestionServiceImplTest {

    private final MemberOperationSuggestionServiceImpl service = new MemberOperationSuggestionServiceImpl();

    private List<String> ruleCodes(List<MemberOperationSuggestionVO> list) {
        return list.stream().map(MemberOperationSuggestionVO::getRuleCode).collect(Collectors.toList());
    }

    private MemberOperationMetrics baseHealthy() {
        MemberOperationMetrics m = new MemberOperationMetrics();
        m.setDeptId(100L);
        m.setTotalMemberCount(100);
        m.setNewMemberCount(10);
        m.setActiveMemberCount(60);
        m.setSilentMemberCount(20);
        m.setHighValueMemberCount(15);
        m.setFirstPurchaseRate(new BigDecimal("35"));
        m.setRepurchaseRate30d(new BigDecimal("25"));
        m.setPointsLiabilityAmount(new BigDecimal("500"));
        m.setActivityRoiAvailable(true);
        return m;
    }

    @Test
    void nullMetricsReturnsEmptyList() {
        assertTrue(service.generateSuggestions(null).isEmpty());
    }

    @Test
    void healthyMetricsProduceNoSuggestions() {
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(baseHealthy());
        assertTrue(list.isEmpty(), "健康态不应生成建议，实际: " + ruleCodes(list));
    }

    @Test
    void silentMemberHighTriggeredWhenSilentRatioAbove30() {
        MemberOperationMetrics m = baseHealthy();
        m.setSilentMemberCount(40); // 40%
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertTrue(ruleCodes(list).contains("SILENT_MEMBER_HIGH"));
        MemberOperationSuggestionVO vo = list.stream()
                .filter(s -> "SILENT_MEMBER_HIGH".equals(s.getRuleCode())).findFirst().get();
        assertEquals("HIGH", vo.getSeverity());
        assertEquals(100L, vo.getDeptId());
        assertNotNull(vo.getTargetRoute());
    }

    @Test
    void silentMemberNotTriggeredWhenTotalMemberZero() {
        MemberOperationMetrics m = baseHealthy();
        m.setTotalMemberCount(0);
        m.setSilentMemberCount(0);
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertFalse(ruleCodes(list).contains("SILENT_MEMBER_HIGH"),
                "总会员为 0 时不应触发沉默会员建议");
    }

    @Test
    void repurchaseLowTriggeredWhenRateBelow15() {
        MemberOperationMetrics m = baseHealthy();
        m.setRepurchaseRate30d(new BigDecimal("10"));
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertTrue(ruleCodes(list).contains("REPURCHASE_LOW"));
    }

    @Test
    void firstPurchaseLowTriggeredWhenRateBelow20() {
        MemberOperationMetrics m = baseHealthy();
        m.setFirstPurchaseRate(new BigDecimal("12"));
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertTrue(ruleCodes(list).contains("FIRST_PURCHASE_LOW"));
    }

    @Test
    void pointsLiabilityHighTriggeredWhenAmountAbove1000() {
        MemberOperationMetrics m = baseHealthy();
        m.setPointsLiabilityAmount(new BigDecimal("2500"));
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertTrue(ruleCodes(list).contains("POINTS_LIABILITY_HIGH"));
        MemberOperationSuggestionVO vo = list.stream()
                .filter(s -> "POINTS_LIABILITY_HIGH".equals(s.getRuleCode())).findFirst().get();
        assertEquals(new BigDecimal("2500"), vo.getImpactAmount());
    }

    @Test
    void activityRoiUnavailableTriggeredWhenRoiNotAvailable() {
        MemberOperationMetrics m = baseHealthy();
        m.setActivityRoiAvailable(false);
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertTrue(ruleCodes(list).contains("ACTIVITY_ROI_UNAVAILABLE"));
    }

    @Test
    void multipleRulesCanTriggerTogether() {
        MemberOperationMetrics m = baseHealthy();
        m.setSilentMemberCount(50); // 50% > 30
        m.setRepurchaseRate30d(new BigDecimal("5")); // < 15
        m.setFirstPurchaseRate(new BigDecimal("8")); // < 20
        m.setPointsLiabilityAmount(new BigDecimal("3000")); // > 1000
        m.setActivityRoiAvailable(false);
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertEquals(5, list.size(), "五条规则应同时触发");
    }

    @Test
    void nullRateFieldsDoNotTriggerRules() {
        MemberOperationMetrics m = baseHealthy();
        m.setFirstPurchaseRate(null);
        m.setRepurchaseRate30d(null);
        m.setPointsLiabilityAmount(null);
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        assertFalse(ruleCodes(list).contains("FIRST_PURCHASE_LOW"));
        assertFalse(ruleCodes(list).contains("REPURCHASE_LOW"));
        assertFalse(ruleCodes(list).contains("POINTS_LIABILITY_HIGH"));
    }

    @Test
    void dedupKeyFollowsMemberRuleCodeDeptDatePattern() {
        MemberOperationMetrics m = baseHealthy();
        m.setSilentMemberCount(40);
        m.setBusinessDate("20260701");
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        MemberOperationSuggestionVO vo = list.stream()
                .filter(s -> "SILENT_MEMBER_HIGH".equals(s.getRuleCode())).findFirst().get();
        assertEquals("MEMBER:SILENT_MEMBER_HIGH:100:20260701", vo.getDedupKey());
    }

    @Test
    void dedupKeyUniquePerRuleCode() {
        MemberOperationMetrics m = baseHealthy();
        m.setSilentMemberCount(50);
        m.setRepurchaseRate30d(new BigDecimal("5"));
        m.setFirstPurchaseRate(new BigDecimal("8"));
        m.setPointsLiabilityAmount(new BigDecimal("3000"));
        m.setActivityRoiAvailable(false);
        m.setBusinessDate("20260701");
        List<MemberOperationSuggestionVO> list = service.generateSuggestions(m);
        long distinctKeys = list.stream().map(MemberOperationSuggestionVO::getDedupKey).distinct().count();
        assertEquals(list.size(), distinctKeys, "每条建议的去重 key 必须唯一");
    }
}
