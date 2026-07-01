package com.junsong.finance.service.diagnosis.rules;

import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers when member sales ratio falls below 20%.
 *
 * <p>Mirrors the original logic: when {@code monthSales > 0} and
 * {@code memberRatio < 20%}, produces a LOW alert.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class MemberContributionDropRule implements FinanceDiagnosisRule {

    public static final String RULE_ID = "MEMBER_CONTRIBUTION_DROP";
    private static final BigDecimal RATIO_THRESHOLD = new BigDecimal("20");

    @Override
    public String getRuleId() { return RULE_ID; }

    @Override
    public List<FinanceDiagnosisResult> evaluate(FinanceDiagnosisContext ctx) {
        if (ctx.getMonthSales().signum() <= 0) {
            return Collections.emptyList();
        }

        BigDecimal memberRatio = ctx.getMemberSalesRatio();
        if (memberRatio.compareTo(RATIO_THRESHOLD) >= 0) {
            return Collections.emptyList();
        }

        List<FinanceDiagnosisResult> results = new ArrayList<>(1);
        results.add(new FinanceDiagnosisResult(
                RULE_ID,
                "会员贡献偏低预警规则",
                "LOW",
                "会员贡献偏低",
                "会员销售占比 " + memberRatio + "% 低于 20%，会员经营价值未充分发挥",
                "会员销售占比",
                memberRatio,
                RATIO_THRESHOLD,
                RATIO_THRESHOLD,
                ctx.getMonthSales().subtract(ctx.getMemberSales()),
                "查看会员贡献报表，分析会员复购和活动效果",
                "/member/report/member",
                "{\"timeType\":\"month\"}"
        ));
        return results;
    }
}
