package com.junsong.finance.service.diagnosis.rules;

import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers when profit rate falls below 5%.
 *
 * <p>Mirrors the original logic: {@code profitRate < 5%} (and sales > 0) produces a MEDIUM alert.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class ProfitRateLowRule implements FinanceDiagnosisRule {

    public static final String RULE_ID = "PROFIT_RATE_DROP";
    private static final BigDecimal PROFIT_RATE_THRESHOLD = new BigDecimal("5");

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
        if (ctx.getMonthSales().signum() <= 0 || ctx.getProfitRate().compareTo(PROFIT_RATE_THRESHOLD) >= 0) {
            return Collections.emptyList();
        }

        List<FinanceDiagnosisResult> results = new ArrayList<>(1);
        results.add(new FinanceDiagnosisResult(
                RULE_ID,
                "利润率偏低预警规则",
                "MEDIUM",
                "利润率偏低预警",
                "当前利润率 " + ctx.getProfitRate() + "% 低于 5% 安全线",
                "利润率",
                ctx.getProfitRate(),
                PROFIT_RATE_THRESHOLD,
                PROFIT_RATE_THRESHOLD,
                ctx.getNetProfit(),
                "查看利润报表，分析成本与收入结构",
                "/finance/report/profit",
                "{\"timeType\":\"month\"}"
        ));
        return results;
    }
}
