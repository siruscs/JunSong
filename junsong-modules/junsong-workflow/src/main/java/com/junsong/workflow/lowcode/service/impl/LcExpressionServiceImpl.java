package com.junsong.workflow.lowcode.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.junsong.workflow.lowcode.service.LcExpressionService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

@Service
public class LcExpressionServiceImpl implements LcExpressionService
{
    private static final Logger log = LoggerFactory.getLogger(LcExpressionServiceImpl.class);

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /** SpEL 表达式编译缓存（LRU，上限 1000） */
    private final Map<String, Expression> exprCache = new ConcurrentHashMap<>(256);
    private static final int MAX_CACHE_SIZE = 1000;

    @Override
    public boolean evaluateCondition(String conditionJson, Map<String, Object> fieldValues)
    {
        if (conditionJson == null || conditionJson.isBlank()) return true;
        try
        {
            JSONObject cond = JSON.parseObject(conditionJson);
            JSONObject when = cond.getJSONObject("when");
            if (when == null) return true;

            if (when.containsKey("field"))
            {
                return evaluateSingleCondition(when, fieldValues);
            }
            else
            {
                JSONArray conditions = when.getJSONArray("conditions");
                String groupOp = when.getString("op");
                if (groupOp == null) groupOp = "AND";
                return evaluateConditionGroup(conditions, groupOp, fieldValues);
            }
        }
        catch (Exception e)
        {
            log.warn("条件求值异常，默认返回 false: conditionJson={}", conditionJson, e);
            return false;
        }
    }

    private boolean evaluateSingleCondition(JSONObject c, Map<String, Object> fieldValues)
    {
        String field = c.getString("field");
        String op = c.getString("op", "eq");
        Object expected = c.get("value");
        Object actual = fieldValues.get(field);
        if (actual == null) actual = "";

        String spelOp = mapOperator(op);
        String exprStr = buildSpelExpr(field, spelOp, expected);
        return evaluateSpEL(exprStr, fieldValues) == Boolean.TRUE;
    }

    private boolean evaluateConditionGroup(JSONArray conditions, String groupOp, Map<String, Object> fieldValues)
    {
        if (conditions == null || conditions.isEmpty()) return true;
        boolean result = "AND".equalsIgnoreCase(groupOp);
        for (Object item : conditions)
        {
            JSONObject c = (JSONObject) item;
            boolean subResult;
            if (c.containsKey("field"))
            {
                subResult = evaluateSingleCondition(c, fieldValues);
            }
            else
            {
                JSONArray subConds = c.getJSONArray("conditions");
                String subOp = c.getString("op", "AND");
                subResult = evaluateConditionGroup(subConds, subOp, fieldValues);
            }
            if ("AND".equalsIgnoreCase(groupOp))
            {
                if (!subResult) return false;
            }
            else
            {
                if (subResult) return true;
            }
        }
        return result;
    }

    private String mapOperator(String op)
    {
        return switch (op)
        {
            case "eq" -> "==";
            case "ne" -> "!=";
            case "gt" -> ">";
            case "ge" -> ">=";
            case "lt" -> "<";
            case "le" -> "<=";
            case "contains" -> "contains";
            case "in" -> "in";
            default -> "==";
        };
    }

    private String buildSpelExpr(String field, String spelOp, Object expected)
    {
        String val;
        if (expected == null)
        {
            val = "null";
        }
        else if (expected instanceof String)
        {
            val = "'" + ((String) expected).replace("\\", "\\\\").replace("'", "\\'") + "'";
        }
        else if (expected instanceof Number)
        {
            val = String.valueOf(expected);
        }
        else
        {
            val = "'" + String.valueOf(expected).replace("'", "\\'") + "'";
        }

        String fieldRef = "#" + field;
        if ("contains".equals(spelOp))
            return fieldRef + " != null && " + fieldRef + ".toString().contains(" + val + ")";
        if ("in".equals(spelOp))
            return "{" + val + "}.contains(" + fieldRef + ")";
        return fieldRef + " " + spelOp + " " + val;
    }

    @Override
    public Object evaluateSpEL(String expression, Map<String, Object> variables)
    {
        if (expression == null || expression.isBlank()) return null;
        // 使用 SimpleEvaluationContext 限制 SpEL 能力，防止 RCE
        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        if (variables != null)
        {
            variables.forEach(context::setVariable);
        }
        try
        {
            Expression exp = exprCache.computeIfAbsent(expression, key -> {
                if (exprCache.size() >= MAX_CACHE_SIZE) {
                    exprCache.clear();
                }
                return PARSER.parseExpression(key);
            });
            return exp.getValue(context);
        }
        catch (Exception e)
        {
            log.warn("SpEL 求值异常: expression={}", expression, e);
            return null;
        }
    }

    /**
     * 清空 SpEL 表达式编译缓存。
     * 业务配置变更后调用，确保旧表达式不残留。
     */
    public void clearCache()
    {
        exprCache.clear();
        log.info("SpEL 表达式编译缓存已清空，条目数={}", exprCache.size());
    }
}
