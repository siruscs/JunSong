package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinCompositeAccountingPool;
import com.junsong.finance.domain.FinCompositePeriodItem;
import com.junsong.finance.domain.FinCompositePoolDept;
import com.junsong.finance.domain.FinCompositePoolInvestor;
import com.junsong.finance.domain.vo.CompositeAccountingSummaryVO;
import com.junsong.finance.domain.vo.CompositePoolOverviewVO;
import com.junsong.finance.domain.vo.CompositeTrialResultVO;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinCompositeAccountingMapper;
import com.junsong.finance.service.IFinCompositeAccountingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinCompositeAccountingServiceImpl
 * Uses hand-written fakes (no Mockito).
 */
class FinCompositeAccountingServiceImplTest
{
    private FinCompositeAccountingServiceImpl service;
    private FakeCompositeMapper compositeMapper;
    private FakePeriodMapper periodMapper;
    private FakeTransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() throws Exception
    {
        service = new FinCompositeAccountingServiceImpl();
        compositeMapper = new FakeCompositeMapper();
        periodMapper = new FakePeriodMapper();
        // 模拟独立事务:正常情况下直接执行回调;失败时抛异常(由外层 catch)
        transactionTemplate = new FakeTransactionTemplate();
        transactionTemplate.setMapper(compositeMapper);
        setField(service, "compositeMapper", compositeMapper);
        setField(service, "finAccountingPeriodMapper", periodMapper);
        setField(service, "auditTrailRecorder", new FinAuditTrailRecorder());
        setField(service, "transactionTemplate", transactionTemplate);
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ===== Test 1: 创建复合核算池默认为草稿状态 =====

    @Test
    void createPool_defaultDraftStatus()
    {
        FinCompositeAccountingPool pool = new FinCompositeAccountingPool();
        pool.setPoolName("测试池");
        pool.setTotalInvestAmount(new BigDecimal("100000.00"));

        int rows = service.createPool(pool);

        assertEquals(1, rows);
        assertEquals("4", pool.getStatus(), "新池状态应为草稿(4)");
        assertEquals(new BigDecimal("100000.00"), pool.getTotalInvestAmount());
        assertEquals(BigDecimal.ZERO, pool.getTotalReturnAmount(), "累计回本应为0");
        assertEquals(new BigDecimal("100000.00"), pool.getBreakEvenGap(), "回本缺口应等于总出资");
        assertNotNull(pool.getPoolNo(), "池编号应自动生成");
    }

    // ===== Test 2: 单店结转后自动纳入进行中的复合核算池 =====

    @Test
    void autoInclude_activePool_includesPeriod()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("100000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolIdByDept = 100L;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L));

        FinAccountingPeriod period = createCarriedPeriod(200L, 100L, new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"));
        periodMapper.periods.put(200L, period);

        service.autoIncludeAfterPeriodCarryForward(200L);

        assertEquals(1, compositeMapper.periodItems.size(), "应纳入1个周期");
        FinCompositePeriodItem item = compositeMapper.periodItems.get(0);
        assertEquals(200L, item.getPeriodId());
        assertEquals("0", item.getIncludedMode(), "纳入方式应为自动");
        assertEquals(0, new BigDecimal("40000.00").compareTo(item.getInvestorProfitAmount()), "纳入金额应取投资人可分配金额");
    }

    // ===== Test 3: 累计回本达总出资后状态变为已达回本 =====

    @Test
    void autoInclude_reachBreakEven_statusChangesToBreakEven()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("30000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolIdByDept = 100L;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L));

        FinAccountingPeriod period = createCarriedPeriod(200L, 100L, new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"));
        periodMapper.periods.put(200L, period);

        service.autoIncludeAfterPeriodCarryForward(200L);

        assertEquals("1", compositeMapper.pool.getStatus(), "累计回本达总出资后状态应为已达回本");
        assertNotNull(compositeMapper.pool.getBreakEvenTime(), "应记录达到回本时间");
    }

    // ===== Test 4: 回本后新结转周期不自动纳入 =====

    @Test
    void autoInclude_afterBreakEven_doesNotInclude()
    {
        FinCompositeAccountingPool pool = createTestPool("1", new BigDecimal("10000"), new BigDecimal("15000"));
        compositeMapper.pool = pool;
        compositeMapper.poolIdByDept = 100L;

        FinAccountingPeriod period = createCarriedPeriod(200L, 100L, new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"));
        periodMapper.periods.put(200L, period);

        service.autoIncludeAfterPeriodCarryForward(200L);

        assertEquals(0, compositeMapper.periodItems.size(), "已达回本后不应自动纳入");
    }

    // ===== Test 5: 草稿状态的池不自动纳入 =====

    @Test
    void autoInclude_draftPool_doesNotInclude()
    {
        FinCompositeAccountingPool pool = createTestPool("4", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolIdByDept = 100L;

        FinAccountingPeriod period = createCarriedPeriod(200L, 100L, new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"));
        periodMapper.periods.put(200L, period);

        service.autoIncludeAfterPeriodCarryForward(200L);

        assertEquals(0, compositeMapper.periodItems.size(), "草稿状态的池不应自动纳入");
    }

    // ===== Test 6: [P0#2] 自动纳入重算失败时回滚,不留半成品数据 =====

    @Test
    void autoInclude_recalculateFails_rollsBackAndDoesNotBlock()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("100000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolIdByDept = 100L;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L));

        FinAccountingPeriod period = createCarriedPeriod(200L, 100L, new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"));
        periodMapper.periods.put(200L, period);

        // 让 recalculatePool 内部失败:requirePool 调用 selectCompositePoolByPoolId 返回 null(模拟并发删除)
        compositeMapper.failOnSelectPool = true;

        // 不应抛异常(不阻断单店结转)
        assertDoesNotThrow(() -> service.autoIncludeAfterPeriodCarryForward(200L));

        // 独立事务应回滚,periodItems 不应留下半成品
        assertEquals(0, compositeMapper.periodItems.size(), "重算失败时应回滚,不应留下已纳入但未刷新的记录");
    }

    // ===== Test 7: [P1#3] 手动纳入跨店周期被拦截 =====

    @Test
    void trialInclude_periodFromNonPoolDept_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("2", new BigDecimal("10000"), new BigDecimal("15000"));
        compositeMapper.pool = pool;
        // 池参与店面:100, 101
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L, 101L));

        // 周期属于店面 999(不属于池)
        FinAccountingPeriod period = createCarriedPeriod(200L, 999L, new BigDecimal("6000"), new BigDecimal("1000"), new BigDecimal("5000"));
        periodMapper.periods.put(200L, period);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.trialIncludePeriods(1L, List.of(200L)));
        assertTrue(ex.getMessage().contains("不属于该复合核算池"), "应拦截跨店纳入,提示店面不属于池");
    }

    // ===== Test 8: [P1#4] 候选周期查询非池内店面被拦截 =====

    @Test
    void listCandidatePeriods_deptNotInPool_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("2", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L, 101L));

        // 查询店面 999(不属于池)
        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.listCandidatePeriods(1L, 999L));
        assertTrue(ex.getMessage().contains("不属于该复合核算池"), "应拦截跨池查询候选周期");
    }

    // ===== Test 9: [P2#7] 草稿激活:绑定店面和投资人都齐全后转为进行中 =====

    @Test
    void bindInvestors_draftPoolWithDepts_activatesToActive()
    {
        FinCompositeAccountingPool pool = createTestPool("4", new BigDecimal("0"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L, 101L));
        // 模拟已有投资人(让 tryActivateDraft 检测到)
        compositeMapper.investors = new ArrayList<>(List.of(createInvestor(1L, "投资人A", new BigDecimal("50000"))));

        IFinCompositeAccountingService.InvestorInput input = new IFinCompositeAccountingService.InvestorInput();
        input.setInvestorId(1L);
        input.setInvestorName("投资人A");
        input.setInvestAmount(new BigDecimal("50000"));

        service.bindInvestors(1L, List.of(input));

        assertEquals("0", compositeMapper.pool.getStatus(), "店面和投资人都齐全后,草稿应转为进行中(0)");
    }

    // ===== Test 9.1: 未回本池激活后补齐全部店面核算周期 =====

    @Test
    void bindInvestors_activePoolNotBreakEven_includesAllPeriodsFromPoolDepts()
    {
        FinCompositeAccountingPool pool = createTestPool("4", new BigDecimal("100000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L, 101L));

        periodMapper.periods.put(200L, createCarriedPeriod(200L, 100L, new BigDecimal("12000"), new BigDecimal("2000"), new BigDecimal("10000")));
        periodMapper.periods.put(201L, createCarriedPeriod(201L, 100L, new BigDecimal("500"), new BigDecimal("1000"), BigDecimal.ZERO));
        periodMapper.periods.put(202L, createCarriedPeriod(202L, 101L, new BigDecimal("7000"), new BigDecimal("1000"), new BigDecimal("6000")));
        FinAccountingPeriod activePeriod = createCarriedPeriod(203L, 101L, new BigDecimal("-3000"), BigDecimal.ZERO, BigDecimal.ZERO);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        periodMapper.periods.put(203L, activePeriod);

        IFinCompositeAccountingService.InvestorInput input = new IFinCompositeAccountingService.InvestorInput();
        input.setInvestorId(1L);
        input.setInvestorName("投资人A");
        input.setInvestAmount(new BigDecimal("100000"));

        service.bindInvestors(1L, List.of(input));

        assertEquals("0", compositeMapper.pool.getStatus(), "累计回本未达总出资时应保持进行中");
        assertEquals(4, compositeMapper.periodItems.size(), "未回本时应补齐池内店面的全部核算周期");
        assertTrue(compositeMapper.periodItems.stream().anyMatch(item ->
                item.getPeriodId().equals(201L) && BigDecimal.ZERO.compareTo(item.getInvestorProfitAmount()) == 0),
                "收益为0的核算周期也应纳入,保证周期口径完整");
        assertTrue(compositeMapper.periodItems.stream().anyMatch(item -> item.getPeriodId().equals(203L)),
                "进行中的核算周期也应纳入,保证未回本复合池统计完整");
        assertEquals(0, new BigDecimal("16000.00").compareTo(compositeMapper.pool.getTotalReturnAmount()), "累计回本应汇总全部纳入周期金额");
    }

    // ===== Test 10: [P2#7] 草稿状态禁止手动纳入 =====

    @Test
    void confirmInclude_draftPool_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("4", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.confirmIncludePeriods(1L, List.of(200L)));
        assertTrue(ex.getMessage().contains("草稿"), "草稿状态应禁止纳入");
    }

    // ===== Test 11: 进行中状态禁止手动纳入 =====

    @Test
    void confirmInclude_activePool_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.confirmIncludePeriods(1L, List.of(200L)));
        assertTrue(ex.getMessage().contains("进行中"), "进行中状态应禁止手动纳入");
    }

    // ===== Test 12: 已纳入复合核算的周期可被检测到(回退保护) =====

    @Test
    void isPeriodIncluded_included_returnsTrue()
    {
        FinCompositePeriodItem item = new FinCompositePeriodItem();
        item.setPeriodId(200L);
        compositeMapper.includedPeriods.add(item);

        assertTrue(service.isPeriodIncludedInComposite(200L), "已纳入周期应返回true");
        assertFalse(service.isPeriodIncludedInComposite(999L), "未纳入周期应返回false");
    }

    // ===== Test 13: [P0#2] 自动纳入失败不阻断(独立事务回滚) =====

    @Test
    void autoInclude_error_doesNotThrow()
    {
        assertDoesNotThrow(() -> service.autoIncludeAfterPeriodCarryForward(999L));
    }

    // ===== Test 14: bindDepts 至少选择2个店面 =====

    @Test
    void bindDepts_lessThan2_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("4", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.bindDepts(1L, List.of(100L)));
        assertTrue(ex.getMessage().contains("2"), "应提示至少选择2个店面");
    }

    // ===== Test 15: 确认回本状态校验 =====

    @Test
    void confirmBreakEven_notBreakEvenStatus_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.confirmBreakEven(1L));
        assertTrue(ex.getMessage().contains("已达回本"), "应提示只有已达回本状态才能确认");
    }

    // ===== Test 16: 回本后手动选择周期可正确试算 =====

    @Test
    void trialInclude_afterBreakEven_calculatesCorrectly()
    {
        FinCompositeAccountingPool pool = createTestPool("1", new BigDecimal("10000"), new BigDecimal("8000"));
        compositeMapper.pool = pool;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L));

        FinCompositePeriodItem existing = new FinCompositePeriodItem();
        existing.setPoolId(1L);
        existing.setPeriodId(99L);
        existing.setInvestorProfitAmount(new BigDecimal("8000"));
        compositeMapper.existingItems.add(existing);

        FinAccountingPeriod candidate = createCarriedPeriod(200L, 100L, new BigDecimal("6000"), new BigDecimal("1000"), new BigDecimal("5000"));
        periodMapper.periods.put(200L, candidate);

        CompositeTrialResultVO result = service.trialIncludePeriods(1L, List.of(200L));

        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getCurrentIncludeAmount()), "本次纳入金额应为5000");
        assertEquals(0, new BigDecimal("13000.00").compareTo(result.getTotalReturnAmount()), "累计回本应为8000+5000=13000");
        assertTrue(result.getBreakEvenReached(), "应已达到回本");
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.getOverReturnAmount()), "超额收益应为3000");
    }

    // ===== Test 17: 同一周期重复纳入抛异常 =====

    @Test
    void trialInclude_duplicatePeriod_throwsException()
    {
        FinCompositeAccountingPool pool = createTestPool("2", new BigDecimal("10000"), new BigDecimal("15000"));
        compositeMapper.pool = pool;
        compositeMapper.poolDeptIds = new ArrayList<>(List.of(100L));

        FinCompositePeriodItem existing = new FinCompositePeriodItem();
        existing.setPoolId(1L);
        existing.setPeriodId(200L);
        compositeMapper.existingItemsByPoolAndPeriod.add(existing);

        FinAccountingPeriod candidate = createCarriedPeriod(200L, 100L, new BigDecimal("6000"), new BigDecimal("1000"), new BigDecimal("5000"));
        periodMapper.periods.put(200L, candidate);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.trialIncludePeriods(1L, List.of(200L)));
        assertTrue(ex.getMessage().contains("已纳入"), "应抛出已纳入异常");
    }

    // ===== Test 18: 概览周期明细返回店面名称和周期起止时间 =====

    @Test
    void getOverview_periodItemsIncludeDeptNameAndPeriodTime()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("10000"), BigDecimal.ZERO);
        compositeMapper.pool = pool;
        Date startTime = new Date(1000L);
        Date endTime = new Date(2000L);

        FinCompositePeriodItem item = new FinCompositePeriodItem();
        item.setPoolId(1L);
        item.setDeptId(100L);
        item.setPeriodId(200L);
        item.setPeriodNo("AP200");
        item.setDeptName("盛和里");
        item.setPeriodStartTime(startTime);
        item.setPeriodEndTime(endTime);
        compositeMapper.periodItems.add(item);

        CompositePoolOverviewVO overview = service.getOverview(1L);

        assertEquals("盛和里", overview.getPeriodItems().get(0).getDeptName(), "周期明细应显示店面名称");
        assertEquals(startTime, overview.getPeriodItems().get(0).getPeriodStartTime(), "周期明细应显示周期开始时间");
        assertEquals(endTime, overview.getPeriodItems().get(0).getPeriodEndTime(), "周期明细应显示周期结束时间");
    }

    // ===== Test 19: 概览返回已纳入周期的复合核算汇总 =====

    @Test
    void getOverview_includedPeriodSummary()
    {
        FinCompositeAccountingPool pool = createTestPool("0", new BigDecimal("100000"), new BigDecimal("30000"));
        compositeMapper.pool = pool;
        compositeMapper.summary = new CompositeAccountingSummaryVO();
        compositeMapper.summary.setTotalVerifiedExpense(new BigDecimal("1200.50"));
        compositeMapper.summary.setTotalPurchase(new BigDecimal("5000.00"));
        compositeMapper.summary.setTotalSaleAmount(new BigDecimal("9000.00"));
        compositeMapper.summary.setTotalSalePayment(new BigDecimal("8300.00"));

        CompositePoolOverviewVO overview = service.getOverview(1L);

        assertEquals(0, new BigDecimal("1200.50").compareTo(overview.getSummary().getTotalVerifiedExpense()), "应汇总总费用");
        assertEquals(0, new BigDecimal("5000.00").compareTo(overview.getSummary().getTotalPurchase()), "应汇总总进货");
        assertEquals(0, new BigDecimal("9000.00").compareTo(overview.getSummary().getTotalSaleAmount()), "应汇总总销售");
        assertEquals(0, new BigDecimal("8300.00").compareTo(overview.getSummary().getTotalSalePayment()), "应汇总总缴款");
        assertEquals(0, new BigDecimal("70000.00").compareTo(overview.getSummary().getBreakEvenGap()), "回本差额应取复合池主表口径");
        assertEquals(0, new BigDecimal("-2099.50").compareTo(overview.getSummary().getTheoreticalBreakEvenGap()), "理论回本差额应等于总费用+总进货-总缴款");
    }

    // ===== 辅助方法 =====

    private FinCompositeAccountingPool createTestPool(String status, BigDecimal totalInvest, BigDecimal totalReturn)
    {
        FinCompositeAccountingPool pool = new FinCompositeAccountingPool();
        pool.setPoolId(1L);
        pool.setPoolNo("CP001");
        pool.setPoolName("测试池");
        pool.setStatus(status);
        pool.setTotalInvestAmount(totalInvest);
        pool.setTotalReturnAmount(totalReturn);
        pool.setBreakEvenGap(totalInvest.subtract(totalReturn).max(BigDecimal.ZERO));
        pool.setOverReturnAmount(totalReturn.subtract(totalInvest).max(BigDecimal.ZERO));
        return pool;
    }

    private FinAccountingPeriod createCarriedPeriod(Long periodId, Long deptId, BigDecimal netProfit, BigDecimal managerAmount, BigDecimal investorAmount)
    {
        FinAccountingPeriod p = new FinAccountingPeriod();
        p.setPeriodId(periodId);
        p.setDeptId(deptId);
        p.setPeriodNo("AP" + periodId);
        p.setStatus(PeriodStatus.CARRIED);
        p.setNetProfit(netProfit);
        p.setManagerProfitAmount(managerAmount);
        p.setInvestorProfitAmount(investorAmount);
        p.setCarryForwardTime(new Date());
        return p;
    }

    private FinCompositePoolInvestor createInvestor(Long id, String name, BigDecimal amount)
    {
        FinCompositePoolInvestor inv = new FinCompositePoolInvestor();
        inv.setId(id);
        inv.setInvestorId(id);
        inv.setInvestorName(name);
        inv.setInvestAmount(amount);
        inv.setInvestRatio(new BigDecimal("1.0000"));
        inv.setReturnedAmount(BigDecimal.ZERO);
        inv.setStatus("0");
        return inv;
    }

    // ===== Fake TransactionTemplate =====

    static class FakeTransactionTemplate extends TransactionTemplate
    {
        private FakeCompositeMapper mapper;

        FakeTransactionTemplate() { super(); }

        void setMapper(FakeCompositeMapper mapper) { this.mapper = mapper; }

        @Override
        public <T> T execute(TransactionCallback<T> action)
        {
            TransactionStatus status = new FakeTransactionStatus();
            // 模拟真实事务管理器:事务开启前保存数据库状态快照
            List<FinCompositePeriodItem> snapshotItems = mapper == null ? null : new ArrayList<>(mapper.periodItems);
            List<FinCompositePeriodItem> snapshotIncluded = mapper == null ? null : new ArrayList<>(mapper.includedPeriods);
            try {
                return action.doInTransaction(status);
            } catch (RuntimeException e) {
                // 模拟真实事务管理器:回滚未提交的变更后抛出
                if (mapper != null) {
                    mapper.periodItems = snapshotItems;
                    mapper.includedPeriods = snapshotIncluded;
                }
                throw e;
            }
        }
    }

    static class FakeTransactionStatus implements TransactionStatus
    {
        private boolean rollbackOnly = false;
        @Override public boolean isNewTransaction() { return true; }
        @Override public boolean hasSavepoint() { return false; }
        @Override public void setRollbackOnly() { rollbackOnly = true; }
        @Override public boolean isRollbackOnly() { return rollbackOnly; }
        @Override public Object createSavepoint() { return null; }
        @Override public void rollbackToSavepoint(Object savepoint) throws org.springframework.transaction.TransactionException {}
        @Override public void releaseSavepoint(Object savepoint) throws org.springframework.transaction.TransactionException {}
        @Override public boolean isCompleted() { return false; }
        @Override public void flush() {}
    }

    // ===== Fake Mapper =====

    static class FakeCompositeMapper implements FinCompositeAccountingMapper
    {
        FinCompositeAccountingPool pool;
        Long poolIdByDept = null;
        List<Long> poolDeptIds = new ArrayList<>();
        List<FinCompositePoolInvestor> investors = new ArrayList<>();
        List<FinCompositePeriodItem> periodItems = new ArrayList<>();
        List<FinCompositePeriodItem> existingItems = new ArrayList<>();
        List<FinCompositePeriodItem> existingItemsByPoolAndPeriod = new ArrayList<>();
        List<FinCompositePeriodItem> includedPeriods = new ArrayList<>();
        CompositeAccountingSummaryVO summary;
        int selectCount = 0;
        boolean failOnSelectPool = false;

        @Override
        public FinCompositeAccountingPool selectCompositePoolByPoolId(Long poolId) {
            selectCount++;
            if (failOnSelectPool && selectCount >= 1) {
                // 模拟并发删除:查询返回 null,触发 requirePool 抛异常
                return null;
            }
            return pool != null && pool.getPoolId().equals(poolId) ? pool : null;
        }

        @Override
        public List<FinCompositeAccountingPool> selectCompositePoolList(FinCompositeAccountingPool query) {
            List<FinCompositeAccountingPool> list = new ArrayList<>();
            if (pool != null) list.add(pool);
            return list;
        }

        @Override
        public int insertCompositePool(FinCompositeAccountingPool p) {
            this.pool = p;
            return 1;
        }

        @Override
        public int updateCompositePool(FinCompositeAccountingPool p) {
            if (pool != null && pool.getPoolId().equals(p.getPoolId())) {
                if (p.getStatus() != null) pool.setStatus(p.getStatus());
                if (p.getTotalInvestAmount() != null) pool.setTotalInvestAmount(p.getTotalInvestAmount());
                if (p.getTotalReturnAmount() != null) pool.setTotalReturnAmount(p.getTotalReturnAmount());
                if (p.getBreakEvenGap() != null) pool.setBreakEvenGap(p.getBreakEvenGap());
                if (p.getOverReturnAmount() != null) pool.setOverReturnAmount(p.getOverReturnAmount());
                if (p.getBreakEvenTime() != null) pool.setBreakEvenTime(p.getBreakEvenTime());
                if (p.getConfirmedTime() != null) pool.setConfirmedTime(p.getConfirmedTime());
                if (p.getConfirmedBy() != null) pool.setConfirmedBy(p.getConfirmedBy());
            }
            return 1;
        }

        @Override
        public int deleteCompositePoolByPoolIds(Long[] poolIds) { return 1; }

        @Override
        public FinCompositeAccountingPool selectActivePoolByDeptId(Long deptId) {
            if (poolIdByDept != null && pool != null) {
                return pool;
            }
            return null;
        }

        @Override
        public int addReturnAmount(Long poolId, BigDecimal amount) { return 1; }

        @Override
        public List<FinCompositePoolDept> selectPoolDeptsByPoolId(Long poolId) { return new ArrayList<>(); }

        @Override
        public List<Long> selectPoolDeptIdsByPoolId(Long poolId) {
            return poolDeptIds;
        }

        @Override
        public int insertPoolDept(FinCompositePoolDept dept) { return 1; }

        @Override
        public int deletePoolDeptByPoolId(Long poolId) { return 1; }

        @Override
        public int deletePoolDeptByPoolIdAndDeptId(Long poolId, Long deptId) { return 1; }

        @Override
        public List<FinCompositePoolInvestor> selectPoolInvestorsByPoolId(Long poolId) {
            return investors;
        }

        @Override
        public int insertPoolInvestor(FinCompositePoolInvestor investor) {
            investors.add(investor);
            return 1;
        }

        @Override
        public int deletePoolInvestorByPoolId(Long poolId) {
            investors.clear();
            return 1;
        }

        @Override
        public int updateInvestorReturnedAmount(Long id, BigDecimal returnedAmount) { return 1; }

        @Override
        public List<FinCompositePeriodItem> selectPeriodItemsByPoolId(Long poolId) {
            List<FinCompositePeriodItem> all = new ArrayList<>(periodItems);
            all.addAll(existingItems);
            return all;
        }

        @Override
        public CompositeAccountingSummaryVO selectSummaryByPoolId(Long poolId) {
            return summary;
        }

        @Override
        public FinCompositePeriodItem selectPeriodItemByPoolIdAndPeriodId(Long poolId, Long periodId) {
            return existingItemsByPoolAndPeriod.stream()
                    .filter(i -> i.getPeriodId().equals(periodId))
                    .findFirst().orElse(null);
        }

        @Override
        public FinCompositePeriodItem selectPeriodItemByPeriodId(Long periodId) {
            return includedPeriods.stream()
                    .filter(i -> i.getPeriodId().equals(periodId))
                    .findFirst().orElse(null);
        }

        @Override
        public int insertPeriodItem(FinCompositePeriodItem item) {
            periodItems.add(item);
            includedPeriods.add(item);
            return 1;
        }

        @Override
        public int deletePeriodItemByPoolId(Long poolId) { return 1; }

        @Override
        public int revokePeriodItem(Long poolId, Long periodId) { return 1; }

        @Override
        public List<Long> selectUserDeptIdsByUserId(Long userId) {
            return java.util.Collections.singletonList(1L);
        }

        @Override
        public String selectDeptNameById(Long deptId) {
            return "店面" + deptId;
        }
    }

    static class FakePeriodMapper implements FinAccountingPeriodMapper
    {
        public FinAccountingPeriod selectPeriodForUpdate(Long id, Long tenantId, Long deptId) { return selectFinAccountingPeriodByPeriodId(id); }
        Map<Long, FinAccountingPeriod> periods = new HashMap<>();

        @Override
        public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId) {
            return periods.get(periodId);
        }

        @Override
        public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return null; }

        @Override
        public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return null; }

        @Override
        public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod query) {
            List<FinAccountingPeriod> list = new ArrayList<>();
            for (FinAccountingPeriod p : periods.values()) {
                if (query.getDeptId() != null && !query.getDeptId().equals(p.getDeptId())) continue;
                if (query.getStatus() != null && !query.getStatus().equals(p.getStatus())) continue;
                list.add(p);
            }
            return list;
        }

        @Override
        public int insertFinAccountingPeriod(FinAccountingPeriod p) { return 1; }

        @Override
        public int updateFinAccountingPeriod(FinAccountingPeriod p) { return 1; }

        @Override
        public int resetCarryForwardByPeriodId(Long periodId, String updateBy) { return 1; }

        @Override
        public int deleteFinAccountingPeriodByPeriodId(Long periodId) { return 1; }

        @Override
        public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds) { return 1; }

        @Override
        public BigDecimal selectTotalVerifiedExpense(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalPurchase(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalSalePayment(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalSaleAmount(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalUnverifiedAdvance(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override
        public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) { return "0"; }

        @Override
        public FinAccountingPeriod selectPeriodById(Long periodId) { return periods.get(periodId); }

        @Override
        public FinAccountingPeriod selectPreviousPeriod(Long deptId, Date startTime, Long periodId) { return null; }

        @Override
        public FinAccountingPeriod selectNextPeriod(Long deptId, Date startTime, Long periodId) { return null; }

        @Override
        public int updateStartTimeOnly(Long periodId, Date startTime, Date endTime, String updateBy, String remark) { return 1; }

    @Override
    public com.junsong.finance.domain.FinAccountingPeriod selectCurrentPeriodByDeptIdForUpdate(Long tenantId, Long deptId) {
        return selectCurrentPeriodByDeptId(deptId);
    }

}
}
