package com.junsong.finance.idempotency;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.junsong.common.core.idempotency.IdempotencyFingerprintExtractor;
import com.junsong.finance.controller.FinSaleRecordController;
import com.junsong.finance.controller.FinPurchaseController;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.FinPurchase;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真实 Controller 集成测试：验证 IdempotencyFingerprintExtractor 在生产 Controller 方法签名上的正确性。
 *
 * <h2>测试目标</h2>
 * <p>用户第二轮复核明确要求："必须增加真实 Controller 集成测试，验证如下签名：
 * <pre>
 * method(
 *     @RequestHeader String key,
 *     @PathVariable Long id,
 *     @RequestBody Request request
 * )
 * </pre>
 * 不同 id 或不同 request 必须得到不同指纹。"
 *
 * <p>本测试通过反射访问真实的 {@link FinSaleRecordController#addPayment} 方法
 * （签名：@RequestHeader + @PathVariable Long saleId + @RequestBody Map params），
 * 验证提取器在生产代码上的行为，而非测试类自定义的方法签名。
 *
 * <h2>验证场景</h2>
 * <ol>
 *   <li>不同 saleId + 相同 params → 不同指纹（PathVariable 参与计算）</li>
 *   <li>相同 saleId + 不同 params → 不同指纹（RequestBody 参与计算）</li>
 *   <li>不同 idempotencyKey + 相同 saleId + 相同 params → 相同指纹（@RequestHeader 被排除）</li>
 *   <li>真实 FinSaleRecordController.add（@RequestHeader + @RequestBody）签名验证</li>
 *   <li>真实 FinPurchaseController.add（@RequestHeader + @RequestBody）签名验证</li>
 * </ol>
 *
 * @author junsong
 */
@DisplayName("真实 Controller 指纹提取集成测试")
class RealControllerFingerprintIntegrationTest {

    /**
     * 辅助方法：基于真实 Controller 方法构造 JoinPoint mock。
     *
     * @param controllerClass 真实 Controller 类
     * @param methodName 方法名
     * @param paramTypes 参数类型列表
     * @param args 参数值列表
     */
    private JoinPoint mockJoinPointFromRealMethod(
            Class<?> controllerClass, String methodName, Class<?>[] paramTypes, Object[] args)
            throws NoSuchMethodException {
        Method method = controllerClass.getDeclaredMethod(methodName, paramTypes);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        JoinPoint point = mock(JoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(args);
        return point;
    }

    // =========================================================================
    // 场景 1：FinSaleRecordController.addPayment 真实方法签名
    //   @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
    //   @PathVariable Long saleId,
    //   @RequestBody java.util.Map<String, Object> params
    // =========================================================================

    @Test
    @DisplayName("addPayment：不同 saleId + 相同 params → 不同指纹（PathVariable 参与计算）")
    void addPayment_differentSaleIdSameParams_differentFingerprint() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("amount", 100);
        params.put("paymentType", "CASH");

        // 相同 header key，不同 saleId → 应得到不同指纹
        JoinPoint p1 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "addPayment",
                new Class<?>[]{String.class, Long.class, Map.class},
                new Object[]{"same-key", 1001L, params});
        JoinPoint p2 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "addPayment",
                new Class<?>[]{String.class, Long.class, Map.class},
                new Object[]{"same-key", 1002L, params});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotNull(fp1, "saleId=1001 的指纹不应为 null");
        assertNotNull(fp2, "saleId=1002 的指纹不应为 null");
        assertNotEquals(fp1, fp2,
                "不同 saleId 必须产生不同指纹（PathVariable 参与计算，解决同键不同请求问题）");
    }

    @Test
    @DisplayName("addPayment：相同 saleId + 不同 params → 不同指纹（RequestBody 参与计算）")
    void addPayment_sameSaleIdDifferentParams_differentFingerprint() throws Exception {
        Map<String, Object> params1 = new LinkedHashMap<>();
        params1.put("amount", 100);
        params1.put("paymentType", "CASH");

        Map<String, Object> params2 = new LinkedHashMap<>();
        params2.put("amount", 200);
        params2.put("paymentType", "CASH");

        // 相同 header key + 相同 saleId + 不同 params → 应得到不同指纹
        JoinPoint p1 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "addPayment",
                new Class<?>[]{String.class, Long.class, Map.class},
                new Object[]{"same-key", 1001L, params1});
        JoinPoint p2 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "addPayment",
                new Class<?>[]{String.class, Long.class, Map.class},
                new Object[]{"same-key", 1001L, params2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2,
                "不同 params 必须产生不同指纹（RequestBody 参与计算）");
    }

    @Test
    @DisplayName("addPayment：不同 idempotencyKey + 相同 saleId + 相同 params → 相同指纹（@RequestHeader 被排除）")
    void addPayment_differentHeaderSameBodySameFingerprint() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("amount", 100);
        params.put("paymentType", "CASH");

        // 不同 header key + 相同 saleId + 相同 params → 应得到相同指纹
        JoinPoint p1 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "addPayment",
                new Class<?>[]{String.class, Long.class, Map.class},
                new Object[]{"key-A", 1001L, params});
        JoinPoint p2 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "addPayment",
                new Class<?>[]{String.class, Long.class, Map.class},
                new Object[]{"key-B", 1001L, params});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertEquals(fp1, fp2,
                "相同 saleId + 相同 params 不同 header 必须产生相同指纹（@RequestHeader 被排除）");
    }

    // =========================================================================
    // 场景 2：FinSaleRecordController.add 真实方法签名
    //   @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
    //   @Validated @RequestBody FinSaleRecord finSaleRecord
    // =========================================================================

    @Test
    @DisplayName("saleAdd：不同 FinSaleRecord body → 不同指纹（真实 domain 对象）")
    void saleAdd_differentBodyDifferentFingerprint() throws Exception {
        FinSaleRecord body1 = new FinSaleRecord();
        body1.setSaleAmount(java.math.BigDecimal.valueOf(100.00));
        body1.setProductId(42L);

        FinSaleRecord body2 = new FinSaleRecord();
        body2.setSaleAmount(java.math.BigDecimal.valueOf(200.00));
        body2.setProductId(42L);

        // 相同 header key，不同 body → 应得到不同指纹
        JoinPoint p1 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "add",
                new Class<?>[]{String.class, FinSaleRecord.class},
                new Object[]{"same-key", body1});
        JoinPoint p2 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "add",
                new Class<?>[]{String.class, FinSaleRecord.class},
                new Object[]{"same-key", body2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2,
                "不同 FinSaleRecord body 必须产生不同指纹（真实 domain 对象参与计算）");
    }

    @Test
    @DisplayName("saleAdd：相同 body + 不同 header → 相同指纹（@RequestHeader 被排除）")
    void saleAdd_sameBodyDifferentHeaderSameFingerprint() throws Exception {
        FinSaleRecord body = new FinSaleRecord();
        body.setSaleAmount(java.math.BigDecimal.valueOf(100.00));
        body.setProductId(42L);

        JoinPoint p1 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "add",
                new Class<?>[]{String.class, FinSaleRecord.class},
                new Object[]{"key-A", body});
        JoinPoint p2 = mockJoinPointFromRealMethod(
                FinSaleRecordController.class, "add",
                new Class<?>[]{String.class, FinSaleRecord.class},
                new Object[]{"key-B", body});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertEquals(fp1, fp2,
                "相同 body 不同 header 必须产生相同指纹");
    }

    // =========================================================================
    // 场景 3：FinPurchaseController.add 真实方法签名（跨 Controller 验证）
    // =========================================================================

    @Test
    @DisplayName("purchaseAdd：不同 FinPurchase body → 不同指纹（跨 Controller 验证）")
    void purchaseAdd_differentBodyDifferentFingerprint() throws Exception {
        FinPurchase body1 = new FinPurchase();
        body1.setTotalAmount(java.math.BigDecimal.valueOf(500.00));

        FinPurchase body2 = new FinPurchase();
        body2.setTotalAmount(java.math.BigDecimal.valueOf(999.00));

        JoinPoint p1 = mockJoinPointFromRealMethod(
                FinPurchaseController.class, "add",
                new Class<?>[]{String.class, FinPurchase.class},
                new Object[]{"same-key", body1});
        JoinPoint p2 = mockJoinPointFromRealMethod(
                FinPurchaseController.class, "add",
                new Class<?>[]{String.class, FinPurchase.class},
                new Object[]{"same-key", body2});

        String fp1 = IdempotencyFingerprintExtractor.extract(p1);
        String fp2 = IdempotencyFingerprintExtractor.extract(p2);

        assertNotEquals(fp1, fp2,
                "不同 FinPurchase body 必须产生不同指纹（跨 Controller 验证）");
    }

    // =========================================================================
    // 场景 4：验证真实方法参数上的注解类型（反射元数据验证）
    // =========================================================================

    @Test
    @DisplayName("addPayment 方法签名元数据：第一个参数是 @RequestHeader，第二是 @PathVariable，第三是 @RequestBody")
    void addPayment_methodSignatureAnnotations_correct() throws Exception {
        Method method = FinSaleRecordController.class.getDeclaredMethod(
                "addPayment", String.class, Long.class, Map.class);

        java.lang.reflect.Parameter[] params = method.getParameters();
        assertEquals(3, params.length, "addPayment 应有 3 个参数");

        // 第一个参数：@RequestHeader（必须被提取器排除）
        assertNotNull(params[0].getAnnotation(RequestHeader.class),
                "第一个参数必须是 @RequestHeader");

        // 第二个参数：@PathVariable（必须参与指纹计算）
        assertNotNull(params[1].getAnnotation(PathVariable.class),
                "第二个参数必须是 @PathVariable");

        // 第三个参数：@RequestBody（必须参与指纹计算）
        assertNotNull(params[2].getAnnotation(RequestBody.class),
                "第三个参数必须是 @RequestBody");
    }

    @Test
    @DisplayName("saleAdd 方法签名元数据：第一个参数是 @RequestHeader，第二是 @RequestBody")
    void saleAdd_methodSignatureAnnotations_correct() throws Exception {
        Method method = FinSaleRecordController.class.getDeclaredMethod(
                "add", String.class, FinSaleRecord.class);

        java.lang.reflect.Parameter[] params = method.getParameters();
        assertEquals(2, params.length, "add 应有 2 个参数");

        assertNotNull(params[0].getAnnotation(RequestHeader.class),
                "第一个参数必须是 @RequestHeader");
        assertNotNull(params[1].getAnnotation(RequestBody.class),
                "第二个参数必须是 @RequestBody");
    }
}
