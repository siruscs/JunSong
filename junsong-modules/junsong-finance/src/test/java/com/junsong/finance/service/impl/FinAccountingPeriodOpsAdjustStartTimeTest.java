package com.junsong.finance.service.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.system.api.model.LoginUser;

/**
 * 运维调整历史核算周期起始时间测试
 */
class FinAccountingPeriodOpsAdjustStartTimeTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.remove();
    }

    private static void setupAdmin()
    {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser lu = new LoginUser();
        lu.setUserid(1L);
        lu.setUsername("admin");
        lu.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = null;
        Class<?> clazz = target.getClass();
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    // ===== Fake Mappers =====

    static class FakePeriodMapper implements FinAccountingPeriodMapper
    {
        public FinAccountingPeriod selectPeriodForUpdate(Long id, Long tenantId, Long deptId) { return selectFinAccountingPeriodByPeriodId(id); }
        Map<Long, FinAccountingPeriod> periods = new HashMap<>();
        Long updatedPeriodId;
        Date updatedStartTime;
        String updatedBy;
        String updatedRemark;
        FinAccountingPeriod prevPeriod;
        FinAccountingPeriod nextPeriod;

        @Override
        public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId) {
            return periods.get(periodId);
        }

        @Override
        public int updateStartTimeOnly(Long periodId, Date startTime, Date endTime, String updateBy, String remark) {
            this.updatedPeriodId = periodId;
            this.updatedStartTime = startTime;
            this.updatedBy = updateBy;
            this.updatedRemark = remark;
            FinAccountingPeriod p = periods.get(periodId);
            if (p != null) {
                p.setStartTime(startTime);
            }
            return 1;
        }

        @Override
        public FinAccountingPeriod selectPreviousPeriod(Long deptId, Date startTime, Long periodId) {
            return prevPeriod;
        }

        @Override
        public FinAccountingPeriod selectNextPeriod(Long deptId, Date startTime, Long periodId) {
            return nextPeriod;
        }

        // ===== 以下方法不需要实现 =====
        @Override public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return null; }
        @Override public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return null; }
        @Override public java.util.List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod q) { return null; }
        @Override public int insertFinAccountingPeriod(FinAccountingPeriod p) { return 0; }
        @Override public int updateFinAccountingPeriod(FinAccountingPeriod p) { return 0; }
        @Override public int resetCarryForwardByPeriodId(Long periodId, String updateBy) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodId(Long periodId) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds) { return 0; }
        @Override public BigDecimal selectTotalVerifiedExpense(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalPurchase(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSalePayment(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSaleAmount(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalUnverifiedAdvance(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public String selectCurrentPeriodStatusByDeptIds(java.util.List<Long> deptIds) { return null; }
        @Override public FinAccountingPeriod selectPeriodById(Long periodId) { return null; }
    }

    static class FakeShareMapper implements FinProfitShareRecordMapper
    {
        FinProfitShareRecord shareByPeriod;
        Long updatedPeriodId;
        Date updatedShareTime;

        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByPeriodId(Long periodId) {
            return shareByPeriod;
        }

        @Override
        public int updateShareTimeByPeriodId(Long periodId, Date shareTime, String updateBy, String remark) {
            this.updatedPeriodId = periodId;
            this.updatedShareTime = shareTime;
            if (shareByPeriod != null) {
                shareByPeriod.setShareTime(shareTime);
            }
            return 1;
        }

        // ===== 以下方法不需要实现 =====
        @Override public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId) { return null; }
        @Override public java.util.List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord q) { return null; }
        @Override public int insertFinProfitShareRecord(FinProfitShareRecord r) { return 0; }
        @Override public int updateFinProfitShareRecord(FinProfitShareRecord r) { return 0; }
        @Override public int deleteFinProfitShareRecordByShareId(Long shareId) { return 0; }
        @Override public int deleteFinProfitShareRecordByShareIds(Long[] shareIds) { return 0; }
        @Override public BigDecimal selectProfitShareTotal(Map<String, Object> params) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectManagerProfitTotal(Map<String, Object> params) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectInvestorProfitTotal(Map<String, Object> params) { return BigDecimal.ZERO; }
        @Override public java.util.List<Map<String, Object>> selectManagerProfitByDept(Map<String, Object> params) { return null; }
        @Override public java.util.List<Map<String, Object>> selectInvestorProfitByDept(Map<String, Object> params) { return null; }
        @Override public java.util.List<Map<String, Object>> selectProfitShareTrend(Map<String, Object> params) { return null; }
        @Override public int countUnsettledRecords(java.util.List<Long> deptIds) { return 0; }
        @Override public int countUnsettledRecordsByPeriodId(java.util.List<Long> deptIds, Long periodId) { return 0; }
        @Override public java.util.List<Map<String, Object>> selectSettlementByDept(java.util.List<Long> deptIds, Date startTime, Date endTime) { return null; }
        @Override public BigDecimal selectPaidAmount(java.util.List<Long> deptIds, Date startTime, Date endTime) { return BigDecimal.ZERO; }
    }

    static class NoopAuditRecorder extends FinAuditTrailRecorder
    {
        @Override
        public void record(String action, String targetType, String targetId,
                           String beforeSnapshot, String afterSnapshot) {
            // 测试中不写审计
        }
    }

    private FinAccountingPeriodServiceImpl buildService(FakePeriodMapper periodMapper, FakeShareMapper shareMapper) {
        FinAccountingPeriodServiceImpl service = new FinAccountingPeriodServiceImpl();
        try {
            setField(service, "finAccountingPeriodMapper", periodMapper);
            setField(service, "finProfitShareRecordMapper", shareMapper);
            setField(service, "auditTrailRecorder", new NoopAuditRecorder());
        } catch (Exception e) {
            fail("注入失败: " + e.getMessage());
        }
        return service;
    }

    private FinAccountingPeriod buildPeriod(Long periodId, Long deptId, String status, Date startTime, Date endTime) {
        FinAccountingPeriod p = new FinAccountingPeriod();
        p.setPeriodId(periodId);
        p.setDeptId(deptId);
        p.setPeriodNo("AP" + periodId);
        p.setStartTime(startTime);
        p.setEndTime(endTime);
        p.setStatus(status);
        p.setNetProfit(new BigDecimal("1000.00"));
        p.setManagerProfitAmount(new BigDecimal("500.00"));
        p.setInvestorProfitAmount(new BigDecimal("500.00"));
        return p;
    }

    // ===== 测试用例 =====

    @Test
    void adjustActivePeriod_updatesStartTimeOnly() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date newStart = parseDate("2026-05-15 00:00:00");
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, oldStart, null));
        FakeShareMapper sm = new FakeShareMapper();
        FinAccountingPeriodServiceImpl service = buildService(pm, sm);

        FinAccountingPeriod result = service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据");

        assertNotNull(result);
        assertEquals(newStart, pm.updatedStartTime);
        assertNull(sm.updatedPeriodId); // 进行中周期无分润记录，不更新
    }

    @Test
    void adjustCarriedPeriod_updatesStartTimeAndShareTime() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date endTime = parseDate("2026-06-30 23:59:59");
        Date newStart = parseDate("2026-05-15 00:00:00");
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.CARRIED, oldStart, endTime));
        FakeShareMapper sm = new FakeShareMapper();
        FinProfitShareRecord share = new FinProfitShareRecord();
        share.setShareId(10L);
        share.setPeriodId(1L);
        share.setShareTime(parseDate("2026-07-01 10:00:00"));
        sm.shareByPeriod = share;

        FinAccountingPeriodServiceImpl service = buildService(pm, sm);
        FinAccountingPeriod result = service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据");

        assertNotNull(result);
        assertEquals(newStart, pm.updatedStartTime);
        assertEquals(endTime, sm.updatedShareTime); // 分润时间同步为周期结束时间
    }

    @Test
    void emptyReason_throwsException() {
        setupAdmin();
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, new Date(), null));
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(1L, parseDate("2026-05-01 00:00:00"), null, ""));
    }

    @Test
    void nullStartTime_throwsException() {
        setupAdmin();
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, new Date(), null));
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(1L, null, null, "补录历史费用数据"));
    }

    @Test
    void startTimeAfterEndTime_throwsException() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date endTime = parseDate("2026-06-30 23:59:59");
        Date newStart = parseDate("2026-07-15 00:00:00"); // 晚于结束时间
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.CARRIED, oldStart, endTime));
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据"));
        assertTrue(ex.getMessage().contains("早于周期结束时间"));
    }

    @Test
    void startTimeBeforePrevPeriodEndTime_allowsCrossPrevPeriod() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date newStart = parseDate("2026-04-15 00:00:00"); // 早于上一周期结束时间（倒序补录历史数据）
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, oldStart, null));
        FinAccountingPeriod prev = buildPeriod(2L, 100L, PeriodStatus.CARRIED, parseDate("2026-05-01 00:00:00"), parseDate("2026-05-31 23:59:59"));
        pm.prevPeriod = prev;
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        // 不再限制新起始时间不得早于上一周期结束时间，允许倒序补录
        assertDoesNotThrow(() -> service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据"));
    }

    @Test
    void startTimeAfterNextPeriodStartTime_throwsException() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        // 选用早于当前系统日期(2026-07-07)但晚于下一周期起始时间(2026-07-01)的日期，
        // 以确保到达"不得晚于下一周期起始时间"校验逻辑
        Date newStart = parseDate("2026-07-05 00:00:00");
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, oldStart, null));
        FinAccountingPeriod next = buildPeriod(3L, 100L, PeriodStatus.ACTIVE, parseDate("2026-07-01 00:00:00"), null);
        pm.nextPeriod = next;
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据"));
        assertTrue(ex.getMessage().contains("不得晚于下一周期起始时间"));
    }

    @Test
    void periodNotFound_throwsException() {
        setupAdmin();
        FakePeriodMapper pm = new FakePeriodMapper();
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(999L, parseDate("2026-05-01 00:00:00"), null, "补录历史费用数据"));
    }

    @Test
    void reasonTooShort_throwsException() {
        setupAdmin();
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, new Date(), null));
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(1L, parseDate("2026-05-01 00:00:00"), null, "补录"));
    }

    @Test
    void adjustDoesNotChangeAmountFields() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date endTime = parseDate("2026-06-30 23:59:59");
        Date newStart = parseDate("2026-05-15 00:00:00");
        BigDecimal originalNetProfit = new BigDecimal("1000.00");
        BigDecimal originalManagerAmount = new BigDecimal("500.00");
        BigDecimal originalInvestorAmount = new BigDecimal("500.00");

        FakePeriodMapper pm = new FakePeriodMapper();
        FinAccountingPeriod period = buildPeriod(1L, 100L, PeriodStatus.CARRIED, oldStart, endTime);
        pm.periods.put(1L, period);
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据");

        // 金额字段保持不变
        assertEquals(originalNetProfit, period.getNetProfit());
        assertEquals(originalManagerAmount, period.getManagerProfitAmount());
        assertEquals(originalInvestorAmount, period.getInvestorProfitAmount());
    }

    @Test
    void noShareRecord_doesNotFail() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date endTime = parseDate("2026-06-30 23:59:59");
        Date newStart = parseDate("2026-05-15 00:00:00");
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.CARRIED, oldStart, endTime));
        FakeShareMapper sm = new FakeShareMapper();
        sm.shareByPeriod = null; // 无分润记录

        FinAccountingPeriodServiceImpl service = buildService(pm, sm);
        FinAccountingPeriod result = service.opsAdjustStartTime(1L, newStart, null, "补录历史费用数据");

        assertNotNull(result);
        assertEquals(newStart, pm.updatedStartTime);
        assertNull(sm.updatedPeriodId); // 未调用更新分润时间
    }

    @Test
    void activePeriodNoEndTime_newTimeMustBeBeforeNow() {
        setupAdmin();
        Date oldStart = parseDate("2026-06-01 00:00:00");
        Date futureStart = new Date(System.currentTimeMillis() + 86400000L); // 明天
        FakePeriodMapper pm = new FakePeriodMapper();
        pm.periods.put(1L, buildPeriod(1L, 100L, PeriodStatus.ACTIVE, oldStart, null));
        FinAccountingPeriodServiceImpl service = buildService(pm, new FakeShareMapper());

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.opsAdjustStartTime(1L, futureStart, null, "补录历史费用数据"));
        assertTrue(ex.getMessage().contains("早于当前时间"));
    }

    private Date parseDate(String s) {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
