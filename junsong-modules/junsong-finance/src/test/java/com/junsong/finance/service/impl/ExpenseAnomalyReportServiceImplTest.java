package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.vo.*;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseAnomalyReportServiceImplTest {

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
    void getExpenseAnomalyReport_returnsCategoryBreakdownAndStoreRank() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        expenseMapper.expenseTotal = new BigDecimal("5000.00");

        Map<String, Object> catRow = new HashMap<>();
        catRow.put("categoryName", "Office");
        catRow.put("amount", new BigDecimal("3000.00"));
        expenseMapper.categoryStats = List.of(catRow);

        Map<String, Object> deptRow = new HashMap<>();
        deptRow.put("deptId", 100L);
        deptRow.put("deptName", "Store A");
        deptRow.put("totalExpense", new BigDecimal("5000.00"));
        expenseMapper.deptStats = List.of(deptRow);

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ExpenseAnomalyReportVO vo = svc.getExpenseAnomalyReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("5000.00"), vo.getTotalExpense());
        assertNotNull(vo.getCategoryBreakdown());
        assertEquals(1, vo.getCategoryBreakdown().size());
        assertNotNull(vo.getStoreExpenseRank());
        assertEquals(1, vo.getStoreExpenseRank().size());
    }

    @Test
    void getExpenseAnomalyReport_unverifiedListReturned() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();

        Map<String, Object> unverifiedRow = new HashMap<>();
        unverifiedRow.put("label", "unverified");
        unverifiedRow.put("expenseId", 42L);
        unverifiedRow.put("expenseNo", "FY202606010001");
        unverifiedRow.put("deptId", 100L);
        unverifiedRow.put("deptName", "Store A");
        unverifiedRow.put("currentAmount", new BigDecimal("500.00"));
        expenseMapper.unverifiedList = List.of(unverifiedRow);

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ExpenseAnomalyReportVO vo = svc.getExpenseAnomalyReport(makeParams(List.of(100L)));

        assertNotNull(vo.getUnverifiedList());
        assertEquals(1, vo.getUnverifiedList().size());
        assertEquals("UNVERIFIED", vo.getUnverifiedList().get(0).getAnomalyType());
        assertEquals(42L, vo.getUnverifiedList().get(0).getExpenseId());
        assertEquals(new BigDecimal("500.00"), vo.getUnverifiedList().get(0).getCurrentAmount());
    }

    @Test
    void getExpenseAnomalyReport_nonAdminFiltering() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(List.of(100L));

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ReportQueryParams params = makeParams(List.of(100L, 999L));
        svc.getExpenseAnomalyReport(params);

        assertEquals(List.of(100L), params.getDeptIds());
    }

    @Test
    void getExpenseAnomalyReport_ocrAnomaliesReturned() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();

        Map<String, Object> ocrRow = new HashMap<>();
        ocrRow.put("anomalyType", "AMOUNT_MISMATCH");
        ocrRow.put("label", "OCR mismatch");
        ocrRow.put("expenseId", 99L);
        ocrRow.put("expenseNo", "FY202606020001");
        ocrRow.put("deptId", 100L);
        ocrRow.put("deptName", "Store A");
        ocrRow.put("currentAmount", new BigDecimal("123.45"));
        expenseMapper.ocrAnomalies = List.of(ocrRow);

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ExpenseAnomalyReportVO vo = svc.getExpenseAnomalyReport(makeParams(List.of(100L)));

        assertNotNull(vo.getOcrAnomalies());
        assertEquals(1, vo.getOcrAnomalies().size());
        assertEquals("AMOUNT_MISMATCH", vo.getOcrAnomalies().get(0).getAnomalyType());
    }

    @Test
    void getExpenseAnomalyReport_emptyUnverifiedAndOcr() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ExpenseAnomalyReportVO vo = svc.getExpenseAnomalyReport(makeParams(List.of(100L)));

        assertNotNull(vo.getUnverifiedList());
        assertTrue(vo.getUnverifiedList().isEmpty());
        assertNotNull(vo.getOcrAnomalies());
        assertTrue(vo.getOcrAnomalies().isEmpty());
        assertNotNull(vo.getCategorySpikes());
        assertNotNull(vo.getStoreSpikes());
    }
}
