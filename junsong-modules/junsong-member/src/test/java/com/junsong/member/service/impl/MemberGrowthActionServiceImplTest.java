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
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemberGrowthActionServiceImpl 单元测试
 *
 * @author junsong
 */
class MemberGrowthActionServiceImplTest {

    private MemberGrowthActionServiceImpl service;
    private RecordingMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        setupSecurityContext();
        service = new MemberGrowthActionServiceImpl();
        mapper = new RecordingMapper();
        inject(service, "memberGrowthActionMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void dashboard_returnsCandidatesAndEffectSummary() {
        // 准备候选会员数据
        mapper.candidates.add(createCandidate(1L, "SLEEPING_HIGH_VALUE", 800L));
        mapper.candidates.add(createCandidate(2L, "NEAR_LEVEL_UP", 450L));
        mapper.effectSummary = createEffect(10, 3, 2, 1, 4);

        GrowthActionDashboardVO dashboard = service.getDashboard(new GrowthActionQueryParams());

        assertNotNull(dashboard);
        assertEquals(2, dashboard.getCandidates().size());
        assertNotNull(dashboard.getEffectSummary());
        assertEquals(10, dashboard.getEffectSummary().getTotalMemberCount());
        assertEquals(4, dashboard.getEffectiveMemberCount());
        assertNotNull(dashboard.getEffectRate());
    }

    @Test
    void generateAction_createsOneActionAndDeduplicatedMembers() {
        mapper.candidates.add(createCandidate(1L, "SLEEPING_HIGH_VALUE", 800L));
        mapper.candidates.add(createCandidate(2L, "SLEEPING_HIGH_VALUE", 600L));
        mapper.candidates.add(createCandidate(1L, "SLEEPING_HIGH_VALUE", 800L)); // 重复会员

        GrowthActionGenerateParams params = new GrowthActionGenerateParams();
        params.setActionType("RECALL_VISIT");
        params.setSegmentType("SLEEPING_HIGH_VALUE");

        int count = service.generateAction(params);

        // 去重后 2 个会员（memberId=1 去重保留第一个）
        assertEquals(2, count);
        assertEquals(1, mapper.insertedActions.size());
        assertEquals(2, mapper.insertedMembers.size());
        assertNotNull(mapper.insertedActions.get(0).getActionNo());
        assertTrue(mapper.insertedActions.get(0).getActionNo().startsWith("GROWTH"));
    }

    @Test
    void executeAction_rejectsEmptyExecuteNoteWhenDoneOrIgnored() {
        GrowthActionExecuteParams params = new GrowthActionExecuteParams();
        params.setActionId(1L);
        params.setMemberId(1L);
        params.setExecuteStatus("DONE");
        params.setExecuteNote(""); // 空备注

        assertThrows(RuntimeException.class, () -> service.executeAction(params));

        params.setExecuteStatus("IGNORED");
        params.setExecuteNote(null);
        assertThrows(RuntimeException.class, () -> service.executeAction(params));

        // IN_PROGRESS 不需要备注，且 RecordingMapper 默认返回 affected=1
        params.setExecuteStatus("IN_PROGRESS");
        assertDoesNotThrow(() -> service.executeAction(params));
    }

    @Test
    void executeAction_rejectsMemberNotBelongingToAction() {
        // P0 修复验证：updateActionMemberExecute 返回 0 行（会员不属于该动作或已终态）时必须抛异常
        mapper.updateActionMemberExecuteAffected = 0;

        GrowthActionExecuteParams params = new GrowthActionExecuteParams();
        params.setActionId(6L);
        params.setMemberId(29L); // 不属于 actionId=6 的会员
        params.setExecuteStatus("IN_PROGRESS");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.executeAction(params));
        assertTrue(ex.getMessage().contains("不属于此动作") || ex.getMessage().contains("终态"),
                "错误消息应说明不属于动作或已终态，实际: " + ex.getMessage());

        // 不应调用 updateActionStatus（动作主表不应被错误更新）
        assertEquals(0, mapper.updateActionStatusCalls);
    }

    @Test
    void executeAction_updatesActionStatusByCurrentActionId() {
        // P0 修复验证：executed_count/status 应按当前 actionId 重算
        mapper.updateActionMemberExecuteAffected = 1;
        mapper.countExecutedByActionIdResult = 2;
        mapper.countTotalByActionIdResult = 3;

        GrowthActionExecuteParams params = new GrowthActionExecuteParams();
        params.setActionId(10L);
        params.setMemberId(100L);
        params.setExecuteStatus("DONE");
        params.setExecuteNote("已电话回访");

        service.executeAction(params);

        // 应按 actionId=10 查询已执行数和总数
        assertEquals(10L, mapper.countExecutedByActionIdArg);
        assertEquals(10L, mapper.countTotalByActionIdArg);
        // 应更新动作主表 status=IN_PROGRESS（2<3 未全部处理）
        assertEquals(1, mapper.updateActionStatusCalls);
        MemGrowthAction update = mapper.lastActionStatusUpdate;
        assertEquals(10L, update.getActionId());
        assertEquals(2, update.getExecutedCount());
        assertEquals("IN_PROGRESS", update.getStatus());
    }

    @Test
    void pressureHighPrioritizesPressureStoreRecall() {
        // 设置压力等级为 HIGH
        mapper.latestPressureLevel = "HIGH";
        mapper.candidates.add(createCandidate(1L, "SLEEPING_HIGH_VALUE", 800L));

        GrowthActionQueryParams params = new GrowthActionQueryParams();
        GrowthActionDashboardVO dashboard = service.getDashboard(params);

        assertEquals("HIGH", dashboard.getPressureLevel());
        assertEquals("PRESSURE_STORE_RECALL", dashboard.getTopSegmentType());
    }

    // ==================== 辅助方法 ====================

    private GrowthActionCandidateVO createCandidate(Long memberId, String segmentType, Long growthValue) {
        GrowthActionCandidateVO candidate = new GrowthActionCandidateVO();
        candidate.setMemberId(memberId);
        candidate.setMemberNo("M" + memberId);
        candidate.setMemberName("会员" + memberId);
        candidate.setSegmentType(segmentType);
        candidate.setGrowthValue(growthValue);
        candidate.setCardType("formal");
        return candidate;
    }

    private GrowthActionEffectVO createEffect(int total, int repurchase, int signIn, int growthInc, int effective) {
        GrowthActionEffectVO effect = new GrowthActionEffectVO();
        effect.setTotalMemberCount(total);
        effect.setRepurchaseMemberCount(repurchase);
        effect.setSignInMemberCount(signIn);
        effect.setGrowthIncreasedMemberCount(growthInc);
        effect.setEffectiveMemberCount(effective);
        return effect;
    }

    private static void setupSecurityContext() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * 录制型 Mapper，用于测试
     */
    static class RecordingMapper implements MemberGrowthActionMapper {
        final List<GrowthActionCandidateVO> candidates = new ArrayList<>();
        final List<GrowthActionRowVO> recentActions = new ArrayList<>();
        final List<MemGrowthAction> insertedActions = new ArrayList<>();
        final List<MemGrowthActionMember> insertedMembers = new ArrayList<>();
        GrowthActionEffectVO effectSummary = null;
        String latestPressureLevel = null;
        Integer pendingActionCount = 0;
        Integer pendingMemberCount = 0;
        Integer executedMemberCount = 0;

        // P0 修复相关录制字段
        Integer updateActionMemberExecuteAffected = 1;
        int updateActionStatusCalls = 0;
        MemGrowthAction lastActionStatusUpdate = null;
        Long countExecutedByActionIdArg = null;
        Integer countExecutedByActionIdResult = 0;
        Long countTotalByActionIdArg = null;
        Integer countTotalByActionIdResult = 0;

        @Override
        public String selectLatestCashPressureLevel(Long deptId) {
            return latestPressureLevel;
        }

        @Override
        public List<GrowthActionCandidateVO> selectCandidates(GrowthActionQueryParams params) {
            return new ArrayList<>(candidates);
        }

        @Override
        public List<GrowthActionRowVO> selectRecentActions(GrowthActionQueryParams params) {
            return new ArrayList<>(recentActions);
        }

        @Override
        public Integer countPendingActions(GrowthActionQueryParams params) {
            return pendingActionCount;
        }

        @Override
        public Integer countPendingMembers(GrowthActionQueryParams params) {
            return pendingMemberCount;
        }

        @Override
        public Integer countExecutedMembers(GrowthActionQueryParams params) {
            return executedMemberCount;
        }

        @Override
        public Integer insertAction(MemGrowthAction action) {
            action.setActionId((long) (insertedActions.size() + 1));
            insertedActions.add(action);
            return 1;
        }

        @Override
        public Integer insertActionMember(MemGrowthActionMember member) {
            insertedMembers.add(member);
            return 1;
        }

        @Override
        public Integer updateActionStatus(MemGrowthAction action) {
            updateActionStatusCalls++;
            lastActionStatusUpdate = action;
            return 1;
        }

        @Override
        public Integer updateActionMemberExecute(GrowthActionExecuteParams params) {
            return updateActionMemberExecuteAffected;
        }

        @Override
        public List<MemGrowthActionMember> selectActionMembers(Long actionId) {
            return new ArrayList<>();
        }

        @Override
        public Integer countExecutedByActionId(Long actionId) {
            countExecutedByActionIdArg = actionId;
            return countExecutedByActionIdResult;
        }

        @Override
        public Integer countTotalByActionId(Long actionId) {
            countTotalByActionIdArg = actionId;
            return countTotalByActionIdResult;
        }

        @Override
        public GrowthActionEffectVO selectEffectSummary(GrowthActionQueryParams params) {
            return effectSummary;
        }

        @Override
        public GrowthActionEffectVO selectRealEffectByActionId(Long actionId) {
            return effectSummary;
        }

        @Override
        public Integer updateMemberEffectFlags(Long actionId) {
            return 1;
        }

        @Override
        public java.util.List<Long> selectAllActionIds() {
            return java.util.Collections.emptyList();
        }
    }
}
