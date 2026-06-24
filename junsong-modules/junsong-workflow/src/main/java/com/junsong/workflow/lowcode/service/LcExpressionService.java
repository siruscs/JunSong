package com.junsong.workflow.lowcode.service;

import java.util.Map;

/**
 * 低代码表达式服务：提供字段条件可见性求值和 SpEL 表达式执行能力。
 */
public interface LcExpressionService
{
    /**
     * 判断条件表达式是否满足（用于字段条件显隐联动）
     * @param conditionJson fieldExt 中的 visibleCondition JSON 字符串
     * @param fieldValues 当前表单字段值 Map（key=fieldKey, value=字段值）
     * @return true=条件满足（字段可见），false=条件不满足
     */
    boolean evaluateCondition(String conditionJson, Map<String, Object> fieldValues);

    /**
     * SpEL 表达式求值（用于复杂参与者规则）
     * @param expression SpEL 表达式字符串，如 "#amount > 1000 && #type == 'EXPENSE'"
     * @param variables 变量上下文 Map
     * @return 求值结果
     */
    Object evaluateSpEL(String expression, Map<String, Object> variables);
}
