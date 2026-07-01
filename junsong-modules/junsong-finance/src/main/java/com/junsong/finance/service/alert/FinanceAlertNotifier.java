package com.junsong.finance.service.alert;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.system.api.RemoteNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务预警通知器 —— 将 HIGH 级别诊断结果推送至系统通知中心。
 *
 * <p>设计原则：
 * <ul>
 *   <li>仅对 HIGH 级别发送通知</li>
 *   <li>bizId 格式: FIN_ALERT:{ruleId}:{deptId}:{yyyyMMdd}，同日同规则去重</li>
 *   <li>Best-effort：发送失败仅记录日志，不抛出异常</li>
 *   <li>去重策略：前置 Feign 查询去重（优化）+ 数据库唯一键兜底（保证）。
 *       数据库 uk_notification_user_type_biz (user_id, type, biz_id) 唯一键
 *       配合 INSERT IGNORE，确保并发场景下也不会插入重复通知。</li>
 * </ul>
 *
 * @author junsong
 * @since NIGHT-P1-R3B
 */
@Component
public class FinanceAlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(FinanceAlertNotifier.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RemoteNotificationService remoteNotificationService;

    @Autowired
    public FinanceAlertNotifier(RemoteNotificationService remoteNotificationService) {
        this.remoteNotificationService = remoteNotificationService;
    }

    /**
     * Send notifications for HIGH-severity diagnosis results.
     * This method is best-effort: failures are logged but never thrown.
     *
     * @param results diagnosis results from the rule engine
     */
    public void notifyHighAlerts(List<FinanceDiagnosisResult> results) {
        if (results == null || results.isEmpty()) return;

        List<Map<String, Object>> notifications = new ArrayList<>();
        String today = LocalDate.now().format(DATE_FMT);

        for (FinanceDiagnosisResult r : results) {
            if (!"HIGH".equals(r.getAlertLevel())) continue;

            String bizId = "FIN_ALERT:" + r.getRuleId() + ":" + r.getDeptId() + ":" + today;

            // Dedup (pre-check optimization): reduce redundant remote calls.
            // The authoritative dedup is enforced by the DB unique key
            // uk_notification_user_type_biz + INSERT IGNORE on the insert side.
            Long userId = 1L; // MVP: admin user
            try {
                R<Boolean> exists = remoteNotificationService.checkNotificationExists(
                        userId, "finance_alert", bizId, SecurityConstants.INNER);
                if (exists != null && exists.getCode() == R.SUCCESS && Boolean.TRUE.equals(exists.getData())) {
                    log.debug("[FinanceAlertNotifier] 跳过已存在通知: bizId={}", bizId);
                    continue; // Skip this notification
                }
            } catch (Exception e) {
                log.warn("[FinanceAlertNotifier] 去重查询失败，仍然发送通知", e);
                // Proceed to send anyway
            }

            Map<String, Object> n = new HashMap<>();
            // For MVP, send to admin user (userId=1). In future, resolve by permission.
            n.put("userId", userId);
            n.put("title", r.getTitle());
            n.put("content", r.getReason());
            n.put("type", "finance_alert");
            n.put("linkUrl", r.getTargetRoute());
            n.put("bizId", bizId);
            notifications.add(n);
        }

        if (notifications.isEmpty()) return;

        try {
            R<Boolean> result = remoteNotificationService.batchSendNotification(
                    notifications, SecurityConstants.INNER);
            if (result != null && result.getCode() == R.SUCCESS) {
                log.info("[FinanceAlertNotifier] 成功发送 {} 条预警通知", notifications.size());
            } else {
                log.warn("[FinanceAlertNotifier] 通知发送返回异常: {}", result != null ? result.getMsg() : "null");
            }
        } catch (Exception e) {
            log.error("[FinanceAlertNotifier] 通知发送失败，不影响预警接口返回", e);
        }
    }
}
