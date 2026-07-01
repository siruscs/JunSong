package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.mapper.*;
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

class OperatingProfitReportServiceImplTest {

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
        lu.setUserid(1L);
        lu.setUsername("admin");
        lu.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser lu = new LoginUser();
        lu.setUserid(2L);
        lu.setUsername(username);
        lu.setDeptId(deptId);
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
    void getOperatingProfitReport_netProfitCalculation() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        // totalIncome from saleTrend = 5000
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("5000.00"));
        saleMapper.saleTrendStats = List.of(trendRow);

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        expenseMapper.expenseTotal = new BigDecimal("2000.00");

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        OperatingProfitReportVO vo = svc.getOperatingProfitReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("5000.00"), vo.getTotalIncome());
        assertEquals(new BigDecimal("2000.00"), vo.getOperatingExpense());
        assertEquals(new BigDecimal("3000.00"), vo.getNetProfit(),
                "netProfit = totalIncome - operatingExpense = 5000 - 2000 = 3000");
    }

    @Test
    void getOperatingProfitReport_profitRateZeroWhenSalesZero() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        // No sales trend rows -> totalIncome = 0
        saleMapper.saleTrendStats = Collections.emptyList();

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        expenseMapper.expenseTotal = new BigDecimal("500.00");

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        OperatingProfitReportVO vo = svc.getOperatingProfitReport(makeParams(List.of(100L)));

        assertEquals(BigDecimal.ZERO, vo.getTotalIncome());
        assertEquals(BigDecimal.ZERO, vo.getProfitRate(),
                "profitRate should be 0 when totalSales = 0 (no division by zero)");
    }

    @Test
    void getOperatingProfitReport_nonAdminFiltering() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(List.of(100L, 200L));

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ReportQueryParams params = makeParams(List.of(100L, 200L, 999L));
        svc.getOperatingProfitReport(params);

        // Only authorized deptIds should remain
        assertEquals(List.of(100L, 200L), params.getDeptIds());
    }

    @Test
    void getOperatingProfitReport_profitRateCalculation() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("4000.00"));
        saleMapper.saleTrendStats = List.of(trendRow);

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        expenseMapper.expenseTotal = new BigDecimal("1000.00");

        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        OperatingProfitReportVO vo = svc.getOperatingProfitReport(makeParams(List.of(100L)));

        // netProfit = 4000 - 1000 = 3000, profitRate = 3000/4000*100 = 75.00
        assertEquals(new BigDecimal("75.00"), vo.getProfitRate());
    }

    @Test
    void getOperatingProfitReport_costNotePresent() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        OperatingProfitReportVO vo = svc.getOperatingProfitReport(makeParams(List.of(100L)));

        assertFalse(vo.isCostReliable());
        assertNotNull(vo.getCostNote());
        assertEquals(BigDecimal.ZERO, vo.getProductCost());
    }

    @Test
    void getOperatingProfitReport_storeRanksReturned() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        Map<String, Object> deptRow = new HashMap<>();
        deptRow.put("deptId", 100L);
        deptRow.put("deptName", "Store A");
        deptRow.put("totalSales", new BigDecimal("3000.00"));
        saleMapper.salesByDept = List.of(deptRow);

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        OperatingProfitReportVO vo = svc.getOperatingProfitReport(makeParams(List.of(100L)));

        assertNotNull(vo.getStoreProfitRank());
        assertEquals(1, vo.getStoreProfitRank().size());
    }
}
