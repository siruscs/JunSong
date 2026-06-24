package com.junsong.workflow.lowcode.sync;

import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.service.LcExpressionService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分支规则求值器：求值分支条件，返回命中的 targetVarName。
 * 支持 AND/OR 组合条件。
 */
@Component
public class LcBranchRuleEvaluator
{
    @Autowired
    private LcExpressionService expressionService;

    /**
     * 求值分支规则列表，返回第一个命中的 targetVarName。
     * 同一 gatewayKey 下的规则按 orderNum/groupOp 组合求值。
     *
     * @param rules 规则列表（已按 id 排序）
     * @param processVariables 流程变量（key=变量名, value=变量值）
     * @return 命中的 targetVarName，无命中返回 null
     */
    public String evaluate(List<LcBizBranchRule> rules, Map<String, Object> processVariables)
    {
        if (rules == null || rules.isEmpty()) return null;

        // 按 gatewayKey 分组处理
        String currentGateway = null;
        String groupOp = "OR";
        String fallbackTarget = null;

        for (LcBizBranchRule rule : rules)
        {
            if (rule.getGatewayKey() == null) continue;

            // gateway 切换时重置
            if (!rule.getGatewayKey().equals(currentGateway))
            {
                currentGateway = rule.getGatewayKey();
                groupOp = "OR";
            }

            // 更新 groupOp（最后一条有效）
            if (rule.getGroupOp() != null) groupOp = rule.getGroupOp();

            String conditionJson = buildConditionJson(rule);
            boolean match = expressionService.evaluateCondition(conditionJson, processVariables);

            if ("AND".equalsIgnoreCase(groupOp))
            {
                if (!match) return null; // AND 模式下任一不满足则整体失败
            }
            else
            {
                // OR 模式：命中即返回
                if (match) return rule.getTargetVarName();
            }
            // 记录 OR 模式的最终兜底
            fallbackTarget = rule.getTargetVarName();
        }

        // OR 模式兜底返回最后一个 target
        return "AND".equalsIgnoreCase(groupOp) ? null : fallbackTarget;
    }

    private String buildConditionJson(LcBizBranchRule rule)
    {
        if (rule.getFieldKey() == null) return null;
        return String.format(
            "{\"when\":{\"field\":\"%s\",\"op\":\"%s\",\"value\":\"%s\"}}",
            rule.getFieldKey(),
            mapOperator(rule.getOperator()),
            rule.getCompareValue() != null ? rule.getCompareValue() : ""
        );
    }

    private String mapOperator(String op)
    {
        if (op == null) return "eq";
        return switch (op)
        {
            case "eq", "=", "==" -> "eq";
            case "ne", "!=", "<>" -> "ne";
            case "gt", ">", "greater" -> "gt";
            case "ge", ">=", "greaterEqual" -> "ge";
            case "lt", "<", "less" -> "lt";
            case "le", "<=", "lessEqual" -> "le";
            case "contains" -> "contains";
            case "in" -> "in";
            default -> "eq";
        };
    }
}
