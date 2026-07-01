package com.junsong.finance.service.diagnosis;

import com.junsong.finance.service.diagnosis.rules.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the diagnosis rule engine and all 6 rule implementations.
 *
 * <p>Uses hand-written test data — no Mockito (Mockito 5.x self-attach fails on JDK 26+).</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
class FinanceDiagnosisRuleEngineTest {

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper: build a FinanceDiagnosisContext with specific values
    // ─────────────────────────────────────────────────────────────────────────

    private FinanceDiagnosisContext ctx(BigDecimal monthSales, BigDecimal prevMonthSales,
                                         BigDecimal monthExpense, BigDecimal prevMonthExpense,
                                         BigDecimal profitRate, BigDecimal netProfit,
                                         int unverifiedCount, BigDecimal unverifiedAmount,
                                         int unsettledCount,
                                         BigDecimal memberSales, BigDecimal memberSalesRatio) {
        FinanceDiagnosisContext c = new FinanceDiagnosisContext();
        c.setMonthSales(monthSales);
        c.setPrevMonthSales(prevMonthSales);
        c.setMonthExpense(monthExpense);
        c.setPrevMonthExpense(prevMonthExpense);
        c.setProfitRate(profitRate);
        c.setNetProfit(netProfit);
        c.setUnverifiedExpenseCount(unverifiedCount);
        c.setUnverifiedExpenseAmount(unverifiedAmount);
        c.setUnsettledProfitShareCount(unsettledCount);
        c.setMemberSales(memberSales);
        c.setMemberSalesRatio(memberSalesRatio);
        return c;
    }

    private FinanceDiagnosisContext healthyCtx() {
        return ctx(
            new BigDecimal("200000"), new BigDecimal("190000"),  // sales up
            new BigDecimal("20000"), new BigDecimal("18000"),    // expense stable
            new BigDecimal("15"), new BigDecimal("180000"),      // 15% profit rate
            2, BigDecimal.ZERO,                                   // low unverified
            0,                                                    // no unsettled
            new BigDecimal("60000"), new BigDecimal("30")        // 30% member ratio
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SalesDropRule tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void salesDropRule_triggersWhenDropOver20Percent() {
        SalesDropRule rule = new SalesDropRule();
        // sales=10000, prev=13000 → change = -23.08% < -20%
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertEquals("SALES_DROP", results.get(0).getRuleId());
        assertEquals("HIGH", results.get(0).getAlertLevel());
        assertEquals("销售下滑预警", results.get(0).getTitle());
        assertNotNull(results.get(0).getSuggestedAction());
        assertNotNull(results.get(0).getTargetRoute());
        assertEquals("/finance/report/sale", results.get(0).getTargetRoute());
        assertNotNull(results.get(0).getRuleName(), "ruleName should be set");
        assertNotNull(results.get(0).getMetricName(), "metricName should be set");
        assertNotNull(results.get(0).getThreshold(), "threshold should be set");
    }

    @Test
    void salesDropRule_doesNotTriggerWhenDropBelow20Percent() {
        SalesDropRule rule = new SalesDropRule();
        // sales=10000, prev=11000 → change = -9.09% > -20%
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("11000"),
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty());
    }

    @Test
    void salesDropRule_doesNotTriggerWhenPrevMonthZero() {
        SalesDropRule rule = new SalesDropRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty(), "Should not trigger when prevMonthSales is zero (no comparison possible)");
    }

    @Test
    void salesDropRule_doesNotTriggerWhenSalesIncrease() {
        SalesDropRule rule = new SalesDropRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("15000"), new BigDecimal("10000"),
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ExpenseSpikeRule tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void expenseSpikeRule_triggersWhenIncreaseOver30Percent() {
        ExpenseSpikeRule rule = new ExpenseSpikeRule();
        // expense=11000, prev=8000 → change = +37.5% > 30%
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("11000"), new BigDecimal("8000"),
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertEquals("EXPENSE_SPIKE", results.get(0).getRuleId());
        assertEquals("MEDIUM", results.get(0).getAlertLevel());
    }

    @Test
    void expenseSpikeRule_doesNotTriggerWhenIncreaseBelow30Percent() {
        ExpenseSpikeRule rule = new ExpenseSpikeRule();
        // expense=9000, prev=8000 → change = +12.5% < 30%
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("9000"), new BigDecimal("8000"),
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty());
    }

    @Test
    void expenseSpikeRule_doesNotTriggerWhenPrevMonthZero() {
        ExpenseSpikeRule rule = new ExpenseSpikeRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("11000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ProfitRateLowRule tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void profitRateLowRule_triggersWhenBelow5Percent() {
        ProfitRateLowRule rule = new ProfitRateLowRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("100000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("3"), new BigDecimal("3000"),  // 3% profit rate
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertEquals("PROFIT_RATE_DROP", results.get(0).getRuleId());
        assertEquals("MEDIUM", results.get(0).getAlertLevel());
    }

    @Test
    void profitRateLowRule_doesNotTriggerWhenAt5Percent() {
        ProfitRateLowRule rule = new ProfitRateLowRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("100000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("5"), new BigDecimal("5000"),  // exactly 5%
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty(), "Exactly at 5% should not trigger");
    }

    @Test
    void profitRateLowRule_doesNotTriggerWhenSalesZero() {
        ProfitRateLowRule rule = new ProfitRateLowRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("3"), BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty(), "Should not trigger when sales is zero");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PendingVerifyHighRule tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void pendingVerifyHighRule_triggersWhenAmountOver5000() {
        PendingVerifyHighRule rule = new PendingVerifyHighRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            5, new BigDecimal("6000"),  // amount > 5000
            0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertEquals("PENDING_VERIFY", results.get(0).getRuleId());
        assertEquals("LOW", results.get(0).getAlertLevel());
    }

    @Test
    void pendingVerifyHighRule_triggersWhenCountOver20() {
        PendingVerifyHighRule rule = new PendingVerifyHighRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            25, new BigDecimal("3000"),  // count > 20
            0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertEquals(1, results.size());
    }

    @Test
    void pendingVerifyHighRule_doesNotTriggerWhenBothBelowThreshold() {
        PendingVerifyHighRule rule = new PendingVerifyHighRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            5, new BigDecimal("3000"),  // both below
            0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ProfitShareExceptionRule tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void profitShareExceptionRule_triggersWhenUnsettledRecordsExist() {
        ProfitShareExceptionRule rule = new ProfitShareExceptionRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, new BigDecimal("10000"),
            0, BigDecimal.ZERO,
            3,  // unsettled > 0
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertEquals("PROFIT_SHARE_EXCEPTION", results.get(0).getRuleId());
        assertEquals("MEDIUM", results.get(0).getAlertLevel());
        assertTrue(results.get(0).getReason().contains("尚未结算"));
    }

    @Test
    void profitShareExceptionRule_triggersWhenNetProfitNegative() {
        ProfitShareExceptionRule rule = new ProfitShareExceptionRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, new BigDecimal("-5000"),  // negative profit
            0, BigDecimal.ZERO,
            0,  // no unsettled
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertTrue(results.get(0).getReason().contains("净利润为负"));
    }

    @Test
    void profitShareExceptionRule_doesNotTriggerWhenAllSettledAndPositive() {
        ProfitShareExceptionRule rule = new ProfitShareExceptionRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, new BigDecimal("10000"),
            0, BigDecimal.ZERO,
            0,  // no unsettled
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MemberContributionDropRule tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void memberContributionDropRule_triggersWhenRatioBelow20Percent() {
        MemberContributionDropRule rule = new MemberContributionDropRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("100000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            new BigDecimal("5000"), new BigDecimal("5")  // 5% member ratio < 20%
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);

        assertEquals(1, results.size());
        assertEquals("MEMBER_CONTRIBUTION_DROP", results.get(0).getRuleId());
        assertEquals("LOW", results.get(0).getAlertLevel());
    }

    @Test
    void memberContributionDropRule_doesNotTriggerWhenAt20Percent() {
        MemberContributionDropRule rule = new MemberContributionDropRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("100000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            new BigDecimal("20000"), new BigDecimal("20")  // exactly 20%
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty(), "Exactly at 20% should not trigger");
    }

    @Test
    void memberContributionDropRule_doesNotTriggerWhenSalesZero() {
        MemberContributionDropRule rule = new MemberContributionDropRule();
        FinanceDiagnosisContext c = ctx(
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertTrue(results.isEmpty(), "Should not trigger when sales is zero");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Engine integration tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void engine_returnsEmptyForHealthyDepartment() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        ));

        List<FinanceDiagnosisResult> results = engine.runAll(healthyCtx());
        assertTrue(results.isEmpty(), "Healthy department should produce no alerts");
    }

    @Test
    void engine_combinesResultsFromAllRules() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        ));

        // Troubled department that triggers all 6 rules
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),   // sales drop -23%
            new BigDecimal("11000"), new BigDecimal("8000"),    // expense spike +37.5%
            new BigDecimal("3"), new BigDecimal("-1000"),       // 3% profit rate, negative net profit
            25, new BigDecimal("6000"),                         // unverified > threshold
            3,                                                  // unsettled > 0
            new BigDecimal("500"), new BigDecimal("5")          // 5% member ratio
        );

        List<FinanceDiagnosisResult> results = engine.runAll(c);

        assertEquals(6, results.size(), "All 6 rules should fire for a troubled department");

        Set<String> ruleIds = new HashSet<>();
        for (FinanceDiagnosisResult r : results) {
            ruleIds.add(r.getRuleId());
        }
        assertTrue(ruleIds.contains("SALES_DROP"));
        assertTrue(ruleIds.contains("EXPENSE_SPIKE"));
        assertTrue(ruleIds.contains("PROFIT_RATE_DROP"));
        assertTrue(ruleIds.contains("PENDING_VERIFY"));
        assertTrue(ruleIds.contains("PROFIT_SHARE_EXCEPTION"));
        assertTrue(ruleIds.contains("MEMBER_CONTRIBUTION_DROP"));
    }

    @Test
    void engine_sortsResultsByPriorityThenImpactAmount() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),          // HIGH
                new ExpenseSpikeRule(),       // MEDIUM
                new ProfitRateLowRule(),      // MEDIUM
                new PendingVerifyHighRule(),  // LOW
                new MemberContributionDropRule()  // LOW
        ));

        // Trigger multiple levels
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),   // HIGH: sales drop
            new BigDecimal("11000"), new BigDecimal("8000"),    // MEDIUM: expense spike
            new BigDecimal("3"), new BigDecimal("-1000"),       // MEDIUM: profit rate low
            25, new BigDecimal("6000"),                         // LOW: pending verify
            0,
            new BigDecimal("500"), new BigDecimal("5")          // LOW: member drop
        );

        List<FinanceDiagnosisResult> results = engine.runAll(c);

        assertEquals(5, results.size());
        // First should be HIGH
        assertEquals("HIGH", results.get(0).getAlertLevel());
        // Next should be MEDIUM
        assertEquals("MEDIUM", results.get(1).getAlertLevel());
        assertEquals("MEDIUM", results.get(2).getAlertLevel());
        // Last should be LOW
        assertEquals("LOW", results.get(3).getAlertLevel());
        assertEquals("LOW", results.get(4).getAlertLevel());
    }

    @Test
    void engine_toleratesRuleException() {
        // A rule that always throws
        FinanceDiagnosisRule brokenRule = new FinanceDiagnosisRule() {
            @Override
            public String getRuleId() { return "BROKEN_RULE"; }
            @Override
            public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
                throw new RuntimeException("Simulated failure");
            }
        };

        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                brokenRule,
                new SalesDropRule()
        ));

        // Sales drop context
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            BigDecimal.ZERO, BigDecimal.ZERO
        );

        // Should not throw — broken rule is skipped
        List<FinanceDiagnosisResult> results = engine.runAll(c);

        assertEquals(1, results.size());
        assertEquals("SALES_DROP", results.get(0).getRuleId());
    }

    @Test
    void engine_getRuleCount_returnsCorrectCount() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule()
        ));

        assertEquals(3, engine.getRuleCount());
    }

    @Test
    void engine_returnsEmptyForEmptyRuleList() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Collections.emptyList());

        List<FinanceDiagnosisResult> results = engine.runAll(healthyCtx());
        assertTrue(results.isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FinanceDiagnosisContext helper tests
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void context_salesChangeRate_computedCorrectly() {
        FinanceDiagnosisContext c = new FinanceDiagnosisContext();
        c.setMonthSales(new BigDecimal("10000"));
        c.setPrevMonthSales(new BigDecimal("12500"));

        BigDecimal rate = c.salesChangeRate();
        assertNotNull(rate);
        // (10000 - 12500) / 12500 * 100 = -20.00
        assertEquals(0, new BigDecimal("-20.00").compareTo(rate));
    }

    @Test
    void context_salesChangeRate_nullWhenPrevZero() {
        FinanceDiagnosisContext c = new FinanceDiagnosisContext();
        c.setMonthSales(new BigDecimal("10000"));
        c.setPrevMonthSales(BigDecimal.ZERO);

        assertNull(c.salesChangeRate());
    }

    @Test
    void context_expenseChangeRate_computedCorrectly() {
        FinanceDiagnosisContext c = new FinanceDiagnosisContext();
        c.setMonthExpense(new BigDecimal("11000"));
        c.setPrevMonthExpense(new BigDecimal("8000"));

        BigDecimal rate = c.expenseChangeRate();
        assertNotNull(rate);
        // (11000 - 8000) / 8000 * 100 = 37.50
        assertEquals(0, new BigDecimal("37.50").compareTo(rate));
    }

    @Test
    void context_nullSafeSetters_defaultToZero() {
        FinanceDiagnosisContext c = new FinanceDiagnosisContext();
        c.setMonthSales(null);
        c.setMonthExpense(null);
        c.setNetProfit(null);

        assertEquals(BigDecimal.ZERO, c.getMonthSales());
        assertEquals(BigDecimal.ZERO, c.getMonthExpense());
        assertEquals(BigDecimal.ZERO, c.getNetProfit());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Diagnosis Rule Governance tests (R3-D)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verify all 6 rules produce results with ruleId AND correct targetRoute.
     */
    @Test
    void allRules_shouldProduceResultsWithRuleIdAndTargetRoute() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        ));

        // Troubled department that triggers all 6 rules
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),   // sales drop -23%
            new BigDecimal("11000"), new BigDecimal("8000"),    // expense spike +37.5%
            new BigDecimal("3"), new BigDecimal("-1000"),       // 3% profit rate, negative net profit
            25, new BigDecimal("6000"),                         // unverified > threshold
            3,                                                  // unsettled > 0
            new BigDecimal("500"), new BigDecimal("5")          // 5% member ratio
        );

        List<FinanceDiagnosisResult> results = engine.runAll(c);
        assertEquals(6, results.size());

        // Build a map from ruleId to result for easy assertion
        Map<String, FinanceDiagnosisResult> byRuleId = new HashMap<>();
        for (FinanceDiagnosisResult r : results) {
            assertNotNull(r.getRuleId(), "Every result must have a ruleId");
            assertNotNull(r.getTargetRoute(), "Every result must have a targetRoute");
            assertFalse(r.getTargetRoute().isEmpty(), "targetRoute must not be empty");
            byRuleId.put(r.getRuleId(), r);
        }

        // Verify specific target routes per governance plan
        assertEquals("/finance/salesOperation", byRuleId.get("SALES_DROP").getTargetRoute());
        assertEquals("/finance/expenseAnomaly", byRuleId.get("EXPENSE_SPIKE").getTargetRoute());
        assertEquals("/finance/profitDrilldown", byRuleId.get("PROFIT_RATE_DROP").getTargetRoute());
        assertEquals("/finance/expense", byRuleId.get("PENDING_VERIFY").getTargetRoute());
        assertEquals("/finance/profitShareSettlement", byRuleId.get("PROFIT_SHARE_EXCEPTION").getTargetRoute());
        assertEquals("/member/contribution", byRuleId.get("MEMBER_CONTRIBUTION_DROP").getTargetRoute());
    }

    /**
     * Verify MEMBER_CONTRIBUTION_DROP targetRoute specifically.
     */
    @Test
    void memberContributionDropRule_targetRouteIsMemberReport() {
        MemberContributionDropRule rule = new MemberContributionDropRule();
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("100000"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0, BigDecimal.ZERO, 0,
            new BigDecimal("5000"), new BigDecimal("5")  // 5% < 20%
        );

        List<FinanceDiagnosisResult> results = rule.evaluate(c);
        assertEquals(1, results.size());
        assertEquals("/member/contribution", results.get(0).getTargetRoute());
    }

    /**
     * Verify all rules set the governance fields: ruleName, metricName, threshold.
     */
    @Test
    void allRules_shouldSetGovernanceFields() {
        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        ));

        // Troubled department that triggers all 6 rules
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),
            new BigDecimal("11000"), new BigDecimal("8000"),
            new BigDecimal("3"), new BigDecimal("-1000"),
            25, new BigDecimal("6000"),
            3,
            new BigDecimal("500"), new BigDecimal("5")
        );

        List<FinanceDiagnosisResult> results = engine.runAll(c);
        assertEquals(6, results.size());

        for (FinanceDiagnosisResult r : results) {
            assertNotNull(r.getRuleName(),
                "Rule [" + r.getRuleId() + "] must set ruleName");
            assertNotNull(r.getMetricName(),
                "Rule [" + r.getRuleId() + "] must set metricName");
            assertNotNull(r.getThreshold(),
                "Rule [" + r.getRuleId() + "] must set threshold");

            assertFalse(r.getRuleName().isEmpty(),
                "Rule [" + r.getRuleId() + "] ruleName must not be empty");
            assertFalse(r.getMetricName().isEmpty(),
                "Rule [" + r.getRuleId() + "] metricName must not be empty");
        }
    }

    /**
     * Verify each rule's ruleName is a meaningful Chinese name.
     */
    @Test
    void allRules_ruleNamesAreDescriptive() {
        // Trigger each rule individually and check ruleName
        Map<String, String> expectedNames = new LinkedHashMap<>();
        expectedNames.put("SALES_DROP", "销售下滑预警规则");
        expectedNames.put("EXPENSE_SPIKE", "费用突增预警规则");
        expectedNames.put("PROFIT_RATE_DROP", "利润率偏低预警规则");
        expectedNames.put("PENDING_VERIFY", "未核销费用堆积预警规则");
        expectedNames.put("PROFIT_SHARE_EXCEPTION", "分润结算异常预警规则");
        expectedNames.put("MEMBER_CONTRIBUTION_DROP", "会员贡献偏低预警规则");

        // Trigger all rules at once
        FinanceDiagnosisContext c = ctx(
            new BigDecimal("10000"), new BigDecimal("13000"),
            new BigDecimal("11000"), new BigDecimal("8000"),
            new BigDecimal("3"), new BigDecimal("-1000"),
            25, new BigDecimal("6000"),
            3,
            new BigDecimal("500"), new BigDecimal("5")
        );

        FinanceDiagnosisRuleEngine engine = new FinanceDiagnosisRuleEngine(Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        ));

        List<FinanceDiagnosisResult> results = engine.runAll(c);

        Map<String, String> actualNames = new HashMap<>();
        for (FinanceDiagnosisResult r : results) {
            actualNames.put(r.getRuleId(), r.getRuleName());
        }

        assertEquals(expectedNames, actualNames,
            "All rule names should match the governance-defined Chinese names");
    }
}
