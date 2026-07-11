package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;

import com.junsong.member.domain.MemGrowthAction;
import com.junsong.member.domain.MemGrowthActionMember;
import com.junsong.member.domain.vo.GrowthActionCandidateVO;
import com.junsong.member.domain.vo.GrowthActionDashboardVO;
import com.junsong.member.domain.vo.GrowthActionEffectVO;
import com.junsong.member.domain.vo.GrowthActionExecuteParams;
import com.junsong.member.domain.vo.GrowthActionGenerateParams;
import com.junsong.member.domain.vo.GrowthActionQueryParams;
import com.junsong.member.domain.vo.GrowthActionRowVO;
import com.junsong.member.mapper.MemberGrowthActionMapper;
import com.junsong.member.service.IMemberGrowthActionService;
import com.junsong.system.api.model.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 会员增长动作Service实现
 *
 * @author junsong
 */
@Service
public class MemberGrowthActionServiceImpl implements IMemberGrowthActionService
{
    private static final Logger log = LoggerFactory.getLogger(MemberGrowthActionServiceImpl.class);

    @Autowired
    private MemberGrowthActionMapper memberGrowthActionMapper;

    // 候选会员分层常量
    private static final String SLEEPING_HIGH_VALUE = "SLEEPING_HIGH_VALUE";
    private static final String NEAR_LEVEL_UP = "NEAR_LEVEL_UP";
    private static final String RECENT_ACTIVE_NO_REPEAT = "RECENT_ACTIVE_NO_REPEAT";
    private static final String PRESSURE_STORE_RECALL = "PRESSURE_STORE_RECALL";

    // 动作类型常量
    private static final String ACTION_RECALL_VISIT = "RECALL_VISIT";
    private static final String ACTION_LEVEL_UP_NUDGE = "LEVEL_UP_NUDGE";
    private static final String ACTION_SIGN_IN_RECOVER = "SIGN_IN_RECOVER";
    private static final String ACTION_REPEAT_PURCHASE = "REPEAT_PURCHASE";

    @Override
    public GrowthActionDashboardVO getDashboard(GrowthActionQueryParams params)
    {
        if (params == null) {
            params = new GrowthActionQueryParams();
        }

        GrowthActionDashboardVO dashboard = new GrowthActionDashboardVO();

        // 查询候选会员前 30 条
        List<GrowthActionCandidateVO> candidates = memberGrowthActionMapper.selectCandidates(params);
        if (candidates != null && candidates.size() > 30) {
            candidates = candidates.subList(0, 30);
        }
        dashboard.setCandidates(candidates != null ? candidates : new ArrayList<>());

        // 查询最近动作前 20 条
        List<GrowthActionRowVO> recentActions = memberGrowthActionMapper.selectRecentActions(params);
        dashboard.setRecentActions(recentActions != null ? recentActions : new ArrayList<>());

        // 查询 effectSummary
        GrowthActionEffectVO effectSummary = memberGrowthActionMapper.selectEffectSummary(params);
        dashboard.setEffectSummary(effectSummary != null ? effectSummary : createEmptyEffect());

        // 统计 KPI
        dashboard.setPendingActionCount(safeCount(memberGrowthActionMapper.countPendingActions(params)));
        dashboard.setPendingMemberCount(safeCount(memberGrowthActionMapper.countPendingMembers(params)));
        dashboard.setExecutedMemberCount(safeCount(memberGrowthActionMapper.countExecutedMembers(params)));

        if (effectSummary != null && effectSummary.getTotalMemberCount() != null && effectSummary.getTotalMemberCount() > 0) {
            dashboard.setEffectiveMemberCount(effectSummary.getEffectiveMemberCount());
            BigDecimal rate = new BigDecimal(effectSummary.getEffectiveMemberCount())
                    .divide(new BigDecimal(effectSummary.getTotalMemberCount()), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            dashboard.setEffectRate(rate);
        } else {
            dashboard.setEffectiveMemberCount(0);
            dashboard.setEffectRate(BigDecimal.ZERO);
        }

        // 读取现金压力等级
        String pressureLevel = null;
        Boolean pressureFallbackUsed = false;
        try {
            pressureLevel = memberGrowthActionMapper.selectLatestCashPressureLevel(params.getDeptId());
        } catch (Exception e) {
            log.warn("读取现金压力快照失败，使用 fallback: {}", e.getMessage());
        }
        if (pressureLevel == null || pressureLevel.isEmpty()) {
            // 没有快照时使用 params.pressureLevel
            pressureLevel = params.getPressureLevel() != null ? params.getPressureLevel() : "LOW";
            pressureFallbackUsed = true;
        }
        dashboard.setPressureLevel(pressureLevel);
        dashboard.setPressureFallbackUsed(pressureFallbackUsed);

        // 确定 topSegmentType
        if ("HIGH".equals(pressureLevel) || "CRITICAL".equals(pressureLevel)) {
            dashboard.setTopSegmentType(PRESSURE_STORE_RECALL);
        } else if (candidates != null && !candidates.isEmpty()) {
            dashboard.setTopSegmentType(candidates.get(0).getSegmentType());
        } else {
            dashboard.setTopSegmentType(SLEEPING_HIGH_VALUE);
        }

        return dashboard;
    }

    @Override
    public List<GrowthActionCandidateVO> listCandidates(GrowthActionQueryParams params)
    {
        if (params == null) {
            params = new GrowthActionQueryParams();
        }
        List<GrowthActionCandidateVO> candidates = memberGrowthActionMapper.selectCandidates(params);
        return candidates != null ? candidates : new ArrayList<>();
    }

    @Override
    public int generateAction(GrowthActionGenerateParams params)
    {
        if (params == null) {
            throw new RuntimeException("生成参数不能为空");
        }

        // 构建查询参数
        GrowthActionQueryParams queryParams = new GrowthActionQueryParams();
        queryParams.setDeptId(params.getDeptId());
        queryParams.setSegmentType(params.getSegmentType());

        // 查询候选会员
        List<GrowthActionCandidateVO> candidates = memberGrowthActionMapper.selectCandidates(queryParams);
        if (candidates == null || candidates.isEmpty()) {
            // 无候选会员时返回 0，由 Controller 返回 code 200 的业务提示，不抛异常避免 500
            return 0;
        }

        // 去重：UNION ALL 4 层分层中同一会员可能同时满足多个分层条件，
        // 候选列表已按分层优先级排序，按 memberId 去重保留第一个（最高优先级分层）
        candidates = deduplicateByMemberId(candidates);

        // 限制最多 50 人
        int limit = params.getLimit() != null ? Math.min(params.getLimit(), 50) : 30;
        if (candidates.size() > limit) {
            candidates = candidates.subList(0, limit);
        }

        // 推导动作类型
        String actionType = params.getActionType();
        if (actionType == null || actionType.isEmpty()) {
            actionType = deriveActionType(params.getSegmentType(), candidates.get(0).getSegmentType());
        }

        // 推导 segmentType
        String segmentType = params.getSegmentType();
        if (segmentType == null || segmentType.isEmpty()) {
            segmentType = candidates.get(0).getSegmentType();
        }

        // 读取压力等级
        String pressureLevel = params.getPressureLevel();
        if (pressureLevel == null || pressureLevel.isEmpty()) {
            try {
                pressureLevel = memberGrowthActionMapper.selectLatestCashPressureLevel(params.getDeptId());
            } catch (Exception e) {
                pressureLevel = "LOW";
            }
        }
        if (pressureLevel == null || pressureLevel.isEmpty()) {
            pressureLevel = "LOW";
        }

        // 生成动作编号: GROWTH + yyyyMMddHHmmss + 4位随机数
        String actionNo = "GROWTH" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

        // 获取当前用户
        String createBy = getCreateBy();

        // 创建动作主记录
        MemGrowthAction action = new MemGrowthAction();
        action.setActionNo(actionNo);
        action.setDeptId(params.getDeptId());
        action.setActionType(actionType);
        action.setActionTitle(buildActionTitle(actionType, segmentType));
        action.setSourceType("MANUAL");
        action.setPressureLevel(pressureLevel);
        action.setCandidateCount(candidates.size());
        action.setExecutedCount(0);
        action.setStatus("PENDING");
        action.setActionReason(params.getActionReason());
        action.setSuggestedScript(params.getSuggestedScript());
        action.setEffectWindowDays(7);
        action.setTenantId(getTenantId());
        action.setCreateBy(createBy);

        memberGrowthActionMapper.insertAction(action);

        // 插入候选会员明细
        for (GrowthActionCandidateVO candidate : candidates) {
            MemGrowthActionMember member = new MemGrowthActionMember();
            member.setActionId(action.getActionId());
            member.setMemberId(candidate.getMemberId());
            member.setMemberNo(candidate.getMemberNo());
            member.setMemberName(candidate.getMemberName());
            member.setDeptId(candidate.getDeptId());
            member.setSegmentType(candidate.getSegmentType());
            member.setGrowthValue(candidate.getGrowthValue());
            member.setCardType(candidate.getCardType());
            member.setLastActiveTime(candidate.getLastActiveTime());
            member.setCandidateReason(candidate.getCandidateReason());
            member.setTenantId(getTenantId());
            member.setCreateBy(createBy);
            memberGrowthActionMapper.insertActionMember(member);
        }

        return candidates.size();
    }

    @Override
    public int executeAction(GrowthActionExecuteParams params)
    {
        if (params == null) {
            throw new RuntimeException("执行参数不能为空");
        }
        if (params.getActionId() == null || params.getMemberId() == null) {
            throw new RuntimeException("动作ID和会员ID不能为空");
        }

        String status = params.getExecuteStatus();
        if (!"IN_PROGRESS".equals(status) && !"DONE".equals(status) && !"IGNORED".equals(status)) {
            throw new RuntimeException("执行状态只允许 IN_PROGRESS/DONE/IGNORED");
        }

        // DONE/IGNORED 必须填写 executeNote
        if (("DONE".equals(status) || "IGNORED".equals(status))
                && (params.getExecuteNote() == null || params.getExecuteNote().trim().isEmpty())) {
            throw new RuntimeException("DONE/IGNORED 状态必须填写执行备注");
        }

        // 更新会员执行状态：仅当明细属于该动作且状态仍为 PENDING/IN_PROGRESS 时才更新
        int affected = memberGrowthActionMapper.updateActionMemberExecute(params);
        if (affected == 0) {
            // 0 行更新说明：会员不属于该动作，或该明细已 DONE/IGNORED 不能重复执行
            throw new RuntimeException("执行失败：该会员不属于此动作，或已处于终态（DONE/IGNORED）不可重复执行");
        }

        // 按当前 actionId 重新计算 executed_count 和 status（不再使用全局 effect summary）
        int executedCount = safeCount(memberGrowthActionMapper.countExecutedByActionId(params.getActionId()));
        int totalCount = safeCount(memberGrowthActionMapper.countTotalByActionId(params.getActionId()));

        // 动作状态：全部已处理（executedCount == totalCount）则 DONE，否则 IN_PROGRESS
        String actionStatus = (totalCount > 0 && executedCount >= totalCount) ? "DONE" : "IN_PROGRESS";

        MemGrowthAction actionUpdate = new MemGrowthAction();
        actionUpdate.setActionId(params.getActionId());
        actionUpdate.setExecutedCount(executedCount);
        actionUpdate.setStatus(actionStatus);
        actionUpdate.setUpdateBy(getCreateBy());
        memberGrowthActionMapper.updateActionStatus(actionUpdate);

        return 1;
    }

    @Override
    public List<MemGrowthActionMember> listActionMembers(Long actionId)
    {
        if (actionId == null) {
            return new ArrayList<>();
        }
        List<MemGrowthActionMember> members = memberGrowthActionMapper.selectActionMembers(actionId);
        return members != null ? members : new ArrayList<>();
    }

    @Override
    public GrowthActionEffectVO getEffect(GrowthActionQueryParams params)
    {
        if (params == null) {
            params = new GrowthActionQueryParams();
        }
        GrowthActionEffectVO effect;
        if (params.getActionId() != null) {
            // R17-D：先回填真实效果标记位到明细表（best-effort），再按 actionId 回查真实 7 天效果数据
            try {
                memberGrowthActionMapper.updateMemberEffectFlags(params.getActionId());
            } catch (Exception e) {
                log.warn("回填效果标记位失败 actionId={}: {}", params.getActionId(), e.getMessage());
            }
            effect = memberGrowthActionMapper.selectRealEffectByActionId(params.getActionId());
        } else {
            effect = memberGrowthActionMapper.selectEffectSummary(params);
        }
        if (effect == null) {
            effect = createEmptyEffect();
        }
        // 计算 effectRate
        if (effect.getTotalMemberCount() != null && effect.getTotalMemberCount() > 0) {
            BigDecimal rate = new BigDecimal(effect.getEffectiveMemberCount())
                    .divide(new BigDecimal(effect.getTotalMemberCount()), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            effect.setEffectRate(rate);
        } else {
            effect.setEffectRate(BigDecimal.ZERO);
        }
        return effect;
    }

    @Override
    public int backfillEffectFlags()
    {
        List<Long> actionIds = memberGrowthActionMapper.selectAllActionIds();
        if (actionIds == null || actionIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long actionId : actionIds) {
            try {
                memberGrowthActionMapper.updateMemberEffectFlags(actionId);
                count++;
            } catch (Exception e) {
                log.warn("R21 回填效果标记位失败 actionId={}: {}", actionId, e.getMessage());
            }
        }
        return count;
    }

    // ==================== 私有方法 ====================

    /**
     * 按 memberId 去重：UNION ALL 4 层分层中同一会员可能同时满足多个分层条件，
     * 候选列表已按分层优先级排序，保留第一个出现的（最高优先级分层）。
     */
    private List<GrowthActionCandidateVO> deduplicateByMemberId(List<GrowthActionCandidateVO> candidates)
    {
        if (candidates == null || candidates.size() <= 1) {
            return candidates;
        }
        java.util.LinkedHashMap<Long, GrowthActionCandidateVO> seen = new java.util.LinkedHashMap<>();
        for (GrowthActionCandidateVO c : candidates) {
            if (c.getMemberId() != null && !seen.containsKey(c.getMemberId())) {
                seen.put(c.getMemberId(), c);
            }
        }
        return new ArrayList<>(seen.values());
    }

    private String deriveActionType(String paramSegmentType, String firstCandidateSegmentType)
    {
        String segment = paramSegmentType != null ? paramSegmentType : firstCandidateSegmentType;
        if (PRESSURE_STORE_RECALL.equals(segment)) {
            return ACTION_RECALL_VISIT;
        } else if (SLEEPING_HIGH_VALUE.equals(segment)) {
            return ACTION_RECALL_VISIT;
        } else if (NEAR_LEVEL_UP.equals(segment)) {
            return ACTION_LEVEL_UP_NUDGE;
        } else if (RECENT_ACTIVE_NO_REPEAT.equals(segment)) {
            return ACTION_REPEAT_PURCHASE;
        }
        return ACTION_RECALL_VISIT;
    }

    private String buildActionTitle(String actionType, String segmentType)
    {
        switch (actionType) {
            case ACTION_RECALL_VISIT:
                return "召回到店";
            case ACTION_LEVEL_UP_NUDGE:
                return "临门升级提醒";
            case ACTION_SIGN_IN_RECOVER:
                return "签到恢复";
            case ACTION_REPEAT_PURCHASE:
                return "复购提醒";
            default:
                return "增长动作";
        }
    }

    private GrowthActionEffectVO createEmptyEffect()
    {
        GrowthActionEffectVO effect = new GrowthActionEffectVO();
        effect.setTotalMemberCount(0);
        effect.setRepurchaseMemberCount(0);
        effect.setSignInMemberCount(0);
        effect.setGrowthIncreasedMemberCount(0);
        effect.setEffectiveMemberCount(0);
        effect.setEffectRate(BigDecimal.ZERO);
        return effect;
    }

    private int safeCount(Integer value)
    {
        return value != null ? value : 0;
    }

    private String getCreateBy()
    {
        try {
            LoginUser loginUser = SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
            if (loginUser != null && loginUser.getUsername() != null) {
                return loginUser.getUsername();
            }
        } catch (Exception e) {
            // ignore
        }
        return "admin";
    }

    /**
     * 获取当前登录用户的租户ID（与 TenantSqlInterceptor 注入条件保持一致）
     */
    private Long getTenantId()
    {
        try {
            LoginUser loginUser = SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
            if (loginUser != null && loginUser.getSysUser() != null
                    && loginUser.getSysUser().getTenantId() != null) {
                return loginUser.getSysUser().getTenantId();
            }
        } catch (Exception e) {
            // ignore
        }
        return 0L;
    }
}
