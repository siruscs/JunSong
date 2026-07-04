package com.junsong.member.service.impl;

import com.junsong.common.core.domain.R;
import com.junsong.member.api.MemberActionPredictionQuery;
import com.junsong.member.api.RemoteMemberPredictionService;
import com.junsong.member.domain.vo.GrowthActionQueryParams;
import com.junsong.member.domain.vo.GrowthActionRowVO;
import com.junsong.member.domain.vo.MemberActionPredictionVO;
import com.junsong.member.service.IMemberActionPredictionService;
import com.junsong.member.service.IMemberGrowthActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * R24 会员动作预测服务实现。
 *
 * <p>使用可解释规则：基于 R17 动作的完成度、压力等级、状态，
 * 不引入机器学习或黑盒评分。不修改 {@code mem_growth_action} 或会员等级。</p>
 */
@Service
public class MemberActionPredictionServiceImpl implements IMemberActionPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MemberActionPredictionServiceImpl.class);

    private static final String LOW = "LOW";
    private static final String MEDIUM = "MEDIUM";
    private static final String HIGH = "HIGH";
    private static final String CRITICAL = "CRITICAL";

    private static final String NO_DATA_BASIS = "无会员动作历史样本，按 LOW 等级处理，等待 R17 动作完成后再评估";

    @Autowired
    private IMemberGrowthActionService growthActionService;

    @Autowired
    private RemoteMemberPredictionService remoteMemberPredictionService;

    @Override
    public List<MemberActionPredictionVO> listActionPredictions(Long deptId, Integer windowDays, String actionType) {
        if (windowDays == null || windowDays <= 0) {
            windowDays = 30;
        }

        // 1) 拉取最近的 R17 动作作为预测输入
        List<GrowthActionRowVO> recentActions = safeListRecentActions(deptId);

        // 2) 通过远端 Feign 探测会员预测服务可用性（仅记日志，不依赖远端数据）
        probeRemoteService(deptId, windowDays, actionType);

        // 3) 规则打分
        if (recentActions.isEmpty()) {
            MemberActionPredictionVO empty = new MemberActionPredictionVO();
            empty.setDeptId(deptId);
            empty.setLevel(LOW);
            empty.setScore(0);
            empty.setRecentActiveRate(BigDecimal.ZERO);
            empty.setHistoricalEffectRate(BigDecimal.ZERO);
            empty.setBasis(NO_DATA_BASIS);
            empty.setRecommendation("按现有节奏继续观察，等待 R17 动作完成后再评估");
            List<MemberActionPredictionVO> list = new ArrayList<>();
            list.add(empty);
            return list;
        }

        List<MemberActionPredictionVO> result = new ArrayList<>();
        for (GrowthActionRowVO row : recentActions) {
            result.add(scoreAction(row, deptId));
        }
        return result;
    }

    private List<GrowthActionRowVO> safeListRecentActions(Long deptId) {
        try {
            GrowthActionQueryParams listParams = new GrowthActionQueryParams();
            listParams.setDeptId(deptId);
            // 复用 R17 仪表盘的 recentActions（已完成 7 天窗口回填）
            return growthActionService.getDashboard(listParams) == null
                    ? new ArrayList<>()
                    : growthActionService.getDashboard(listParams).getRecentActions() == null
                        ? new ArrayList<>()
                        : growthActionService.getDashboard(listParams).getRecentActions();
        } catch (Exception e) {
            log.warn("R24 拉取 R17 动作失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void probeRemoteService(Long deptId, Integer windowDays, String actionType) {
        try {
            MemberActionPredictionQuery remoteQuery = new MemberActionPredictionQuery();
            remoteQuery.setDeptId(deptId);
            remoteQuery.setWindowDays(windowDays);
            remoteQuery.setActionType(actionType);
            R<List<com.junsong.member.api.domain.MemberActionPredictionItem>> remoteResp =
                    remoteMemberPredictionService.listMemberActionPredictions(remoteQuery, "inner");
            if (remoteResp != null && remoteResp.getCode() != R.SUCCESS) {
                log.info("R24 会员预测远端不可用 (deptId={}): {}", deptId, remoteResp.getMsg());
            }
        } catch (Exception e) {
            log.debug("R24 会员预测远端调用跳过: {}", e.getMessage());
        }
    }

    private MemberActionPredictionVO scoreAction(GrowthActionRowVO row, Long deptId) {
        MemberActionPredictionVO vo = new MemberActionPredictionVO();
        vo.setActionId(row.getActionId());
        vo.setActionTitle(row.getActionTitle());
        vo.setActionType(row.getActionType());
        vo.setDeptId(row.getDeptId() == null ? deptId : row.getDeptId());
        vo.setDeptName(row.getDeptName());

        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 规则 1: 候选会员完成率低（executed/candidate < 50%） +25
        Integer candidate = row.getCandidateCount() == null ? 0 : row.getCandidateCount();
        Integer executed = row.getExecutedCount() == null ? 0 : row.getExecutedCount();
        BigDecimal completionRate = BigDecimal.ZERO;
        if (candidate > 0) {
            completionRate = new BigDecimal(executed)
                    .divide(new BigDecimal(candidate), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (completionRate.compareTo(new BigDecimal("50")) < 0) {
                score += 25;
                reasons.add("动作完成率偏低（" + completionRate.setScale(1, RoundingMode.HALF_UP) + "%）");
            } else if (completionRate.compareTo(new BigDecimal("80")) >= 0) {
                score -= 10;
                reasons.add("动作完成率良好（" + completionRate.setScale(1, RoundingMode.HALF_UP) + "%）");
            }
        }
        vo.setHistoricalEffectRate(completionRate);

        // 规则 2: 候选人数过多（candidateCount > 50） +15
        if (candidate > 50) {
            score += 15;
            reasons.add("候选会员数过多(" + candidate + ")，建议分批执行");
        }

        // 规则 3: 现金压力高（CRITICAL/HIGH） +25
        String pressureLevel = row.getPressureLevel();
        if (CRITICAL.equals(pressureLevel) || HIGH.equals(pressureLevel)) {
            score += 25;
            reasons.add("门店现金压力高(" + pressureLevel + ")，会员动作需谨慎");
        } else if (LOW.equals(pressureLevel)) {
            score -= 5;
            reasons.add("门店现金压力低，会员动作可积极推进");
        }

        // 规则 4: 状态为 PENDING +10
        if ("PENDING".equalsIgnoreCase(row.getStatus())) {
            score += 10;
            reasons.add("动作状态仍为 PENDING，需要执行人介入");
        } else if ("DONE".equalsIgnoreCase(row.getStatus())) {
            score -= 5;
            reasons.add("动作已 DONE，效果复盘中");
        }

        // 规则 5: 门店长期未活跃（基于 actionReason 文本启发） +5
        if (row.getActionReason() != null && row.getActionReason().contains("未活跃")) {
            score += 5;
            reasons.add("候选会员长期未活跃");
        }

        score = Math.max(0, Math.min(100, score));
        vo.setScore(score);
        vo.setLevel(resolveLevel(score));
        vo.setRecentActiveRate(candidate == 0 ? BigDecimal.ZERO
                : new BigDecimal(executed).divide(new BigDecimal(candidate), 4, RoundingMode.HALF_UP));
        vo.setBasis(buildBasis(reasons));
        vo.setRecommendation(buildRecommendation(vo.getLevel(), pressureLevel));
        return vo;
    }

    private String resolveLevel(int score) {
        if (score >= 80) return CRITICAL;
        if (score >= 60) return HIGH;
        if (score >= 30) return MEDIUM;
        return LOW;
    }

    private String buildBasis(List<String> reasons) {
        if (reasons.isEmpty()) {
            return "未命中显著风险因子，按 LOW 等级处理";
        }
        return String.join("；", reasons);
    }

    private String buildRecommendation(String level, String pressureLevel) {
        if (CRITICAL.equals(level)) {
            return "建议负责人当天复核，必要时缩减会员动作规模";
        }
        if (HIGH.equals(level)) {
            return "建议进入动作中心人工处理，优先 " + (pressureLevel == null ? "高压力" : pressureLevel) + " 压力门店";
        }
        if (MEDIUM.equals(level)) {
            return "在经营例会中关注，并跟踪候选会员活跃变化";
        }
        return "按现有节奏跟进，无需额外动作";
    }
}
