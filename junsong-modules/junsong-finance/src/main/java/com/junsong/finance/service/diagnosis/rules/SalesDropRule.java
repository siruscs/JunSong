package com.junsong.finance.service.diagnosis.rules;

import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers when month-over-month sales drop more than 20%.
 *
 * <p>Mirrors the original logic: {@code changeRate < -20%} produces a HIGH alert.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class SalesDropRule implements FinanceDiagnosisRule {

    public static final String RULE_ID = "SALES_DROP";
    private static final BigDecimal CHANGE_THRESHOLD = new BigDecimal("-20");

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
        BigDecimal changeRate = ctx.salesChangeRate();
        if (changeRate == null || changeRate.compareTo(CHANGE_THRESHOLD) >= 0) {
            return Collections.emptyList();
        }

        List<FinanceDiagnosisResult> results = new ArrayList<>(1);
        results.add(new FinanceDiagnosisResult(
                RULE_ID,
                "销售下滑预警规则",
                "HIGH",
                "销售下滑预警",
                "本月销售额较上月下降 " + changeRate.abs() + "%，超过 20% 阈值",
                "销售环比变化率",
                ctx.getMonthSales(),
                CHANGE_THRESHOLD,
                ctx.getPrevMonthSales(),
                ctx.getPrevMonthSales().subtract(ctx.getMonthSales()),
                "查看销售经营分析，定位下滑门店和品类",
                "/finance/report/sale",
                "{\"timeType\":\"month\"}"
        ));
        return results;
    }
}
