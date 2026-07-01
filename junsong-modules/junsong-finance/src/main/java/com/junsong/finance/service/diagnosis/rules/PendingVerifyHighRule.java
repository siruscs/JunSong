package com.junsong.finance.service.diagnosis.rules;

import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers when unverified expenses are excessive (amount &gt; 5000 or count &gt; 20).
 *
 * <p>Mirrors the original logic: produces a LOW alert.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class PendingVerifyHighRule implements FinanceDiagnosisRule {

    public static final String RULE_ID = "PENDING_VERIFY";
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("5000");
    private static final int COUNT_THRESHOLD = 20;

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
        if (ctx.getUnverifiedExpenseAmount().compareTo(AMOUNT_THRESHOLD) <= 0
                && ctx.getUnverifiedExpenseCount() <= COUNT_THRESHOLD) {
            return Collections.emptyList();
        }

        List<FinanceDiagnosisResult> results = new ArrayList<>(1);
        results.add(new FinanceDiagnosisResult(
                RULE_ID,
                "未核销费用堆积预警规则",
                "LOW",
                "未核销费用堆积",
                "当前有 " + ctx.getUnverifiedExpenseCount() + " 笔未核销费用，合计 " + ctx.getUnverifiedExpenseAmount() + " 元",
                "未核销费用金额",
                ctx.getUnverifiedExpenseAmount(),
                AMOUNT_THRESHOLD,
                AMOUNT_THRESHOLD,
                ctx.getUnverifiedExpenseAmount(),
                "前往费用管理页面处理核销",
                "/finance/expense",
                "{\"status\":\"0\"}"
        ));
        return results;
    }
}
