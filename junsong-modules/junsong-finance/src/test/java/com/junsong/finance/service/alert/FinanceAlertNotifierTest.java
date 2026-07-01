package com.junsong.finance.service.alert;

import com.junsong.common.core.domain.R;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.system.api.RemoteNotificationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FinanceAlertNotifier}.
 *
 * <p>Uses a hand-written fake (no Mockito) to record calls to RemoteNotificationService.</p>
 *
 * @author junsong
 * @since NIGHT-P1-R3B
 */
class FinanceAlertNotifierTest {

    // ─────────────────────────────────────────────────────────────────────────
    //  Fake: records all calls to RemoteNotificationService
    // ─────────────────────────────────────────────────────────────────────────

    static class FakeRemoteNotificationService implements RemoteNotificationService {
        final List<List<Map<String, Object>>> calls = new ArrayList<>();
        final List<String> sentBizIds = new ArrayList<>();
        boolean shouldThrow = false;
        boolean checkExistsShouldThrow = false;
        R<Boolean> responseToReturn = R.ok(true);

        @Override
        public R<Boolean> batchSendNotification(List<Map<String, Object>> notifications, String source) {
            if (shouldThrow) {
                throw new RuntimeException("Simulated network failure");
            }
            calls.add(notifications);
            for (Map<String, Object> n : notifications) {
                Object bizId = n.get("bizId");
                if (bizId != null) {
                    sentBizIds.add(bizId.toString());
                }
            }
            return responseToReturn;
        }

        @Override
        public R<Boolean> checkNotificationExists(Long userId, String type, String bizId, String source) {
            if (checkExistsShouldThrow) {
                throw new RuntimeException("Simulated check-exists failure");
            }
            return R.ok(sentBizIds.contains(bizId));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private FinanceDiagnosisResult makeResult(String ruleId, String alertLevel, Long deptId) {
        FinanceDiagnosisResult r = new FinanceDiagnosisResult();
        r.setRuleId(ruleId);
        r.setAlertLevel(alertLevel);
        r.setDeptId(deptId);
        r.setTitle("Test alert: " + ruleId);
        r.setReason("Test reason for " + ruleId);
        r.setTargetRoute("/finance/test");
        r.setMetricValue(BigDecimal.TEN);
        r.setCompareValue(BigDecimal.ONE);
        r.setImpactAmount(new BigDecimal("1000"));
        r.setSuggestedAction("Do something");
        return r;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LOW / MEDIUM alerts → no notification sent
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void lowAndMediumAlerts_shouldNotTriggerNotification() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("EXPENSE_SPIKE", "MEDIUM", 100L),
                makeResult("PENDING_VERIFY", "LOW", 100L)
        );

        notifier.notifyHighAlerts(results);

        assertTrue(fake.calls.isEmpty(), "No notification should be sent for LOW/MEDIUM alerts");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HIGH alert → notification sent
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void highAlert_shouldTriggerNotification() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 200L)
        );

        notifier.notifyHighAlerts(results);

        assertEquals(1, fake.calls.size(), "One batch call should be made");
        List<Map<String, Object>> batch = fake.calls.get(0);
        assertEquals(1, batch.size(), "One notification for one HIGH alert");

        Map<String, Object> n = batch.get(0);
        assertEquals(1L, n.get("userId"));
        assertEquals("Test alert: SALES_DROP", n.get("title"));
        assertEquals("Test reason for SALES_DROP", n.get("content"));
        assertEquals("finance_alert", n.get("type"));
        assertEquals("/finance/test", n.get("linkUrl"));
        assertNotNull(n.get("bizId"));
        assertTrue(n.get("bizId").toString().startsWith("FIN_ALERT:SALES_DROP:200:"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mixed alerts → only HIGH ones sent
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void mixedAlerts_shouldOnlyNotifyHigh() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 100L),
                makeResult("EXPENSE_SPIKE", "MEDIUM", 100L),
                makeResult("PENDING_VERIFY", "LOW", 100L),
                makeResult("MEMBER_CONTRIBUTION_DROP", "LOW", 100L)
        );

        notifier.notifyHighAlerts(results);

        assertEquals(1, fake.calls.size());
        assertEquals(1, fake.calls.get(0).size(), "Only the HIGH alert should be sent");
        assertEquals("Test alert: SALES_DROP", fake.calls.get(0).get(0).get("title"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BizId format and dedup consistency
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void sameAlertTwice_shouldGenerateSameBizId() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        FinanceDiagnosisResult result = makeResult("SALES_DROP", "HIGH", 300L);

        // First call: notification sent
        notifier.notifyHighAlerts(List.of(result));

        assertEquals(1, fake.calls.size());
        String bizId1 = fake.calls.get(0).get(0).get("bizId").toString();
        assertTrue(bizId1.matches("FIN_ALERT:SALES_DROP:300:\\d{8}"));

        // Second call: same bizId → dedup kicks in, no additional batch sent
        notifier.notifyHighAlerts(List.of(result));

        assertEquals(1, fake.calls.size(),
                "Same bizId on same day should be deduped — no second batch");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Notification failure doesn't throw
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void notificationFailure_shouldNotThrow() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        fake.shouldThrow = true;
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 100L)
        );

        assertDoesNotThrow(() -> notifier.notifyHighAlerts(results),
                "Notification failure must not propagate exceptions");
    }

    @Test
    void notificationFailureResponse_shouldNotThrow() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        fake.responseToReturn = R.fail("Service unavailable");
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 100L)
        );

        assertDoesNotThrow(() -> notifier.notifyHighAlerts(results),
                "Failed response must not throw");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Edge cases
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void nullInput_shouldNotThrow() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        assertDoesNotThrow(() -> notifier.notifyHighAlerts(null));
        assertTrue(fake.calls.isEmpty());
    }

    @Test
    void emptyInput_shouldNotCallRemote() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        notifier.notifyHighAlerts(Collections.emptyList());

        assertTrue(fake.calls.isEmpty());
    }

    @Test
    void multipleHighAlerts_shouldBatchAll() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 100L),
                makeResult("PROFIT_RATE_DROP", "HIGH", 200L)
        );

        notifier.notifyHighAlerts(results);

        assertEquals(1, fake.calls.size(), "Should batch into a single remote call");
        assertEquals(2, fake.calls.get(0).size(), "Both HIGH alerts should be in the batch");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Real dedup: same bizId on same day should be skipped
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    void notifyHighAlerts_doesNotSendDuplicateBizIdForSameUserAndDay() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        FinanceDiagnosisResult result = makeResult("SALES_DROP", "HIGH", 100L);

        // First call: checkExists returns false (sentBizIds is empty) → notification sent
        notifier.notifyHighAlerts(List.of(result));

        assertEquals(1, fake.calls.size(), "First call should send one batch");
        assertEquals(1, fake.calls.get(0).size(), "Batch should contain one notification");
        assertEquals(1, fake.sentBizIds.size(), "One bizId should be recorded as sent");

        // Second call (same bizId): checkExists returns true → notification skipped
        notifier.notifyHighAlerts(List.of(result));

        assertEquals(1, fake.calls.size(),
                "Second call with same bizId should NOT produce another batch send");
    }

    @Test
    void notifyHighAlerts_checkExistsFailure_stillSendsNotification() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        fake.checkExistsShouldThrow = true;
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 100L)
        );

        assertDoesNotThrow(() -> notifier.notifyHighAlerts(results),
                "checkExists failure must not propagate");
        assertEquals(1, fake.calls.size(),
                "When dedup check fails, notification should still be sent");
    }

    @Test
    void notifyHighAlerts_differentBizIds_bothSent() {
        FakeRemoteNotificationService fake = new FakeRemoteNotificationService();
        FinanceAlertNotifier notifier = new FinanceAlertNotifier(fake);

        List<FinanceDiagnosisResult> results = List.of(
                makeResult("SALES_DROP", "HIGH", 100L),
                makeResult("PROFIT_RATE_DROP", "HIGH", 200L)
        );

        notifier.notifyHighAlerts(results);

        assertEquals(1, fake.calls.size(), "One batch call");
        assertEquals(2, fake.calls.get(0).size(), "Both different bizIds should be sent");
    }
}
