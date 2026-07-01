package com.junsong.finance.service.diagnosis.rules;

import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers when there are unsettled profit-share records or net profit is negative.
 *
 * <p>Mirrors the original logic: {@code unsettledCount > 0 || netProfit < 0}
 * produces a MEDIUM alert.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class ProfitShareExceptionRule implements FinanceDiagnosisRule {

    public static final String RULE_ID = "PROFIT_SHARE_EXCEPTION";

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
        boolean hasUnsettled = ctx.getUnsettledProfitShareCount() > 0;
        boolean hasNegativeProfit = ctx.getNetProfit().compareTo(BigDecimal.ZERO) < 0;

        if (!hasUnsettled && !hasNegativeProfit) {
            return Collections.emptyList();
        }

        String reasonText;
        String metricName;
        BigDecimal metricVal;
        if (hasNegativeProfit) {
            reasonText = "当前期间净利润为负（" + ctx.getNetProfit() + "），分润无法正常计算";
            metricName = "净利润";
            metricVal = ctx.getNetProfit();
        } else {
            reasonText = "有 " + ctx.getUnsettledProfitShareCount() + " 条分润记录尚未结算";
            metricName = "未结算分润笔数";
            metricVal = new BigDecimal(ctx.getUnsettledProfitShareCount());
        }

        BigDecimal impactAmount = hasNegativeProfit ? ctx.getNetProfit().abs() : BigDecimal.ZERO;

        List<FinanceDiagnosisResult> results = new ArrayList<>(1);
        results.add(new FinanceDiagnosisResult(
                RULE_ID,
                "分润结算异常预警规则",
                "MEDIUM",
                "分润结算异常",
                reasonText,
                metricName,
                metricVal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                impactAmount,
                "查看分润结算看板，处理异常记录",
                "/finance/profitShareSettlement",
                "{}"
        ));
        return results;
    }
}
