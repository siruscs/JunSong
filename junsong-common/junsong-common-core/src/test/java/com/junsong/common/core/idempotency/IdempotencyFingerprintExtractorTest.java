package com.junsong.common.core.idempotency;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 幂等指纹参数提取器测试。
 *
 * 核心验证：当方法签名为
 *   foo(@RequestHeader("X-Idempotency-Key") String key, @RequestBody Req body)
 * 时，指纹基于 body 而非 key 计算，不同 body 应得到不同指纹。
 *
 * @author junsong
 */
class IdempotencyFingerprintExtractorTest {

    /**
     * 辅助方法：模拟 JoinPoint，绑定到指定方法并传入指定参数。
     */
    private JoinPoint mockJoinPoint(String methodName, Object[] args) throws Exception {
        JoinPoint point = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = findMethod(methodName);
        when(signature.getMethod()).thenReturn(method);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(args);
        return point;
    }

    private Method findMethod(String name) throws Exception {
        for (Method m : IdempotencyFingerprintExtractorTest.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new NoSuchMethodException(name);
    }

    // 测试用方法签名（返回 void 即可，测试只关心参数上的注解）

    /** 模拟 FinSaleRecordController.add 的签名 */
    public void saleAdd(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody java.util.Map<String, Object> body) {
    }

    /** 只有 @RequestBody */
    public void onlyBody(@RequestBody java.util.Map<String, Object> body) {
    }

    /** @PathVariable + @RequestBody */
    public void pathAndBody(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
    }

    /** @RequestHeader + @PathVariable + @RequestBody */
    public void headerAndPathAndBody(
            @RequestHeader("X-Idempotency-Key") String key,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
    }

    /** @RequestParam（版本号）+ 无 body */
    public void versionOnly(@RequestParam Integer version) {
    }

    /** @PathVariable + @RequestParam（版本号） */
    public void pathAndVersion(
            @PathVariable Long id,
            @RequestParam Integer version) {
    }

    /** 无参数 */
    public void noArgs() {
    }

    // =========================================================================
    // 核心测试：@RequestHeader 在第一个参数时，指纹基于 @RequestBody 计算
    // =========================================================================

    @Test
    void headerFirstThenBody_fingerprintBasedOnBodyNotHeader() throws Exception {
        java.util.Map<String, Object> body1 = new java.util.LinkedHashMap<>();
        body1.put("amount", 100);
        body1.put("productId", 42);

        java.util.Map<String, Object> body2 = new java.util.LinkedHashMap<>();
        body2.put("amount", 200);
        body2.put("productId", 42);

        // 相同 header key，不同 body → 应得到不同指纹
        JoinPoint p1 = mockJoinPoint("saleAdd", new Object[]{"same-key-header", body1});
        JoinPoint p2 = mockJoinPoint("saleAdd", new Object[]{"same-key-header", body2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotNull(fp1);
        assertNotEquals(fp1, fp2, "不同 body 必须产生不同指纹（即使 header 相同）");
    }

    @Test
    void headerFirstThenBody_sameBodyDifferentHeader_sameFingerprint() throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("amount", 100);

        // 相同 body，不同 header key → 应得到相同指纹
        JoinPoint p1 = mockJoinPoint("saleAdd", new Object[]{"key-A", body});
        JoinPoint p2 = mockJoinPoint("saleAdd", new Object[]{"key-B", body});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertEquals(fp1, fp2, "相同 body 不同 header 必须产生相同指纹");
    }

    // =========================================================================
    // 只有 @RequestBody
    // =========================================================================

    @Test
    void onlyBody_differentBodyDifferentFingerprint() throws Exception {
        java.util.Map<String, Object> body1 = new java.util.LinkedHashMap<>();
        body1.put("amount", 100);

        java.util.Map<String, Object> body2 = new java.util.LinkedHashMap<>();
        body2.put("amount", 200);

        JoinPoint p1 = mockJoinPoint("onlyBody", new Object[]{body1});
        JoinPoint p2 = mockJoinPoint("onlyBody", new Object[]{body2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2, "不同 body 必须产生不同指纹");
    }

    // =========================================================================
    // @PathVariable + @RequestBody
    // =========================================================================

    @Test
    void pathAndBody_combinesPathAndBody() throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("amount", 100);

        JoinPoint p1 = mockJoinPoint("pathAndBody", new Object[]{1L, body});
        JoinPoint p2 = mockJoinPoint("pathAndBody", new Object[]{2L, body});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2, "不同 pathVariable 必须产生不同指纹");
    }

    // =========================================================================
    // @RequestHeader + @PathVariable + @RequestBody
    // =========================================================================

    @Test
    void headerAndPathAndBody_ignoresHeaderIncludesPathAndBody() throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("amount", 100);

        // 相同 header，不同 pathVariable → 不同指纹
        JoinPoint p1 = mockJoinPoint("headerAndPathAndBody", new Object[]{"same-key", 1L, body});
        JoinPoint p2 = mockJoinPoint("headerAndPathAndBody", new Object[]{"same-key", 2L, body});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2, "不同 pathVariable 必须产生不同指纹（忽略 header）");
    }

    // =========================================================================
    // 只有 @RequestParam（版本号）
    // =========================================================================

    @Test
    void versionOnly_differentVersionDifferentFingerprint() throws Exception {
        JoinPoint p1 = mockJoinPoint("versionOnly", new Object[]{1});
        JoinPoint p2 = mockJoinPoint("versionOnly", new Object[]{2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2, "不同 version 必须产生不同指纹");
    }

    // =========================================================================
    // @PathVariable + @RequestParam（版本号）
    // =========================================================================

    @Test
    void pathAndVersion_combinesBoth() throws Exception {
        JoinPoint p1 = mockJoinPoint("pathAndVersion", new Object[]{1L, 1});
        JoinPoint p2 = mockJoinPoint("pathAndVersion", new Object[]{1L, 2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2, "不同 version 必须产生不同指纹");
    }

    // =========================================================================
    // 无参数
    // =========================================================================

    @Test
    void noArgs_returnsNullFingerprint() throws Exception {
        JoinPoint p = mockJoinPoint("noArgs", new Object[0]);
        String fp = IdempotencyFingerprintExtractor.extract(p);
        assertEquals("null", fp, "无参数应返回 'null' 指纹");
    }

    // =========================================================================
    // 边界：null 参数值
    // =========================================================================

    @Test
    void nullArgsReturnsNullFingerprint() throws Exception {
        JoinPoint point = mock(JoinPoint.class);
        when(point.getArgs()).thenReturn(null);
        String fp = IdempotencyFingerprintExtractor.extract(point);
        assertEquals("null", fp);
    }

    @Test
    void emptyArgsReturnsNullFingerprint() throws Exception {
        JoinPoint point = mock(JoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[0]);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(findMethod("noArgs"));
        when(point.getSignature()).thenReturn(signature);
        String fp = IdempotencyFingerprintExtractor.extract(point);
        assertEquals("null", fp);
    }
}
