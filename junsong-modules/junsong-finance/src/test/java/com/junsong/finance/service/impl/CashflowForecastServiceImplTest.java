package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.finance.domain.FinanceCashflowForecastSnapshot;
import com.junsong.finance.domain.vo.CashflowForecastDashboardVO;
import com.junsong.finance.domain.vo.CashflowForecastDeviationVO;
import com.junsong.finance.domain.vo.CashflowForecastQueryParams;
import com.junsong.finance.mapper.CashflowForecastMapper;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CashflowForecastServiceImplTest {

    private CashflowForecastServiceImpl service;
    private RecordingForecastMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        setupSecurityContext();
        service = new CashflowForecastServiceImpl();
        mapper = new RecordingForecastMapper();
        inject(service, "cashflowForecastMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void getDashboard_builds7_14_30DayWindows() {
        CashflowForecastDashboardVO dashboard = service.getDashboard(new CashflowForecastQueryParams());

        assertEquals(3, dashboard.getWindows().size());
        assertEquals(7, dashboard.getWindows().get(0).getWindowDays());
        assertEquals(14, dashboard.getWindows().get(1).getWindowDays());
        assertEquals(30, dashboard.getWindows().get(2).getWindowDays());
    }

    @Test
    void getDashboard_marksCriticalWhenOverdueAndAge30PlusAreHigh() {
        mapper.totalUnpaidAmount = new BigDecimal("10000");
        mapper.overduePromiseAmount = new BigDecimal("3000");
        mapper.age30PlusAmount = new BigDecimal("4000");
        mapper.recentCashInAmount = new BigDecimal("1000");
        mapper.recentExpenseAmount = new BigDecimal("3000");
        mapper.promisedByWindow.put(7, new BigDecimal("500"));
        mapper.promisedByWindow.put(14, new BigDecimal("800"));
        mapper.promisedByWindow.put(30, new BigDecimal("1200"));

        CashflowForecastDashboardVO dashboard = service.getDashboard(new CashflowForecastQueryParams());

        assertTrue(dashboard.getPressure().getPressureScore() >= 80);
        assertEquals("CRITICAL", dashboard.getPressure().getPressureLevel());
        assertTrue(dashboard.getPressure().getReasons().contains("逾期承诺占比偏高"));
        assertTrue(dashboard.getPressure().getReasons().contains("30天以上应收占比偏高"));
        assertTrue(dashboard.getPressure().getReasons().contains("近7天净现金流为负"));
    }

    @Test
    void createSnapshot_insertsOneRowPerForecastWindow() {
        int count = service.createSnapshot(new CashflowForecastQueryParams());

        assertEquals(3, mapper.insertedSnapshots.size());
        assertEquals(7, mapper.insertedSnapshots.get(0).getWindowDays());
        assertEquals("现金流预测来自R15承诺回款和近7天现金流", mapper.insertedSnapshots.get(0).getForecastBasis());
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

    static class RecordingForecastMapper implements CashflowForecastMapper {
        BigDecimal totalUnpaidAmount = BigDecimal.ZERO;
        BigDecimal overduePromiseAmount = BigDecimal.ZERO;
        BigDecimal age30PlusAmount = BigDecimal.ZERO;
        BigDecimal recentCashInAmount = BigDecimal.ZERO;
        BigDecimal recentExpenseAmount = BigDecimal.ZERO;
        final Map<Integer, BigDecimal> promisedByWindow = new HashMap<>();
        final Map<Integer, BigDecimal> actualByWindow = new HashMap<>();
        final List<FinanceCashflowForecastSnapshot> insertedSnapshots = new ArrayList<>();
        final List<CashflowForecastDeviationVO> deviationRows = new ArrayList<>();

        @Override
        public BigDecimal sumPromisedAmount(CashflowForecastQueryParams params, int windowDays) {
            return promisedByWindow.getOrDefault(windowDays, BigDecimal.ZERO);
        }

        @Override
        public BigDecimal sumActualPaymentAmount(CashflowForecastQueryParams params, int windowDays) {
            return actualByWindow.getOrDefault(windowDays, BigDecimal.ZERO);
        }

        @Override
        public BigDecimal sumTotalUnpaidAmount(CashflowForecastQueryParams params) {
            return totalUnpaidAmount;
        }

        @Override
        public BigDecimal sumOverduePromiseAmount(CashflowForecastQueryParams params) {
            return overduePromiseAmount;
        }

        @Override
        public BigDecimal sumAge30PlusAmount(CashflowForecastQueryParams params) {
            return age30PlusAmount;
        }

        @Override
        public BigDecimal sumRecentCashInAmount(CashflowForecastQueryParams params) {
            return recentCashInAmount;
        }

        @Override
        public BigDecimal sumRecentExpenseAmount(CashflowForecastQueryParams params) {
            return recentExpenseAmount;
        }

        @Override
        public List<CashflowForecastDeviationVO> selectRecentDeviation(CashflowForecastQueryParams params) {
            return deviationRows;
        }

        @Override
        public int insertSnapshot(FinanceCashflowForecastSnapshot snapshot) {
            insertedSnapshots.add(snapshot);
            return 1;
        }
    }
}
