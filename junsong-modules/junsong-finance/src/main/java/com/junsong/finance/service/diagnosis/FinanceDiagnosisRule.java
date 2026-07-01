package com.junsong.finance.service.diagnosis;

import java.util.List;

/**
 * A single diagnosis rule that evaluates financial data and produces results.
 *
 * <p>Each rule encapsulates one business check (e.g. sales drop, expense spike).
 * Rules are stateless and thread-safe — the engine may evaluate them in any order.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public interface FinanceDiagnosisRule {

    /**
     * Unique identifier for this rule, e.g. "SALES_DROP", "EXPENSE_SPIKE".
     * Maps directly to {@code FinanceAlertVO.alertType} and {@code FinanceReviewTaskVO.taskType}.
     */
    String getRuleId();

    /**
     * Evaluate the rule against the given context.
     *
     * @param ctx pre-populated context containing aggregated financial metrics
     * @return zero or more diagnosis results (typically 0 or 1 for aggregate-level rules)
     */
    List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx);
}
