package com.junsong.member.service;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;

/** 标准化跨机构配置差异，只负责比较，不负责权限和写入。 */
public record ConfigSyncDiff(String syncType, String businessKey, String operation,
                             Map<String, FieldChange> fields)
{
    public ConfigSyncDiff
    {
        fields = Map.copyOf(fields);
    }

    public static ConfigSyncDiff compare(String syncType, String businessKey,
                                         Map<String, Object> source, Map<String, Object> target)
    {
        Map<String, FieldChange> changes = new LinkedHashMap<>();
        if (source != null)
        {
            source.forEach((name, value) -> {
                Object targetValue = target == null ? null : target.get(name);
                if (!Objects.equals(value, targetValue)) changes.put(name, new FieldChange(value, targetValue));
            });
        }
        if (target != null)
        {
            target.forEach((name, value) -> {
                if (source == null || !source.containsKey(name))
                    changes.putIfAbsent(name, new FieldChange(null, value));
            });
        }
        String operation = target == null ? "CREATE" : changes.isEmpty() ? "NOOP" : "DIFF";
        return new ConfigSyncDiff(syncType, businessKey, operation, changes);
    }

    /**
     * 快照可能一侧来自 JSON（Double/String），另一侧来自 JDBC（BigDecimal/LocalDateTime）。
     * 这些值在业务上相同，不能因为 Java 运行时类型不同而触发预览过期冲突。
     */
    public static boolean equivalent(Object left, Object right)
    {
        return Objects.equals(canonicalize(left), canonicalize(right));
    }

    private static Object canonicalize(Object value)
    {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map)
        {
            Map<String, Object> result = new TreeMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), canonicalize(item)));
            return result;
        }
        if (value instanceof Iterable<?> iterable)
        {
            List<Object> result = new ArrayList<>();
            iterable.forEach(item -> result.add(canonicalize(item)));
            return result;
        }
        if (value instanceof Number number)
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        if (value instanceof TemporalAccessor || value instanceof java.util.Date)
            return temporalText(value);
        if (value instanceof String text && looksLikeDateTime(text))
            return text.replace(' ', 'T');
        return value;
    }

    private static String temporalText(Object value)
    {
        if (value instanceof java.util.Date date)
            return date.toInstant().toString();
        return String.valueOf(value);
    }

    private static boolean looksLikeDateTime(String value)
    {
        return value.length() >= 16 && value.charAt(4) == '-' && value.charAt(7) == '-'
                && (value.indexOf('T') >= 0 || value.indexOf(' ') >= 0);
    }

    public record FieldChange(Object sourceValue, Object targetValue) { }
}
