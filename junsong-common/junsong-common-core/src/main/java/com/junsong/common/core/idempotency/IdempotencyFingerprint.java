package com.junsong.common.core.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 幂等指纹工具。
 *
 * 对请求体做稳定指纹（字段排序 + 规范化），用于检测"相同键不同请求"冲突。
 * 排除字段：时间戳、traceId、用户展示字段、密码、令牌、隐私字段。
 *
 * @author junsong
 */
public final class IdempotencyFingerprint {

    private static final ObjectMapper SORTED_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    /** 指纹计算时排除的字段名（不区分大小写包含匹配） */
    private static final String[] EXCLUDED_FIELDS = {
            "timestamp", "traceId", "createBy", "createByName",
            "createTime", "updateBy", "updateTime",
            "password", "token", "secret", "credential",
            "userDisplayName", "avatar", "remark"
    };

    private IdempotencyFingerprint() {}

    /**
     * 计算请求体的稳定指纹。
     *
     * @param body 请求体对象
     * @return SHA-256 指纹（64 位十六进制），null 表示无法计算
     */
    public static String compute(Object body) {
        if (body == null) {
            return "null";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = SORTED_MAPPER.convertValue(body, Map.class);
            Map<String, Object> filtered = filterExcluded(map);
            String json = SORTED_MAPPER.writeValueAsString(filtered);
            return sha256(json);
        } catch (Exception e) {
            // 无法序列化的对象，退化为 toString
            return sha256(String.valueOf(body));
        }
    }

    /**
     * 递归过滤排除字段。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> filterExcluded(Map<String, Object> map) {
        Map<String, Object> result = new TreeMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (isExcluded(e.getKey())) {
                continue;
            }
            Object v = e.getValue();
            if (v instanceof Map) {
                result.put(e.getKey(), filterExcluded((Map<String, Object>) v));
            } else {
                result.put(e.getKey(), v);
            }
        }
        return result;
    }

    private static boolean isExcluded(String fieldName) {
        if (fieldName == null) return false;
        String lower = fieldName.toLowerCase();
        for (String ex : EXCLUDED_FIELDS) {
            if (lower.contains(ex.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 是 JDK 标准算法，理论上不会失败
            return Integer.toHexString(input.hashCode());
        }
    }
}
