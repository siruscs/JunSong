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

class SalesOperationReportServiceImplTest {

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
    void getSalesOperationReport_totalSalesOrderCountAvgOrderAmount() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("10000.00"));
        saleMapper.saleTrendStats = List.of(trendRow);
        // We need countSaleRecords and sumSaleQuantity to be configurable
        // The current fake returns 0; let's create a custom fake for this test
        var customSaleMapper = new CountableSaleMapper();
        customSaleMapper.saleTrendStats = List.of(trendRow);
        customSaleMapper.saleCount = 20;
        customSaleMapper.saleQuantity = 50;

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(customSaleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        SalesOperationReportVO vo = svc.getSalesOperationReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("10000.00"), vo.getTotalSales());
        assertEquals(20, vo.getOrderCount());
        assertEquals(new BigDecimal("500.00"), vo.getAvgOrderAmount(),
                "avgOrderAmount = 10000 / 20 = 500.00");
        assertEquals(50, vo.getTotalQuantity());
        assertEquals(new BigDecimal("200.00"), vo.getAvgItemAmount(),
                "avgItemAmount = 10000 / 50 = 200.00");
    }

    @Test
    void getSalesOperationReport_memberPlusNonMemberEqualsTotal() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("8000.00"));
        saleMapper.saleTrendStats = List.of(trendRow);
        saleMapper.memberSales = new BigDecimal("5000.00");

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        SalesOperationReportVO vo = svc.getSalesOperationReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("5000.00"), vo.getMemberSales());
        assertEquals(new BigDecimal("3000.00"), vo.getNonMemberSales(),
                "nonMemberSales = totalSales - memberSales = 8000 - 5000 = 3000");
        assertEquals(vo.getTotalSales(), vo.getMemberSales().add(vo.getNonMemberSales()),
                "memberSales + nonMemberSales should equal totalSales");
    }

    @Test
    void getSalesOperationReport_storeRankAndProductRank() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();

        Map<String, Object> deptRow = new HashMap<>();
        deptRow.put("deptId", 100L);
        deptRow.put("deptName", "Store A");
        deptRow.put("totalSales", new BigDecimal("3000.00"));
        deptRow.put("orderCount", 10);
        saleMapper.salesByDept = List.of(deptRow);

        Map<String, Object> prodRow = new HashMap<>();
        prodRow.put("productId", 1L);
        prodRow.put("productName", "Widget");
        prodRow.put("totalSales", new BigDecimal("1500.00"));
        prodRow.put("totalQuantity", 30);
        prodRow.put("deptId", 100L);
        prodRow.put("deptName", "Store A");
        saleMapper.productRank = List.of(prodRow);

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        SalesOperationReportVO vo = svc.getSalesOperationReport(makeParams(List.of(100L)));

        assertNotNull(vo.getStoreRank());
        assertEquals(1, vo.getStoreRank().size());
        assertEquals(100L, vo.getStoreRank().get(0).getDeptId());
        assertEquals(new BigDecimal("3000.00"), vo.getStoreRank().get(0).getAmount());

        assertNotNull(vo.getProductRank());
        assertEquals(1, vo.getProductRank().size());
        assertEquals("Widget", vo.getProductRank().get(0).getName());
    }

    @Test
    void getSalesOperationReport_nonAdminFiltering() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(List.of(100L, 200L));

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ReportQueryParams params = makeParams(List.of(100L, 200L, 999L));
        svc.getSalesOperationReport(params);

        assertEquals(List.of(100L, 200L), params.getDeptIds());
    }

    @Test
    void getSalesOperationReport_seckillAndNormalSales() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("6000.00"));
        saleMapper.saleTrendStats = List.of(trendRow);
        saleMapper.seckillSales = new BigDecimal("2000.00");

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        SalesOperationReportVO vo = svc.getSalesOperationReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("2000.00"), vo.getSeckillSales());
        assertEquals(new BigDecimal("4000.00"), vo.getNormalSales(),
                "normalSales = totalSales - seckillSales = 6000 - 2000 = 4000");
    }

    @Test
    void getSalesOperationReport_salesDropWarning() throws Exception {
        setupAdmin();
        var saleMapper = new FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper();
        // current sales from trend = 700, prev month = 1000 -> drop 30%
        Map<String, Object> trendRow = new HashMap<>();
        trendRow.put("dateStr", "2026-06-01");
        trendRow.put("deptId", 100L);
        trendRow.put("totalSales", new BigDecimal("700.00"));
        saleMapper.saleTrendStats = List.of(trendRow);
        saleMapper.prevMonthTotalSales = new BigDecimal("1000.00");

        var expenseMapper = new FinanceOperationDashboardServiceImplTest.FakeFinExpenseMapper();
        var profitMapper = new FinanceOperationDashboardServiceImplTest.FakeFinProfitShareRecordMapper();
        var periodMapper = new FinanceOperationDashboardServiceImplTest.FakeFinAccountingPeriodMapper();
        var remoteUserService = new FinanceOperationDashboardServiceImplTest.FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        SalesOperationReportVO vo = svc.getSalesOperationReport(makeParams(List.of(100L)));

        assertNotNull(vo.getWarnings());
        assertTrue(vo.getWarnings().stream().anyMatch(w -> "SALES_DROP".equals(w.getWarningType())));
    }

    // ── Custom mapper with configurable count/quantity ──

    static class CountableSaleMapper extends FinanceOperationDashboardServiceImplTest.FakeFinSaleRecordMapper {
        int saleCount = 0;
        int saleQuantity = 0;

        @Override
        public int countSaleRecords(List<Long> deptIds, Date s, Date e) {
            return saleCount;
        }

        @Override
        public int sumSaleQuantity(List<Long> deptIds, Date s, Date e) {
            return saleQuantity;
        }
    }
}
