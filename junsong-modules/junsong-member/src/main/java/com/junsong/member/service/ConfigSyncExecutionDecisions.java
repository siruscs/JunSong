package com.junsong.member.service;

import java.util.Locale;
import java.util.Map;

/** 执行前统一校验同步明细决策，避免未确认的差异被覆盖。 */
public final class ConfigSyncExecutionDecisions
{
    private ConfigSyncExecutionDecisions() { }

    public static String resolve(String operation, String decision)
    {
        String normalizedOperation = normalize(operation);
        String normalizedDecision = decision == null || decision.isBlank() ? null : normalize(decision);
        if (normalizedDecision == null)
        {
            if ("CREATE".equals(normalizedOperation)) return "CREATE";
            if ("PRODUCT_MISSING".equals(normalizedOperation)) return "CREATE";
            if ("NOOP".equals(normalizedOperation) || "CONFLICT".equals(normalizedOperation)
                    || "IMPACT_BLOCKED".equals(normalizedOperation)) return "SKIP";
            throw new IllegalArgumentException("每个差异明细都必须选择覆盖或跳过");
        }
        if ("CREATE".equals(normalizedOperation) && !"CREATE".equals(normalizedDecision))
            throw new IllegalArgumentException("新增明细只能选择新增");
        if ("PRODUCT_MISSING".equals(normalizedOperation) && !"CREATE".equals(normalizedDecision) && !"SKIP".equals(normalizedDecision))
            throw new IllegalArgumentException("缺少商品的明细只能选择同步或跳过");
        if ("NOOP".equals(normalizedOperation) && !"SKIP".equals(normalizedDecision))
            throw new IllegalArgumentException("无差异明细只能选择跳过");
        if ("CONFLICT".equals(normalizedOperation) && !"SKIP".equals(normalizedDecision))
            throw new IllegalArgumentException("编码冲突明细只能跳过");
        if ("IMPACT_BLOCKED".equals(normalizedOperation) && !"SKIP".equals(normalizedDecision))
            throw new IllegalArgumentException("已被会员使用的等级只能跳过，不能自动覆盖");
        if ("DIFF".equals(normalizedOperation)
                && !"OVERWRITE".equals(normalizedDecision) && !"SKIP".equals(normalizedDecision))
            throw new IllegalArgumentException("差异明细只能选择覆盖或跳过");
        return normalizedDecision;
    }

    public static void validateAll(Map<Long, String> decisions, Map<Long, String> operations)
    {
        if (decisions == null || operations == null || decisions.size() != operations.size()
                || !decisions.keySet().containsAll(operations.keySet()))
            throw new IllegalArgumentException("同步明细决策不完整");
        operations.forEach((detailId, operation) -> resolve(operation, decisions.get(detailId)));
    }

    private static String normalize(String value)
    {
        return value.toUpperCase(Locale.ROOT);
    }
}
