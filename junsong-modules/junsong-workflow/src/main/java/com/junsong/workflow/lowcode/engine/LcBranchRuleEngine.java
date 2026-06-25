package com.junsong.workflow.lowcode.engine;

import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 分支规则公共求值引擎。
 *
 * 统一 LcWorkflowAssembleService（提交前预计算）与 LcBranchRuleEvaluator（网关条件求值）
 * 的分支规则判定逻辑，避免双轨实现导致的行为漂移。
 *
 * @author Genesis·峻松
 */
public final class LcBranchRuleEngine
{
    private LcBranchRuleEngine() {}

    /**
     * 单条规则求值。
     *
     * @param rule    分支规则
     * @param context 上下文数据（表单数据或流程变量）
     * @return 是否命中
     */
    public static boolean evaluate(LcBizBranchRule rule, Map<String, Object> context)
    {
        String operator = rule.getOperator() == null ? "EQ" : rule.getOperator().trim().toUpperCase();
        Object actual = context != null ? context.get(rule.getFieldKey()) : null;
        String compare = rule.getCompareValue();

        switch (operator)
        {
            case "EMPTY":
                return isEmpty(actual);
            case "NOT_EMPTY":
                return !isEmpty(actual);
            case "EQ":
                return compareEquals(actual, compare);
            case "NE":
                return !compareEquals(actual, compare);
            case "GT":
            case "GE":
            case "LT":
            case "LE":
                return compareNumeric(actual, compare, operator);
            case "IN":
                return splitCompare(compare).contains(asText(actual));
            case "NOT_IN":
                return !splitCompare(compare).contains(asText(actual));
            case "CONTAINS":
                return actual != null && compare != null && asText(actual).contains(compare);
            default:
                return false;
        }
    }

    // ---------- 内部辅助方法 ----------

    private static boolean compareEquals(Object actual, String compare)
    {
        if (actual == null)
        {
            return compare == null || compare.isEmpty();
        }
        BigDecimal a = toDecimal(actual);
        BigDecimal b = compare == null ? null : toDecimal(compare);
        if (a != null && b != null)
        {
            return a.compareTo(b) == 0;
        }
        return asText(actual).equals(compare);
    }

    private static boolean compareNumeric(Object actual, String compare, String operator)
    {
        BigDecimal a = toDecimal(actual);
        BigDecimal b = compare == null ? null : toDecimal(compare);
        if (a == null || b == null)
        {
            return false;
        }
        int cmp = a.compareTo(b);
        return switch (operator)
        {
            case "GT" -> cmp > 0;
            case "GE" -> cmp >= 0;
            case "LT" -> cmp < 0;
            case "LE" -> cmp <= 0;
            default -> false;
        };
    }

    private static boolean isEmpty(Object value)
    {
        if (value == null) return true;
        if (value instanceof String s) return s.isBlank();
        if (value instanceof java.util.Collection<?> c) return c.isEmpty();
        return false;
    }

    private static List<String> splitCompare(String compare)
    {
        if (compare == null || compare.isBlank()) return List.of();
        return Arrays.stream(compare.split(",")).map(String::trim).toList();
    }

    private static BigDecimal toDecimal(Object value)
    {
        if (value == null) return null;
        try
        {
            if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            return new BigDecimal(value.toString().trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static String asText(Object value)
    {
        return value == null ? "" : value.toString();
    }
}
