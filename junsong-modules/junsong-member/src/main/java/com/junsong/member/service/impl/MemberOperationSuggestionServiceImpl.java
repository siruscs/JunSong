package com.junsong.member.service.impl;

import com.junsong.member.domain.vo.MemberOperationMetrics;
import com.junsong.member.domain.vo.MemberOperationSuggestionVO;
import com.junsong.member.service.IMemberOperationSuggestionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 会员经营建议服务实现：基于确定性规则生成建议。
 *
 * 规则：
 * - SILENT_MEMBER_HIGH: 沉默会员占比 > 30%
 * - REPURCHASE_LOW: 30 天复购率 < 15%
 * - FIRST_PURCHASE_LOW: 新会员首购率 < 20%
 * - POINTS_LIABILITY_HIGH: 积分沉淀成本估算 > 1000 元
 * - ACTIVITY_ROI_UNAVAILABLE: 活动 ROI 不可计算
 *
 * 纯逻辑实现，不依赖数据库，便于单测。
 */
@Service
public class MemberOperationSuggestionServiceImpl implements IMemberOperationSuggestionService {

    private static final BigDecimal SILENT_RATIO_THRESHOLD = new BigDecimal("30");
    private static final BigDecimal REPURCHASE_RATE_THRESHOLD = new BigDecimal("15");
    private static final BigDecimal FIRST_PURCHASE_RATE_THRESHOLD = new BigDecimal("20");
    private static final BigDecimal POINTS_LIABILITY_THRESHOLD = new BigDecimal("1000");

    @Override
    public List<MemberOperationSuggestionVO> generateSuggestions(MemberOperationMetrics metrics) {
        List<MemberOperationSuggestionVO> suggestions = new ArrayList<>();
        if (metrics == null) {
            return suggestions;
        }

        String yyyyMMdd = resolveBusinessDate(metrics.getBusinessDate());

        BigDecimal silentRatio = ratio(metrics.getSilentMemberCount(), metrics.getTotalMemberCount());
        if (silentRatio.compareTo(SILENT_RATIO_THRESHOLD) > 0) {
            suggestions.add(build("SILENT_MEMBER_HIGH", "HIGH", metrics.getDeptId(),
                    "沉默会员占比偏高",
                    "沉默会员 " + metrics.getSilentMemberCount() + " 人，占比 " + silentRatio + "%，超过 30% 警戒线。",
                    "筛选沉默会员名单，安排门店回访或定向权益唤醒。",
                    "/member/segment?segmentType=SILENT",
                    null, yyyyMMdd));
        }

        if (metrics.getRepurchaseRate30d() != null
                && metrics.getRepurchaseRate30d().compareTo(REPURCHASE_RATE_THRESHOLD) < 0) {
            suggestions.add(build("REPURCHASE_LOW", "MEDIUM", metrics.getDeptId(),
                    "30 天复购率偏低",
                    "近 30 天复购率 " + metrics.getRepurchaseRate30d() + "%，低于 15%。",
                    "检查活动设计、会员权益和首购引导，推动复购提升。",
                    "/member/overview",
                    null, yyyyMMdd));
        }

        if (metrics.getFirstPurchaseRate() != null
                && metrics.getFirstPurchaseRate().compareTo(FIRST_PURCHASE_RATE_THRESHOLD) < 0) {
            suggestions.add(build("FIRST_PURCHASE_LOW", "MEDIUM", metrics.getDeptId(),
                    "新会员首购率偏低",
                    "新会员首购率 " + metrics.getFirstPurchaseRate() + "%，低于 20%。",
                    "优化新会员首购引导，发放新人券或体验活动。",
                    "/member/segment?segmentType=NEW",
                    null, yyyyMMdd));
        }

        if (metrics.getPointsLiabilityAmount() != null
                && metrics.getPointsLiabilityAmount().compareTo(POINTS_LIABILITY_THRESHOLD) > 0) {
            suggestions.add(build("POINTS_LIABILITY_HIGH", "MEDIUM", metrics.getDeptId(),
                    "积分沉淀成本较高",
                    "积分负债估算 " + metrics.getPointsLiabilityAmount() + " 元，超过 1000 元警戒线。",
                    "配置积分兑换活动、提升积分消耗，降低负债压力。",
                    "/member/pointsGoods",
                    metrics.getPointsLiabilityAmount(), yyyyMMdd));
        }

        if (!metrics.isActivityRoiAvailable()) {
            suggestions.add(build("ACTIVITY_ROI_UNAVAILABLE", "LOW", metrics.getDeptId(),
                    "活动 ROI 不可计算",
                    "活动成本或销售关联缺失，无法计算 ROI。",
                    "补充活动成本录入或关联销售数据，启用 ROI 评估。",
                    "/member/seckill",
                    null, yyyyMMdd));
        }

        return suggestions;
    }

    /**
     * 复盘任务去重 key：MEMBER:{ruleCode}:{deptId}:{yyyyMMdd}
     * 同一规则、同一门店、同一天只生成一条复盘任务候选。
     */
    String buildDedupKey(String ruleCode, Long deptId, String yyyyMMdd) {
        return "MEMBER:" + ruleCode + ":" + (deptId == null ? "" : deptId) + ":" + yyyyMMdd;
    }

    private String resolveBusinessDate(String businessDate) {
        if (businessDate != null && !businessDate.trim().isEmpty()) {
            return businessDate;
        }
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private MemberOperationSuggestionVO build(String ruleCode, String severity, Long deptId,
                                              String title, String reason, String suggestion,
                                              String targetRoute, BigDecimal impactAmount,
                                              String yyyyMMdd) {
        MemberOperationSuggestionVO vo = new MemberOperationSuggestionVO();
        vo.setRuleCode(ruleCode);
        vo.setSeverity(severity);
        vo.setDeptId(deptId);
        vo.setTitle(title);
        vo.setReason(reason);
        vo.setSuggestion(suggestion);
        vo.setTargetRoute(targetRoute);
        vo.setImpactAmount(impactAmount);
        vo.setDedupKey(buildDedupKey(ruleCode, deptId, yyyyMMdd));
        return vo;
    }

    private BigDecimal ratio(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, BigDecimal.ROUND_HALF_UP);
    }
}
