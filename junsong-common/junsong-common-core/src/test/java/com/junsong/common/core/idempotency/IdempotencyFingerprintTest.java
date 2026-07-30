package com.junsong.common.core.idempotency;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等指纹工具测试。
 *
 * 覆盖：
 * - 相同对象相同指纹
 * - 字段顺序无关
 * - 排除字段（timestamp/createBy/password 等）不参与指纹
 * - null 输入
 * - 不同对象不同指纹
 */
class IdempotencyFingerprintTest {

    @Test
    void nullBodyReturnsConstantFingerprint() {
        assertEquals("null", IdempotencyFingerprint.compute(null));
    }

    @Test
    void sameMapProducesSameFingerprint() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", 100);
        body.put("productId", 42);
        body.put("deptId", 1);

        String fp1 = IdempotencyFingerprint.compute(body);
        String fp2 = IdempotencyFingerprint.compute(body);
        assertEquals(fp1, fp2, "相同对象应产生相同指纹");
    }

    @Test
    void fieldOrderDoesNotAffectFingerprint() {
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("amount", 100);
        body1.put("productId", 42);

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("productId", 42);
        body2.put("amount", 100);

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "字段顺序不同应产生相同指纹"
        );
    }

    @Test
    void excludedFieldsDoNotAffectFingerprint() {
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("amount", 100);
        body1.put("timestamp", 1700000000000L);
        body1.put("createBy", "admin");
        body1.put("traceId", "abc-123");

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("amount", 100);
        body2.put("timestamp", 1800000000000L);
        body2.put("createBy", "guest");
        body2.put("traceId", "xyz-789");

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "排除字段（timestamp/createBy/traceId）不应影响指纹"
        );
    }

    @Test
    void passwordFieldExcludedFromFingerprint() {
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("username", "alice");
        body1.put("password", "secret123");

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("username", "alice");
        body2.put("password", "different456");

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "password 字段必须被排除，不得参与指纹"
        );
    }

    @Test
    void tokenFieldExcludedFromFingerprint() {
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("action", "submit");
        body1.put("token", "token-aaa");

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("action", "submit");
        body2.put("token", "token-bbb");

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "token 字段必须被排除"
        );
    }

    @Test
    void differentValuesProduceDifferentFingerprints() {
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("amount", 100);

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("amount", 200);

        assertNotEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "不同值应产生不同指纹"
        );
    }

    @Test
    void nestedMapFieldOrderDoesNotAffectFingerprint() {
        Map<String, Object> nested1 = new LinkedHashMap<>();
        nested1.put("x", 1);
        nested1.put("y", 2);

        Map<String, Object> nested2 = new LinkedHashMap<>();
        nested2.put("y", 2);
        nested2.put("x", 1);

        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("data", nested1);

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("data", nested2);

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "嵌套 Map 字段顺序不同应产生相同指纹"
        );
    }

    @Test
    void fingerprintIsSha256Hex64Chars() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", 100);

        String fp = IdempotencyFingerprint.compute(body);
        assertNotNull(fp);
        assertEquals(64, fp.length(), "SHA-256 指纹应为 64 位十六进制字符");
        assertTrue(fp.matches("^[0-9a-f]{64}$"), "指纹应为十六进制字符串");
    }

    @Test
    void nestedExcludedFieldDoesNotAffectFingerprint() {
        Map<String, Object> nested1 = new LinkedHashMap<>();
        nested1.put("value", 42);
        nested1.put("createTime", "2026-01-01");

        Map<String, Object> nested2 = new LinkedHashMap<>();
        nested2.put("value", 42);
        nested2.put("createTime", "2026-12-31");

        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("item", nested1);

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("item", nested2);

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "嵌套对象中的排除字段也不应影响指纹"
        );
    }

    @Test
    void caseInsensitiveExcludedFieldMatch() {
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("amount", 100);
        body1.put("CreateTime", "2026-01-01");

        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("amount", 100);
        body2.put("createTime", "2026-12-31");

        assertEquals(
                IdempotencyFingerprint.compute(body1),
                IdempotencyFingerprint.compute(body2),
                "排除字段匹配应不区分大小写"
        );
    }
}
