package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.finance.domain.vo.*;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProfitShareSettlementDashboardServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = FinanceReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser lu = new LoginUser();
        lu.setUserid(1L); lu.setUsername("admin"); lu.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser lu = new LoginUser();
        lu.setUserid(2L); lu.setUsername(username); lu.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    private static ReportQueryParams makeParams(List<Long> deptIds) {
        ReportQueryParams p = new ReportQueryParams();
        p.setDeptIds(deptIds);
        p.setStartTime(new Date());
        p.setEndTime(new Date());
        return p;
    }

    private FinanceReportServiceImpl createService(
            FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper saleMapper,
            FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper expenseMapper,
            FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper profitMapper,
            FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper periodMapper,
            RemoteUserService remoteUserService) throws Exception {
        FinanceReportServiceImpl svc = new FinanceReportServiceImpl();
        setField(svc, "finSaleRecordMapper", saleMapper);
        setField(svc, "finExpenseMapper", expenseMapper);
        setField(svc, "finProfitShareRecordMapper", profitMapper);
        setField(svc, "finAccountingPeriodMapper", periodMapper);
        setField(svc, "remoteUserService", remoteUserService);
        setField(svc, "finProfitShareDetailMapper", new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareDetailMapper());
        return svc;
    }

    // ── Tests ──

    @Test
    void getProfitShareSettlement_payablePaidPending() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        Map<String, Object> row = new HashMap<>();
        row.put("payableAmount", new BigDecimal("10000.00"));
        row.put("paidAmount", new BigDecimal("6000.00"));
        row.put("managerShare", new BigDecimal("4000.00"));
        row.put("investorShare", new BigDecimal("6000.00"));
        profitMapper.settlementRows = List.of(row);

        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ProfitShareSettlementDashboardVO vo = svc.getProfitShareSettlement(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("10000.00"), vo.getPayableAmount());
        assertEquals(new BigDecimal("6000.00"), vo.getPaidAmount());
        assertEquals(new BigDecimal("4000.00"), vo.getPendingAmount(),
                "pendingAmount = payable - paid = 10000 - 6000 = 4000");
    }

    @Test
    void getProfitShareSettlement_managerAndInvestorShareProportions() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        Map<String, Object> row = new HashMap<>();
        row.put("payableAmount", new BigDecimal("5000.00"));
        row.put("paidAmount", new BigDecimal("5000.00"));
        row.put("managerShare", new BigDecimal("2000.00"));
        row.put("investorShare", new BigDecimal("3000.00"));
        profitMapper.settlementRows = List.of(row);

        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ProfitShareSettlementDashboardVO vo = svc.getProfitShareSettlement(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("2000.00"), vo.getManagerShare());
        assertEquals(new BigDecimal("3000.00"), vo.getInvestorShare());
        // manager + investor = payable
        assertEquals(vo.getPayableAmount(), vo.getManagerShare().add(vo.getInvestorShare()));
    }

    @Test
    void getProfitShareSettlement_negativeProfitException() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        // Sales from trend = 1000
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("1000.00"));
        saleMapper.saleTrendStats = List.of(trendRow);

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        // Expense = 5000 -> netProfit = 1000 - 5000 = -4000
        expenseMapper.expenseTotal = new BigDecimal("5000.00");

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ProfitShareSettlementDashboardVO vo = svc.getProfitShareSettlement(makeParams(List.of(100L)));

        assertTrue(vo.getNetProfit().compareTo(BigDecimal.ZERO) < 0);
        assertNotNull(vo.getExceptions());
        assertTrue(vo.getExceptions().stream().anyMatch(e -> "NEGATIVE_PROFIT".equals(e.getExceptionType())),
                "Negative profit should generate NEGATIVE_PROFIT exception");
    }

    @Test
    void getProfitShareSettlement_nonAdminFiltering() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(List.of(100L, 200L));

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ReportQueryParams params = makeParams(List.of(100L, 200L, 999L));
        svc.getProfitShareSettlement(params);

        assertEquals(List.of(100L, 200L), params.getDeptIds());
    }

    @Test
    void getProfitShareSettlement_emptySettlementRows() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ProfitShareSettlementDashboardVO vo = svc.getProfitShareSettlement(makeParams(List.of(100L)));

        assertEquals(BigDecimal.ZERO, vo.getPayableAmount());
        assertEquals(BigDecimal.ZERO, vo.getPaidAmount());
        assertEquals(BigDecimal.ZERO, vo.getPendingAmount());
        assertNotNull(vo.getDeptSettlementRows());
        assertTrue(vo.getDeptSettlementRows().isEmpty());
    }

    @Test
    void getProfitShareSettlement_multipleSettlementRows() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("payableAmount", new BigDecimal("3000.00"));
        row1.put("paidAmount", new BigDecimal("1000.00"));
        row1.put("managerShare", new BigDecimal("1500.00"));
        row1.put("investorShare", new BigDecimal("1500.00"));
        Map<String, Object> row2 = new HashMap<>();
        row2.put("payableAmount", new BigDecimal("7000.00"));
        row2.put("paidAmount", new BigDecimal("4000.00"));
        row2.put("managerShare", new BigDecimal("3500.00"));
        row2.put("investorShare", new BigDecimal("3500.00"));
        profitMapper.settlementRows = List.of(row1, row2);

        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ProfitShareSettlementDashboardVO vo = svc.getProfitShareSettlement(makeParams(List.of(100L, 200L)));

        assertEquals(new BigDecimal("10000.00"), vo.getPayableAmount(),
                "payable = 3000 + 7000 = 10000");
        assertEquals(new BigDecimal("5000.00"), vo.getPaidAmount(),
                "paid = 1000 + 4000 = 5000");
        assertEquals(new BigDecimal("5000.00"), vo.getPendingAmount(),
                "pending = 10000 - 5000 = 5000");
        assertEquals(new BigDecimal("5000.00"), vo.getManagerShare(),
                "managerShare = 1500 + 3500 = 5000");
        assertEquals(new BigDecimal("5000.00"), vo.getInvestorShare(),
                "investorShare = 1500 + 3500 = 5000");
    }
}
