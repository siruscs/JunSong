package com.junsong.finance.service.diagnosis.rules;

import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers when month-over-month expenses increase more than 30%.
 *
 * <p>Mirrors the original logic: {@code expenseChange > 30%} produces a MEDIUM alert.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class ExpenseSpikeRule implements FinanceDiagnosisRule {

    public static final String RULE_ID = "EXPENSE_SPIKE";
    private static final BigDecimal CHANGE_THRESHOLD = new BigDecimal("30");

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
        BigDecimal changeRate = ctx.expenseChangeRate();
        if (changeRate == null || changeRate.compareTo(CHANGE_THRESHOLD) <= 0) {
            return Collections.emptyList();
        }

        List<FinanceDiagnosisResult> results = new ArrayList<>(1);
        results.add(new FinanceDiagnosisResult(
                RULE_ID,
                "费用突增预警规则",
                "MEDIUM",
                "费用突增预警",
                "本月费用较上月增长 " + changeRate + "%，超过 30% 阈值",
                "费用环比变化率",
                ctx.getMonthExpense(),
                CHANGE_THRESHOLD,
                ctx.getPrevMonthExpense(),
                ctx.getMonthExpense().subtract(ctx.getPrevMonthExpense()),
                "查看费用异常分析，排查突增费用项",
                "/finance/expenseAnomaly",
                "{\"timeType\":\"month\"}"
        ));
        return results;
    }
}
