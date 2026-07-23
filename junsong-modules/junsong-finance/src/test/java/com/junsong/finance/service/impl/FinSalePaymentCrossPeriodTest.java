package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.constant.PaymentStatus;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinSalePayment;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.mapper.FinSalePaymentMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售单跨周期补缴款改造单元测试。
 *
 * 业务口径校验：
 * - 销售业务归属"原销售周期"；缴款归属"实际缴款周期"（当前进行中周期）。
 * - 原销售周期已结转后，仍可对未缴清销售单继续新增缴款。
 * - 新增缴款的 payment.periodId = 当前进行中周期；销售单 period_id 保持原周期不变。
 * - 销售单 paid_amount/status 随缴款正确更新（不因原周期已结转而被拦截）。
 * - 修改/删除"当前周期"缴款成功；修改/删除"已结转周期"缴款失败。
 *
 * 使用手写 fake（无 Mockito）。
 */
class FinSalePaymentCrossPeriodTest {

    private static final Long DEPT = 1L;
    private static final Long OLD_PERIOD = 100L;   // 已结转的原销售周期
    private static final Long CUR_PERIOD = 200L;   // 当前进行中周期

    private FakeSaleMapper saleMapper;
    private FakePaymentMapper paymentMapper;
    private FakePeriodService periodService;
    private FinSaleRecordServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        saleMapper = new FakeSaleMapper();
        paymentMapper = new FakePaymentMapper();
        periodService = new FakePeriodService();

        service = new FinSaleRecordServiceImpl();
        inject("finSaleRecordMapper", saleMapper);
        inject("finSalePaymentMapper", paymentMapper);
        inject("finAccountingPeriodService", periodService);

        // 周期：原周期已结转、当前周期进行中
        periodService.periods.put(OLD_PERIOD, period(OLD_PERIOD, PeriodStatus.CARRIED));
        periodService.periods.put(CUR_PERIOD, period(CUR_PERIOD, PeriodStatus.ACTIVE));
        periodService.currentByDept.put(DEPT, periodService.periods.get(CUR_PERIOD));
    }

    private void inject(String name, Object value) throws Exception {
        Field f = FinSaleRecordServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private FinAccountingPeriod period(Long id, String status) {
        FinAccountingPeriod p = new FinAccountingPeriod();
        p.setPeriodId(id);
        p.setDeptId(DEPT);
        p.setPeriodNo("P" + id);
        p.setStatus(status);
        return p;
    }

    private FinSaleRecord seedSale(Long saleId, BigDecimal saleAmount, BigDecimal paid) {
        FinSaleRecord s = new FinSaleRecord();
        s.setSaleId(saleId);
        s.setDeptId(DEPT);
        s.setPeriodId(OLD_PERIOD);   // 归属原销售周期
        s.setSaleNo("SO-" + saleId);
        s.setSaleAmount(saleAmount);
        s.setPaidAmount(paid);
        s.setStatus(PaymentStatus.PARTIAL);
        saleMapper.sales.put(saleId, s);
        // 已缴金额对应的历史缴款记录（归属原周期），用于累计口径统计
        if (paid != null && paid.compareTo(BigDecimal.ZERO) > 0) {
            FinSalePayment prev = new FinSalePayment();
            prev.setPaymentId(paymentMapper.seq++);
            prev.setSaleId(saleId);
            prev.setPeriodId(OLD_PERIOD);
            prev.setPaymentAmount(paid);
            paymentMapper.payments.put(prev.getPaymentId(), prev);
        }
        return s;
    }

    @Test
    void addPayment_onCarriedSalePeriod_succeeds_andBindsCurrentPeriod() {
        seedSale(5001L, new BigDecimal("100.00"), new BigDecimal("30.00"));

        int rows = service.addPayment(5001L, new BigDecimal("20.00"), "wechat", "补缴", new Date());

        assertEquals(1, rows);
        assertEquals(1, paymentMapper.inserted.size());
        FinSalePayment p = paymentMapper.inserted.get(0);
        assertEquals(CUR_PERIOD, p.getPeriodId(), "缴款必须绑定当前进行中周期");
        assertEquals(5001L, p.getSaleId());
    }

    @Test
    void addPayment_keepsSaleOriginalPeriod_unchanged() {
        seedSale(5002L, new BigDecimal("100.00"), new BigDecimal("30.00"));

        service.addPayment(5002L, new BigDecimal("20.00"), "cash", null, new Date());

        assertEquals(OLD_PERIOD, saleMapper.sales.get(5002L).getPeriodId(), "销售单 period_id 必须保持原周期");
    }

    @Test
    void addPayment_updatesPaidAmountAndStatus_partial() {
        seedSale(5003L, new BigDecimal("100.00"), new BigDecimal("30.00"));

        service.addPayment(5003L, new BigDecimal("20.00"), "cash", null, new Date());

        // 累计已缴 30 + 20 = 50 < 100 → 部分缴款
        assertEquals(0, new BigDecimal("50.00").compareTo(saleMapper.paidUpdates.get(5003L)));
        assertEquals(PaymentStatus.PARTIAL, saleMapper.statusUpdates.get(5003L));
    }

    @Test
    void addPayment_fullyPaid_marksPaidOff() {
        seedSale(5004L, new BigDecimal("100.00"), new BigDecimal("30.00"));

        service.addPayment(5004L, new BigDecimal("70.00"), "cash", null, new Date());

        assertEquals(0, new BigDecimal("100.00").compareTo(saleMapper.paidUpdates.get(5004L)));
        assertEquals(PaymentStatus.PAID, saleMapper.statusUpdates.get(5004L));
    }

    @Test
    void refreshSalePaymentState_whenSaleAmountIncreases_marksReceivable() {
        seedSale(5011L, new BigDecimal("800.00"), new BigDecimal("800.00"));
        saleMapper.sales.get(5011L).setStatus(PaymentStatus.PAID);
        saleMapper.sales.get(5011L).setSaleAmount(new BigDecimal("900.00"));

        service.refreshSalePaymentState(5011L);

        assertEquals(new BigDecimal("800.00"), saleMapper.paidUpdates.get(5011L));
        assertEquals(PaymentStatus.PARTIAL, saleMapper.statusUpdates.get(5011L));
    }

    @Test
    void refreshSalePaymentState_whenSaleAmountDecreases_keepsPaidStatusAndAllowsNegativeAdjustment() {
        seedSale(5012L, new BigDecimal("800.00"), new BigDecimal("800.00"));
        saleMapper.sales.get(5012L).setStatus(PaymentStatus.PAID);
        saleMapper.sales.get(5012L).setSaleAmount(new BigDecimal("700.00"));

        service.refreshSalePaymentState(5012L);
        service.addPayment(5012L, new BigDecimal("-100.00"), "cash", "销售金额调整", new Date());

        assertEquals(PaymentStatus.PAID, saleMapper.statusUpdates.get(5012L));
        assertEquals(new BigDecimal("700.00"), saleMapper.paidUpdates.get(5012L));
    }

    @Test
    void updatePayment_onCurrentPeriod_succeeds() {
        seedSale(5005L, new BigDecimal("100.00"), new BigDecimal("50.00"));
        FinSalePayment p = new FinSalePayment();
        p.setPaymentId(9001L);
        p.setSaleId(5005L);
        p.setPeriodId(CUR_PERIOD);   // 缴款属于当前周期
        paymentMapper.payments.put(9001L, p);

        int rows = service.updatePayment(9001L, new BigDecimal("60.00"), "cash", "改", new Date());

        assertEquals(1, rows);
        assertEquals(0, new BigDecimal("60.00").compareTo(paymentMapper.payments.get(9001L).getPaymentAmount()));
    }

    @Test
    void updatePayment_recalculatesSalePaidAmountAndStatus() {
        seedSale(5013L, new BigDecimal("900.00"), BigDecimal.ZERO);
        FinSalePayment p = new FinSalePayment();
        p.setPaymentId(9005L);
        p.setSaleId(5013L);
        p.setPeriodId(CUR_PERIOD);
        p.setPaymentAmount(new BigDecimal("800.00"));
        paymentMapper.payments.put(9005L, p);

        service.updatePayment(9005L, new BigDecimal("900.00"), "cash", "补足", new Date());

        assertEquals(new BigDecimal("900.00"), saleMapper.paidUpdates.get(5013L));
        assertEquals(PaymentStatus.PAID, saleMapper.statusUpdates.get(5013L));
    }

    @Test
    void deletePayment_onCurrentPeriod_succeeds() {
        seedSale(5006L, new BigDecimal("100.00"), new BigDecimal("50.00"));
        FinSalePayment p = new FinSalePayment();
        p.setPaymentId(9002L);
        p.setSaleId(5006L);
        p.setPeriodId(CUR_PERIOD);
        paymentMapper.payments.put(9002L, p);

        int rows = service.deletePayment(9002L);

        assertEquals(1, rows);
        assertFalse(paymentMapper.payments.containsKey(9002L));
    }

    @Test
    void updatePayment_onCarriedPeriod_fails() {
        FinSalePayment p = new FinSalePayment();
        p.setPaymentId(9003L);
        p.setSaleId(5007L);
        p.setPeriodId(OLD_PERIOD);   // 缴款属于已结转周期
        paymentMapper.payments.put(9003L, p);

        assertThrows(ServiceException.class,
                () -> service.updatePayment(9003L, new BigDecimal("10.00"), "cash", null, new Date()),
                "已结转周期的缴款记录不可修改");
    }

    @Test
    void deletePayment_onCarriedPeriod_fails() {
        FinSalePayment p = new FinSalePayment();
        p.setPaymentId(9004L);
        p.setSaleId(5008L);
        p.setPeriodId(OLD_PERIOD);
        paymentMapper.payments.put(9004L, p);

        assertThrows(ServiceException.class,
                () -> service.deletePayment(9004L),
                "已结转周期的缴款记录不可删除");
    }

    @Test
    void addPayment_usesLockedSaleReadBeforeOverpaymentCheck() {
        seedSale(5010L, new BigDecimal("100.00"), new BigDecimal("30.00"));

        service.addPayment(5010L, new BigDecimal("20.00"), "wechat", "补缴", new Date());

        assertTrue(saleMapper.lockedReadCalled, "addPayment must read sale row FOR UPDATE before checking unpaid amount");
    }

    // ==================== Fakes ====================

    static class FakePeriodService implements IFinAccountingPeriodService {
        final Map<Long, FinAccountingPeriod> periods = new HashMap<>();
        final Map<Long, FinAccountingPeriod> currentByDept = new HashMap<>();

        @Override
        public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId) {
            return periods.get(periodId);
        }

        @Override
        public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) {
            return currentByDept.get(deptId);
        }

        @Override
        public FinAccountingPeriod initCurrentPeriod(Long deptId) {
            return currentByDept.get(deptId);
        }

        @Override
        public void assertPeriodEditable(Long periodId) {
            if (periodId == null) {
                return;
            }
            FinAccountingPeriod p = periods.get(periodId);
            if (p != null && !PeriodStatus.ACTIVE.equals(p.getStatus())) {
                throw new ServiceException("会计期间已锁定，不能修改历史流水");
            }
        }

        @Override public FinAccountingPeriod trialBreakEven(Long deptId) { return null; }
        @Override public FinAccountingPeriod carryForward(Long deptId) { return null; }
        @Override public FinAccountingPeriod rollbackCarryForward(Long deptId) { return null; }
        @Override public FinAccountingPeriod rollbackCarryForward(Long deptId, String reason) { return null; }
        @Override public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod finAccountingPeriod) { return new ArrayList<>(); }
        @Override public int insertFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod) { return 0; }
        @Override public int updateFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds) { return 0; }
        @Override public FinAccountingPeriod opsAdjustStartTime(Long periodId, Date startTime, Date endTime, String reason) { return null; }
    }

    static class FakeSaleMapper implements FinSaleRecordMapper {
        final Map<Long, FinSaleRecord> sales = new HashMap<>();
        final Map<Long, BigDecimal> paidUpdates = new HashMap<>();
        final Map<Long, String> statusUpdates = new HashMap<>();

        @Override
        public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId) {
            return sales.get(saleId);
        }

        boolean lockedReadCalled = false;

        @Override
        public FinSaleRecord selectFinSaleRecordBySaleIdForUpdate(Long saleId) {
            lockedReadCalled = true;
            return selectFinSaleRecordBySaleId(saleId);
        }

        @Override
        public int updatePaidAmountAndStatus(Long saleId, BigDecimal paidAmount, String status) {
            paidUpdates.put(saleId, paidAmount);
            statusUpdates.put(saleId, status);
            FinSaleRecord s = sales.get(saleId);
            if (s != null) {
                s.setPaidAmount(paidAmount);
                s.setStatus(status);
            }
            return 1;
        }

        @Override
        public int updateFinSaleRecord(FinSaleRecord finSaleRecord) {
            sales.put(finSaleRecord.getSaleId(), finSaleRecord);
            return 1;
        }

        @Override public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord finSaleRecord) { return new ArrayList<>(); }
        @Override public List<FinSaleRecord> selectReceivableList(FinSaleRecord finSaleRecord) { return new ArrayList<>(); }
        @Override public int countReceivableByPeriodId(Long deptId, Long periodId) { return 0; }
        @Override public BigDecimal sumReceivableByPeriodId(Long deptId, Long periodId) { return BigDecimal.ZERO; }
        @Override public int insertFinSaleRecord(FinSaleRecord finSaleRecord) { return 0; }
        @Override public int deleteFinSaleRecordBySaleId(Long saleId) { return 0; }
        @Override public int deleteFinSaleRecordBySaleIds(Long[] saleIds) { return 0; }
        @Override public List<Map<String, Object>> selectSaleTrendStats(List<Long> deptIds, Date startTime, Date endTime) { return new ArrayList<>(); }
        @Override public int countSaleRecords(List<Long> deptIds, Date startTime, Date endTime) { return 0; }
        @Override public int sumSaleQuantity(List<Long> deptIds, Date startTime, Date endTime) { return 0; }
        @Override public FinSaleRecord checkSaleNoUnique(String saleNo) { return null; }
        @Override public int countTodaySales() { return 0; }
        @Override public int maxTodaySaleSeq() { return 0; }
        @Override public BigDecimal selectTodayTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTodayTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectSalesByDept(List<Long> deptIds, Date startTime, Date endTime) { return new ArrayList<>(); }
        @Override public List<Map<String, Object>> selectProductSalesRank(List<Long> deptIds, Date startTime, Date endTime) { return new ArrayList<>(); }
        @Override public BigDecimal selectMemberSales(List<Long> deptIds, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectSeckillSales(List<Long> deptIds, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectCurrentPeriodPaymentTotal(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectHistoricalReceivableCollected(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectCurrentPeriodNewReceivable(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectEndingReceivableBalance(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public int countOverdueReceivable(List<Long> deptIds) { return 0; }
    }

    static class FakePaymentMapper implements FinSalePaymentMapper {
        final Map<Long, FinSalePayment> payments = new HashMap<>();
        final List<FinSalePayment> inserted = new ArrayList<>();
        long seq = 1;

        @Override
        public FinSalePayment selectFinSalePaymentByPaymentId(Long paymentId) {
            return payments.get(paymentId);
        }

        @Override
        public int insertFinSalePayment(FinSalePayment finSalePayment) {
            finSalePayment.setPaymentId(seq++);
            inserted.add(finSalePayment);
            payments.put(finSalePayment.getPaymentId(), finSalePayment);
            return 1;
        }

        @Override
        public int updateFinSalePayment(FinSalePayment finSalePayment) {
            payments.put(finSalePayment.getPaymentId(), finSalePayment);
            return 1;
        }

        @Override
        public int deleteFinSalePaymentByPaymentId(Long paymentId) {
            return payments.remove(paymentId) != null ? 1 : 0;
        }

        @Override
        public int countTodayPayments() {
            return inserted.size();
        }

        @Override
        public BigDecimal sumPaymentAmountBySaleId(Long saleId) {
            BigDecimal sum = BigDecimal.ZERO;
            for (FinSalePayment p : payments.values()) {
                if (saleId.equals(p.getSaleId()) && p.getPaymentAmount() != null) {
                    sum = sum.add(p.getPaymentAmount());
                }
            }
            return sum;
        }

        @Override public List<FinSalePayment> selectFinSalePaymentBySaleId(Long saleId) { return new ArrayList<>(); }
        @Override public List<FinSalePayment> selectFinSalePaymentList(FinSalePayment finSalePayment) { return new ArrayList<>(); }
        @Override public int batchFinSalePayment(List<FinSalePayment> list) { return 0; }
        @Override public int deleteFinSalePaymentByPaymentIds(Long[] paymentIds) { return 0; }
        @Override public int deleteFinSalePaymentBySaleId(Long saleId) { return 0; }
        @Override public int deleteFinSalePaymentBySaleIds(Long[] saleIds) { return 0; }
        @Override public FinSalePayment checkPaymentNoUnique(String paymentNo) { return null; }
    }
}
