package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.finance.domain.vo.ReviewQualityDashboardVO;
import com.junsong.finance.domain.vo.ReviewQualityQueryParams;
import com.junsong.finance.mapper.ReviewQualityMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复盘质量服务测试（手写 fake，避免 Mockito JDK 26+ 问题）。
 */
class ReviewQualityServiceImplTest {

    private FakeReviewQualityMapper mapper;
    private StubHealthRuleConfigReader ruleReader;
    private ReviewQualityServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = new FakeReviewQualityMapper();
        ruleReader = new StubHealthRuleConfigReader();
        service = new ReviewQualityServiceImpl();
        // Use reflection to inject fakes (no Spring context)
        injectField(service, "mapper", mapper);
        injectField(service, "ruleReader", ruleReader);
    }

    @Test
    void defaultThresholdsUsedWhenConfigMissing() {
        mapper.total = 10;
        mapper.done = 5;
        mapper.overdue = 3;
        mapper.noNote = 1;
        mapper.avgFirst = 12.0;
        mapper.avgClose = 36.0;

        ReviewQualityDashboardVO vo = service.getDashboard(new ReviewQualityQueryParams());

        assertNotNull(vo);
        assertTrue(vo.getQualityScore().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void configuredFirstResponseThresholdChangesScore() {
        mapper.total = 10;
        mapper.done = 8;
        mapper.overdue = 0;
        mapper.noNote = 0;
        mapper.avgFirst = 48.0; // > default 24
        mapper.avgClose = 10.0;

        ReviewQualityDashboardVO vo = service.getDashboard(new ReviewQualityQueryParams());
        BigDecimal scoreWithDefault = vo.getQualityScore();

        // Now set threshold to 96 hours - 48 should not trigger deduction
        ruleReader.thresholds.put("FIN_REVIEW_FIRST_RESPONSE_HOURS", new BigDecimal("96"));
        ReviewQualityDashboardVO vo2 = service.getDashboard(new ReviewQualityQueryParams());

        assertTrue(vo2.getQualityScore().compareTo(scoreWithDefault) > 0,
            "Higher threshold should result in better score");
    }

    @Test
    void disabledRuleDoesNotDeductScore() {
        mapper.total = 10;
        mapper.done = 5;
        mapper.overdue = 5; // 50% overdue
        mapper.noNote = 0;
        mapper.avgFirst = 5.0;
        mapper.avgClose = 10.0;

        ReviewQualityDashboardVO vo = service.getDashboard(new ReviewQualityQueryParams());

        // Disable overdue rule
        ruleReader.enabled.put("FIN_REVIEW_OVERDUE_RATIO", false);
        ReviewQualityDashboardVO vo2 = service.getDashboard(new ReviewQualityQueryParams());

        assertTrue(vo2.getQualityScore().compareTo(vo.getQualityScore()) > 0,
            "Disabling overdue rule should improve score");
    }

    @Test
    void overdueRatioReducesScore() {
        mapper.total = 10;
        mapper.done = 5;
        mapper.overdue = 5; // 50% > 20% threshold
        mapper.noNote = 0;
        mapper.avgFirst = 5.0;
        mapper.avgClose = 10.0;

        ReviewQualityDashboardVO vo = service.getDashboard(new ReviewQualityQueryParams());

        assertTrue(vo.getQualityScore().compareTo(BigDecimal.valueOf(100)) < 0,
            "Overdue ratio above threshold should reduce score");
    }

    @Test
    void sentinelDeptIdsReturnsEmptyStats() {
        mapper.total = 0;
        mapper.done = 0;
        mapper.overdue = 0;
        mapper.noNote = 0;
        mapper.avgFirst = 0.0;
        mapper.avgClose = 0.0;

        ReviewQualityDashboardVO vo = service.getDashboard(new ReviewQualityQueryParams());

        assertEquals(0, vo.getTotalTaskCount());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(vo.getQualityScore()));
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + fieldName, e);
        }
    }

    static class FakeReviewQualityMapper implements ReviewQualityMapper {
        int total = 0;
        int done = 0;
        int overdue = 0;
        int noNote = 0;
        double avgFirst = 0.0;
        double avgClose = 0.0;

        @Override public int countTasks(List<Long> d, Date s, Date e) { return total; }
        @Override public int countDoneTasks(List<Long> d, Date s, Date e) { return done; }
        @Override public int countOverdueTasks(List<Long> d, Date s, Date e) { return overdue; }
        @Override public Double avgFirstResponseHours(List<Long> d, Date s, Date e) { return avgFirst; }
        @Override public Double avgCloseHours(List<Long> d, Date s, Date e) { return avgClose; }
        @Override public int countNoNoteDoneTasks(List<Long> d, Date s, Date e) { return noNote; }
    }

    static class StubHealthRuleConfigReader extends HealthRuleConfigReader {
        final java.util.Map<String, BigDecimal> thresholds = new java.util.HashMap<>();
        final java.util.Map<String, Boolean> enabled = new java.util.HashMap<>();

        StubHealthRuleConfigReader() {
            super(null); // no JdbcTemplate needed
        }

        @Override
        public BigDecimal getThreshold(String ruleCode, BigDecimal defaultValue) {
            return thresholds.getOrDefault(ruleCode, defaultValue);
        }

        @Override
        public boolean isEnabled(String ruleCode, boolean defaultValue) {
            return enabled.getOrDefault(ruleCode, defaultValue);
        }

        @Override
        public String getSuggestion(String ruleCode, String defaultValue) {
            return defaultValue;
        }
    }
}
