package com.junsong.finance.service.diagnosis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates a set of {@link FinanceDiagnosisRule} instances against a shared context.
 *
 * <p>Results are sorted by priority (HIGH first), then by impactAmount descending —
 * matching the original sorting in {@code FinanceReportServiceImpl.getAlerts()}.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class FinanceDiagnosisRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(FinanceDiagnosisRuleEngine.class);

    private final List<FinanceDiagnosisRule> rules;

    public FinanceDiagnosisRuleEngine(List<FinanceDiagnosisRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    /**
     * Evaluate all rules against the context and return combined results,
     * sorted by priority (HIGH then MEDIUM then LOW), then by impactAmount descending.
     *
     * @param ctx the diagnosis context with pre-populated metrics
     * @return combined results from all rules, never null
     */
    public List<FinanceDiagnosisResult> runAll(FinanceDiagnosisContext ctx) {
        List<FinanceDiagnosisResult> allResults = new ArrayList<>();

        for (FinanceDiagnosisRule rule : rules) {
            try {
                List<FinanceDiagnosisResult> results = rule.evaluate(ctx);
                if (results != null) {
                    allResults.addAll(results);
                }
            } catch (Exception e) {
                // Log and continue — one rule failure must not block others
                log.warn("Diagnosis rule [{}] threw an exception, skipping: {}",
                        rule.getRuleId(), e.getMessage(), e);
            }
        }

        // Sort: HIGH > MEDIUM > LOW, then by impactAmount descending (same as original code)
        allResults.sort(Comparator
                .comparingInt(FinanceDiagnosisResult::getPriorityOrdinal)
                .thenComparing(Comparator.comparing(
                        (FinanceDiagnosisResult r) -> r.getImpactAmount() != null ? r.getImpactAmount() : BigDecimal.ZERO)
                        .reversed()));

        log.debug("Diagnosis engine produced {} results from {} rules", allResults.size(), rules.size());
        return allResults;
    }

    /**
     * @return the number of registered rules
     */
    public int getRuleCount() {
        return rules.size();
    }
}
