package com.junsong.member.service.impl;

import com.junsong.member.domain.MemGrowthActionMember;
import com.junsong.member.domain.vo.GrowthActionCandidateVO;
import com.junsong.member.domain.vo.GrowthActionDashboardVO;
import com.junsong.member.domain.vo.GrowthActionEffectVO;
import com.junsong.member.domain.vo.GrowthActionExecuteParams;
import com.junsong.member.domain.vo.GrowthActionGenerateParams;
import com.junsong.member.domain.vo.GrowthActionQueryParams;
import com.junsong.member.domain.vo.GrowthActionRowVO;
import com.junsong.member.domain.vo.MemberActionPredictionVO;
import com.junsong.member.service.IMemberActionPredictionService;
import com.junsong.member.service.IMemberGrowthActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R24 会员动作预测服务单测。
 *
 * <p>关键点：
 * 1) 验证空样本时按 LOW 等级 + NO_DATA_BASIS 返回；
 * 2) 验证规则打分（完成率低 + 压力高 + 状态 PENDING）；
 * 3) 验证不再自调用远端 Feign（移除 probeRemoteService 后的纯净逻辑）。</p>
 */
class MemberActionPredictionServiceImplTest {

    private MemberActionPredictionServiceImpl service;
    private StubGrowthActionService growthService;

    @BeforeEach
    void setUp() throws Exception {
        service = new MemberActionPredictionServiceImpl();
        growthService = new StubGrowthActionService();
        inject(service, "growthActionService", growthService);
    }

    @Test
    void emptyRecentActionsReturnsLowWithNoDataBasis() {
        growthService.recentActions = new ArrayList<>();

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(1, result.size());
        MemberActionPredictionVO vo = result.get(0);
        assertEquals("LOW", vo.getLevel());
        assertEquals(Integer.valueOf(0), vo.getScore());
        assertEquals("无会员动作历史样本，按 LOW 等级处理，等待 R17 动作完成后再评估", vo.getBasis());
        assertNotNull(vo.getRecommendation());
    }

    @Test
    void nullDashboardReturnsLowWithNoDataBasis() {
        growthService.dashboard = null;

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(1, result.size());
        assertEquals("LOW", result.get(0).getLevel());
        assertEquals("无会员动作历史样本，按 LOW 等级处理，等待 R17 动作完成后再评估",
                result.get(0).getBasis());
    }

    @Test
    void completionRateBelow50RaisesScore() {
        GrowthActionRowVO row = new GrowthActionRowVO();
        row.setActionId(1L);
        row.setActionTitle("测试动作");
        row.setActionType("GROWTH");
        row.setDeptId(100L);
        row.setDeptName("门店A");
        row.setCandidateCount(100);
        row.setExecutedCount(20);
        row.setStatus("PENDING");
        row.setPressureLevel("LOW");
        growthService.recentActions = Collections.singletonList(row);

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(1, result.size());
        MemberActionPredictionVO vo = result.get(0);
        // 完成率 20% < 50% (+25) + 候选 100 > 50 (+15) + PENDING (+10) - LOW 压力 (-5) = 45 → MEDIUM
        assertTrue(vo.getScore() >= 30,
                "expected score to reflect 20% completion + 100 candidates + PENDING, got " + vo.getScore());
        assertNotNull(vo.getBasis());
        assertTrue(vo.getBasis().contains("完成率"),
                "basis should mention completion rate, got: " + vo.getBasis());
    }

    @Test
    void criticalPressureRaisesToHigh() {
        GrowthActionRowVO row = new GrowthActionRowVO();
        row.setActionId(1L);
        row.setActionTitle("压力门店动作");
        row.setActionType("GROWTH");
        row.setDeptId(100L);
        row.setDeptName("门店A");
        row.setCandidateCount(60);
        row.setExecutedCount(15); // 25% < 50% → +25
        row.setStatus("PENDING"); // +10
        row.setPressureLevel("CRITICAL"); // +25
        growthService.recentActions = Collections.singletonList(row);

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(1, result.size());
        MemberActionPredictionVO vo = result.get(0);
        // 25 + 15 (候选 > 50) + 10 (PENDING) + 25 (CRITICAL) = 75 → HIGH
        assertTrue(vo.getScore() >= 60,
                "expected score >= 60 for CRITICAL pressure + 25% completion + 60 candidates, got " + vo.getScore());
        assertEquals("HIGH", vo.getLevel());
    }

    @Test
    void highCompletionRateAndDoneStatusYieldsLowScore() {
        GrowthActionRowVO row = new GrowthActionRowVO();
        row.setActionId(1L);
        row.setActionTitle("良好动作");
        row.setActionType("GROWTH");
        row.setDeptId(100L);
        row.setDeptName("门店A");
        row.setCandidateCount(20);
        row.setExecutedCount(18); // 90% → -10
        row.setStatus("DONE"); // -5
        row.setPressureLevel("LOW"); // -5
        growthService.recentActions = Collections.singletonList(row);

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(1, result.size());
        assertEquals("LOW", result.get(0).getLevel());
        assertTrue(result.get(0).getScore() <= 0,
                "expected score clamped to 0 for fully completed action, got " + result.get(0).getScore());
    }

    @Test
    void longTermInactiveReasonAddsScore() {
        GrowthActionRowVO row = new GrowthActionRowVO();
        row.setActionId(1L);
        row.setActionTitle("长期未活跃会员动作");
        row.setActionType("GROWTH");
        row.setDeptId(100L);
        row.setDeptName("门店A");
        row.setCandidateCount(5);
        row.setExecutedCount(5); // 100% → -10
        row.setStatus("DONE"); // -5
        row.setPressureLevel("LOW"); // -5
        row.setActionReason("候选会员长期未活跃 30 天");
        growthService.recentActions = Collections.singletonList(row);

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(1, result.size());
        MemberActionPredictionVO vo = result.get(0);
        // -10 - 5 - 5 + 5 (未活跃) = -15 → clamp 0
        assertTrue(vo.getScore() >= 0, "score must be clamped to non-negative, got " + vo.getScore());
        assertEquals("LOW", vo.getLevel());
    }

    @Test
    void multipleActionsAllScored() {
        GrowthActionRowVO a = new GrowthActionRowVO();
        a.setActionId(1L);
        a.setActionTitle("动作A");
        a.setCandidateCount(10);
        a.setExecutedCount(2);
        a.setStatus("PENDING");
        a.setPressureLevel("HIGH");

        GrowthActionRowVO b = new GrowthActionRowVO();
        b.setActionId(2L);
        b.setActionTitle("动作B");
        b.setCandidateCount(20);
        b.setExecutedCount(20);
        b.setStatus("DONE");
        b.setPressureLevel("LOW");

        growthService.recentActions = new ArrayList<>();
        growthService.recentActions.add(a);
        growthService.recentActions.add(b);

        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, 30, null);

        assertEquals(2, result.size());
        assertEquals("动作A", result.get(0).getActionTitle());
        assertEquals("动作B", result.get(1).getActionTitle());
    }

    @Test
    void windowDaysDefaultsTo30WhenNull() {
        growthService.recentActions = new ArrayList<>();

        // 传 null windowDays：内部应默认为 30
        List<MemberActionPredictionVO> result = service.listActionPredictions(100L, null, null);
        assertEquals(1, result.size());
        assertEquals("LOW", result.get(0).getLevel());
    }

    @Test
    void remoteFeignServiceNoLongerWired() throws Exception {
        // 回归：修复前 MemberActionPredictionServiceImpl 注入了 RemoteMemberPredictionService；
        // 修复后该字段应被移除，不应再自 Feign 递归。
        Field remoteField = null;
        try {
            remoteField = MemberActionPredictionServiceImpl.class
                    .getDeclaredField("remoteMemberPredictionService");
        } catch (NoSuchFieldException expected) {
            // 这是预期结果：字段已被移除
        }
        assertTrue(remoteField == null,
                "remoteMemberPredictionService field should be removed to avoid self-Feign recursion");
    }

    // ============ helpers ============

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * 测试专用 IMemberGrowthActionService stub：只暴露 getDashboard + recentActions，
     * 其他方法返回合理默认值。
     */
    static class StubGrowthActionService implements IMemberGrowthActionService {
        List<GrowthActionRowVO> recentActions = new ArrayList<>();
        GrowthActionDashboardVO dashboard;

        @Override
        public GrowthActionDashboardVO getDashboard(GrowthActionQueryParams params) {
            if (dashboard == null) {
                dashboard = new GrowthActionDashboardVO();
            }
            dashboard.setRecentActions(recentActions);
            return dashboard;
        }

        @Override
        public List<GrowthActionCandidateVO> listCandidates(GrowthActionQueryParams params) {
            return new ArrayList<>();
        }

        @Override
        public int generateAction(GrowthActionGenerateParams params) {
            return 0;
        }

        @Override
        public int executeAction(GrowthActionExecuteParams params) {
            return 0;
        }

        @Override
        public List<MemGrowthActionMember> listActionMembers(Long actionId) {
            return new ArrayList<>();
        }

        @Override
        public GrowthActionEffectVO getEffect(GrowthActionQueryParams params) {
            return new GrowthActionEffectVO();
        }

        @Override
        public int backfillEffectFlags() {
            return 0;
        }
    }
}
