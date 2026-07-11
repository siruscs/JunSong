package com.junsong.finance.service.impl;

import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.FinancePredictionFactor;
import com.junsong.finance.domain.FinancePredictionSample;
import com.junsong.finance.domain.FinanceWhatIfSimulation;
import com.junsong.finance.domain.vo.PredictionRiskVO;
import com.junsong.finance.domain.vo.PredictiveOpsDashboardVO;
import com.junsong.finance.domain.vo.PredictiveOpsQueryParams;
import com.junsong.finance.domain.vo.WhatIfSimulationParams;
import com.junsong.finance.domain.vo.WhatIfSimulationResultVO;
import com.junsong.finance.mapper.PredictiveOpsMapper;
import com.junsong.member.api.MemberActionPredictionQuery;
import com.junsong.member.api.RemoteMemberPredictionService;
import com.junsong.member.api.domain.MemberActionPredictionItem;
import com.junsong.system.api.RemoteOperationAlertService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PredictiveOpsServiceImplTest {

    private PredictiveOpsServiceImpl service;
    private RecordingPredictiveOpsMapper mapper;
    private StubRemoteMemberService remoteService;
    private StubRemoteUserService userService;

    @BeforeEach
    void setUp() throws Exception {
        service = new PredictiveOpsServiceImpl();
        mapper = new RecordingPredictiveOpsMapper();
        remoteService = new StubRemoteMemberService();
        userService = new StubRemoteUserService();
        inject(service, "predictiveOpsMapper", mapper);
        inject(service, "remoteMemberPredictionService", remoteService);
        // 数据权限收口需要 RemoteUserService；测试中不直接测授权门店内，
        // 所以塞一个空 stub（listActionPredictions 在 admin 路径下跳过授权）。
        inject(service, "remoteUserService", userService);
        // R25: 注入 no-op alertService，避免 raiseCriticalAlertIfNeeded 触发 NPE
        inject(service, "alertService", new NoOpAlertService());
    }

    @Test
    void cashflowRiskUsesDeviationAndPressureFactors() {
        mapper.cashflowDeviation = new BigDecimal("0.45");
        mapper.netCashflow = new BigDecimal("-2000");
        mapper.receivableRows = buildReceivableRows(true, 35, 2L, 8, 1);

        PredictiveOpsDashboardVO dashboard = service.getDashboard(new PredictiveOpsQueryParams());
        PredictionRiskVO cashflow = dashboard.getCashflow();

        assertEquals("CASHFLOW", cashflow.getPredictionType());
        assertTrue(cashflow.getScore() >= 55,
                "expected cashflow score to reflect deviation + net negative + overdue ratio, got " + cashflow.getScore());
        assertNotNull(cashflow.getBasis());
        assertTrue(cashflow.getFactors().size() >= 2,
                "cashflow should expose at least 2 explainable factors");
        assertTrue(cashflow.getFactors().stream()
                        .anyMatch(f -> f.getFactorCode().equals("CASHFLOW_DEVIATION_HIGH")),
                "should include deviation factor");
        assertTrue(cashflow.getFactors().stream()
                        .anyMatch(f -> f.getFactorCode().equals("NET_CASHFLOW_NEGATIVE")),
                "should include net cashflow factor");
    }

    @Test
    void receivableRiskRaisesLevelForOverduePromise() {
        // 50% 高风险行（年龄 > 30 + 已逾期 + 跟进空 + 历史跳票 >= 2）
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            rows.add(buildReceivableRow(true, 45, 3L, 8, 0));
        }
        rows.add(buildReceivableRow(false, 5, 0L, 1, 10));
        mapper.receivableRows = rows;

        PredictiveOpsDashboardVO dashboard = service.getDashboard(new PredictiveOpsQueryParams());
        PredictionRiskVO receivable = dashboard.getReceivable();
        assertTrue(receivable.getScore() >= 20,
                "expected receivable score >= 20 for 50% high-risk ratio, got " + receivable.getScore());
        assertNotNull(receivable.getBasis());
    }

    @Test
    void stockRiskIncludesNegativeAndSlowMovingFactors() {
        mapper.stockRows = new ArrayList<>();
        // 负库存
        mapper.stockRows.add(buildStockRow(new BigDecimal("-5"), new BigDecimal("0"), 0));
        // 高出库且低库存
        mapper.stockRows.add(buildStockRow(new BigDecimal("2"), new BigDecimal("50"), 0));
        // 滞销
        mapper.stockRows.add(buildStockRow(new BigDecimal("200"), new BigDecimal("0"), 0));
        // 快照偏差
        mapper.stockRows.add(buildStockRow(new BigDecimal("10"), new BigDecimal("5"), 1));

        PredictiveOpsDashboardVO dashboard = service.getDashboard(new PredictiveOpsQueryParams());
        PredictionRiskVO stock = dashboard.getStock();
        assertTrue(stock.getScore() >= 80,
                "expected stock score >= 80 for combined risks, got " + stock.getScore());
        assertEquals("CRITICAL", stock.getLevel());
        assertTrue(stock.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("STOCK_NEGATIVE")));
        assertTrue(stock.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("STOCK_LOW_AVAILABLE")));
        assertTrue(stock.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("STOCK_SLOW_MOVING")));
        assertTrue(stock.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("STOCK_SNAPSHOT_MISMATCH")));
    }

    @Test
    void whatIfSimulationChangesPressureWithoutMutatingBusinessState() {
        mapper.cashflowDeviation = new BigDecimal("0.10");
        mapper.netCashflow = new BigDecimal("5000");
        mapper.receivableRows = Collections.emptyList();
        mapper.stockRows = Collections.emptyList();
        remoteService.items = new ArrayList<>();

        WhatIfSimulationParams params = new WhatIfSimulationParams();
        params.setWindowDays(7);
        params.setExpectedCollectionDelta(new BigDecimal("10000"));
        params.setExpectedExpenseDelta(new BigDecimal("2000"));
        params.setCompletedCollectionActions(3);
        params.setCompletedMemberActions(5);
        params.setStockReplenishmentDelta(new BigDecimal("10"));

        WhatIfSimulationResultVO result = service.simulateWhatIf(params);

        assertNotNull(result.getSimulationId());
        assertTrue(result.getBasePressureScore() >= 0);
        assertTrue(result.getSimulatedPressureScore() >= 0);
        assertEquals("R24 what-if 是只读模拟，不修改业务表，只调整基线压力分", result.getBasis());
        assertTrue(result.getFactors().size() >= 4,
                "should expose factors for all 5 simulation inputs (collection/expense/collectionCompleted/memberAction/stockReplenishment)");
        assertTrue(result.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("COLLECTION_DELTA")));
        assertTrue(result.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("EXPENSE_DELTA")));
        assertTrue(result.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("COLLECTION_COMPLETED")));
        assertTrue(result.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("MEMBER_ACTION_COMPLETED")));
        assertTrue(result.getFactors().stream().anyMatch(f -> f.getFactorCode().equals("STOCK_REPLENISHMENT")));

        // 验证只插入了 simulation 记录，没有插入 sample 或 factor
        assertEquals(1, mapper.simulationInserts.size());
        assertEquals(0, mapper.sampleInserts.size());
        assertEquals(0, mapper.factorInserts.size());
    }

    @Test
    void emptyDataReturnsLowRiskWithBasis() {
        mapper.cashflowDeviation = BigDecimal.ZERO;
        mapper.netCashflow = BigDecimal.ZERO;
        mapper.receivableRows = Collections.emptyList();
        mapper.stockRows = Collections.emptyList();
        remoteService.items = Collections.emptyList();
        remoteService.code = 200;
        remoteService.success = true;

        PredictiveOpsDashboardVO dashboard = service.getDashboard(new PredictiveOpsQueryParams());
        assertEquals("LOW", dashboard.getCashflow().getLevel());
        assertEquals("LOW", dashboard.getReceivable().getLevel());
        assertEquals("LOW", dashboard.getMemberAction().getLevel());
        assertEquals("LOW", dashboard.getStock().getLevel());
        assertNotNull(dashboard.getCashflow().getBasis());
        assertNotNull(dashboard.getReceivable().getBasis());
        assertNotNull(dashboard.getMemberAction().getBasis());
        assertNotNull(dashboard.getStock().getBasis());
    }

    @Test
    void memberActionRiskFallsBackToLowWhenRemoteFails() {
        mapper.cashflowDeviation = BigDecimal.ZERO;
        mapper.netCashflow = BigDecimal.ZERO;
        mapper.receivableRows = Collections.emptyList();
        mapper.stockRows = Collections.emptyList();
        remoteService.success = false;
        remoteService.code = 500;
        remoteService.items = new ArrayList<>();

        PredictiveOpsDashboardVO dashboard = service.getDashboard(new PredictiveOpsQueryParams());
        PredictionRiskVO member = dashboard.getMemberAction();
        assertEquals("LOW", member.getLevel());
        assertTrue(member.getFactors().stream()
                .anyMatch(f -> f.getFactorCode().equals("MEMBER_SERVICE_UNAVAILABLE")),
                "must surface a 'service unavailable' factor when Feign fallback fires");
    }

    @Test
    void dashboardAppliesDataScopeForNonAdmin() {
        // 非 admin 路径：SecurityUtils.isAdmin() 返回 false（无 SecurityContext），
        // 应进入 loadAllowedDeptIds()。allowedDeptIds 留空 → 触发 SENTINEL (-1)
        mapper.cashflowDeviation = BigDecimal.ZERO;
        mapper.netCashflow = BigDecimal.ZERO;
        mapper.receivableRows = Collections.emptyList();
        mapper.stockRows = Collections.emptyList();
        remoteService.success = true;
        remoteService.items = Collections.emptyList();

        PredictiveOpsQueryParams params = new PredictiveOpsQueryParams();
        params.setDeptId(999L);
        params.setDeptIds(new ArrayList<>(java.util.Arrays.asList(1L, 2L, 3L)));
        service.getDashboard(params);

        // 非 admin 授权为空时：deptId 被清空（不在 allowed），deptIds 改为哨兵 [-1]
        assertEquals(null, params.getDeptId(), "非 admin 提交的非授权 deptId 应被清空");
        assertEquals(Collections.singletonList(-1L), params.getDeptIds(),
                "非 admin 授权为空时 deptIds 应被强制为哨兵 [-1]");
    }

    // ============= helpers =============
    private static Map<String, Object> buildReceivableRow(boolean overdue, int ageDays, long historyMiss, int daysSinceFollow, int id) {
        Map<String, Object> row = new HashMap<>();
        row.put("collectionId", (long) id);
        row.put("saleId", (long) id);
        row.put("deptId", 100L);
        row.put("collectionStatus", overdue ? "PROMISED" : "PENDING");
        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;
        row.put("promisedPayDate", new java.sql.Date(now - (overdue ? day : -day)));
        row.put("lastFollowTime", new java.sql.Date(now - daysSinceFollow * day));
        row.put("ageDays", ageDays);
        row.put("unpaidAmount", new BigDecimal("1000"));
        row.put("memberId", (long) (id * 10));
        row.put("historyMissCount", historyMiss);
        return row;
    }

    private static List<Map<String, Object>> buildReceivableRows(boolean overdue, int ageDays, long historyMiss, int daysSinceFollow, int id) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(buildReceivableRow(overdue, ageDays, historyMiss, daysSinceFollow, id));
        list.add(buildReceivableRow(false, 5, 0L, 1, id + 1));
        return list;
    }

    private static Map<String, Object> buildStockRow(BigDecimal quantity, BigDecimal recentOutbound, int snapshotMismatch) {
        Map<String, Object> row = new HashMap<>();
        row.put("deptId", 100L);
        row.put("productId", 1L);
        row.put("currentQuantity", quantity);
        row.put("recentOutbound", recentOutbound);
        row.put("snapshotDate", "2026-07-01");
        row.put("snapshotMismatch", snapshotMismatch);
        return row;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    static class RecordingPredictiveOpsMapper implements PredictiveOpsMapper {
        BigDecimal cashflowDeviation = BigDecimal.ZERO;
        BigDecimal netCashflow = BigDecimal.ZERO;
        List<Map<String, Object>> receivableRows = new ArrayList<>();
        List<Map<String, Object>> stockRows = new ArrayList<>();
        final List<FinancePredictionSample> sampleInserts = new ArrayList<>();
        final List<FinancePredictionFactor> factorInserts = new ArrayList<>();
        final List<FinanceWhatIfSimulation> simulationInserts = new ArrayList<>();

        @Override
        public BigDecimal selectRecentCashflowDeviation(PredictiveOpsQueryParams params) { return cashflowDeviation; }

        @Override
        public BigDecimal selectRecentNetCashflow(PredictiveOpsQueryParams params) { return netCashflow; }

        @Override
        public List<Map<String, Object>> selectReceivableRiskRows(PredictiveOpsQueryParams params) { return receivableRows; }

        @Override
        public List<Map<String, Object>> selectStockRiskRows(PredictiveOpsQueryParams params) { return stockRows; }

        @Override
        public int insertPredictionSample(FinancePredictionSample sample) { sampleInserts.add(sample); return 1; }

        @Override
        public int insertPredictionFactor(FinancePredictionFactor factor) { factorInserts.add(factor); return 1; }

        @Override
        public int insertWhatIfSimulation(FinanceWhatIfSimulation simulation) {
            simulation.setSimulationId(System.nanoTime());
            simulationInserts.add(simulation);
            return 1;
        }
    }

    static class StubRemoteMemberService implements RemoteMemberPredictionService {
        List<MemberActionPredictionItem> items = new ArrayList<>();
        boolean success = true;
        int code = R.SUCCESS;

        @Override
        public R<List<MemberActionPredictionItem>> listMemberActionPredictions(MemberActionPredictionQuery request, String source) {
            if (!success) return R.fail(code, "fallback");
            return R.ok(items);
        }
    }

    /**
     * 测试专用 RemoteUserService stub：默认返回空授权门店内列表，
     * 触发 sentinel -1 路径。允许单测设置 allowedDeptIds 模拟授权门店。
     */
    static class StubRemoteUserService implements RemoteUserService {
        List<Long> allowedDeptIds = new ArrayList<>();

        @Override
        public R<LoginUser> getUserInfo(String username, String source) {
            return R.ok(new LoginUser());
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) {
            return R.ok(true);
        }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> list = new ArrayList<>();
            for (Long deptId : allowedDeptIds) {
                SysDept dept = new SysDept();
                dept.setDeptId(deptId);
                list.add(dept);
            }
            return R.ok(list);
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) {
            return R.ok(new ArrayList<>());
        }
    }

    /** R25 no-op alertService stub，避免 Feign 依赖在单测中触发 NPE */
    static class NoOpAlertService implements RemoteOperationAlertService {
        @Override
        public R<Boolean> raiseAlert(Map<String, Object> body, String source) {
            return R.ok(true);
        }
    }
}
