package com.junsong.finance.service.impl;

import com.junsong.finance.domain.vo.CashflowDashboardVO;
import com.junsong.finance.mapper.CashflowDashboardMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CashflowDashboardServiceImpl 单元测试.
 * 无 Spring 上下文，直接 new ServiceImpl + fake mapper。
 */
class CashflowDashboardServiceImplTest {

    // ── Test 1: 销售缴款增加现金流入 ──

    @Test
    void salesPaymentIncreasesCashIn() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.summary = baseSummary();
        mapper.summary.setCashInAmount(new BigDecimal("5000.00"));

        CashflowDashboardServiceImpl service = createService(mapper);

        CashflowDashboardVO vo = service.getCashflowDashboard(
                Collections.singletonList(1L), null, null);

        assertEquals(new BigDecimal("5000.00"), vo.getCashInAmount(),
                "销售缴款 5000 应增加现金流入");
    }

    // ── Test 2: 已核销费用增加现金流出 ──

    @Test
    void verifiedExpenseIncreasesCashOut() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.summary = baseSummary();
        mapper.summary.setCashOutAmount(new BigDecimal("2000.00"));

        CashflowDashboardServiceImpl service = createService(mapper);

        CashflowDashboardVO vo = service.getCashflowDashboard(
                Collections.singletonList(1L), null, null);

        assertEquals(new BigDecimal("2000.00"), vo.getCashOutAmount(),
                "已核销费用 2000 应增加现金流出");
    }

    // ── Test 3: 待核销费用进入待结算，不计入现金流出 ──

    @Test
    void pendingExpenseEntersPendingNotCashOut() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.summary = baseSummary();
        mapper.summary.setCashInAmount(new BigDecimal("5000.00"));
        mapper.summary.setCashOutAmount(BigDecimal.ZERO);
        mapper.summary.setPendingExpenseAmount(new BigDecimal("1000.00"));
        mapper.summary.setPendingExpenseCount(5);

        CashflowDashboardServiceImpl service = createService(mapper);

        CashflowDashboardVO vo = service.getCashflowDashboard(
                Collections.singletonList(1L), null, null);

        assertEquals(new BigDecimal("1000.00"), vo.getPendingExpenseAmount(),
                "待核销费用 1000 应进入待结算金额");
        assertEquals(5, vo.getPendingExpenseCount(),
                "待核销费用数量应为 5");
        assertEquals(BigDecimal.ZERO, vo.getCashOutAmount(),
                "待核销费用不应计入现金流出");
    }

    // ── Test 4: 净现金流 = 流入 - 流出 ──

    @Test
    void netCashflowEqualsInMinusOut() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.summary = baseSummary();
        mapper.summary.setCashInAmount(new BigDecimal("5000.00"));
        mapper.summary.setCashOutAmount(new BigDecimal("2000.00"));

        CashflowDashboardServiceImpl service = createService(mapper);

        CashflowDashboardVO vo = service.getCashflowDashboard(
                Collections.singletonList(1L), null, null);

        assertEquals(new BigDecimal("3000.00"), vo.getNetCashflowAmount(),
                "净现金流 = 5000 - 2000 = 3000");
    }

    // ── Test 5: 无授权门店时返回零值 ──

    @Test
    void noAuthorizedDeptReturnsZero() throws Exception {
        FakeMapper mapper = new FakeMapper();
        // 模拟 sentinel -1L 查询结果：全部为零
        mapper.summary = baseSummary(); // 所有字段默认为零

        CashflowDashboardServiceImpl service = createService(mapper);

        // deptIds = [-1L] 是哨兵值
        CashflowDashboardVO vo = service.getCashflowDashboard(
                Collections.singletonList(-1L), null, null);

        assertEquals(BigDecimal.ZERO, vo.getCashInAmount(), "无授权时现金流入应为 0");
        assertEquals(BigDecimal.ZERO, vo.getCashOutAmount(), "无授权时现金流出应为 0");
        assertEquals(BigDecimal.ZERO, vo.getNetCashflowAmount(), "无授权时净现金流应为 0");
        assertEquals(BigDecimal.ZERO, vo.getPendingExpenseAmount(), "无授权时待核销费用应为 0");
        assertEquals(0, vo.getPendingExpenseCount(), "无授权时待核销数量应为 0");
        assertEquals(BigDecimal.ZERO, vo.getPendingAdvanceAmount(), "无授权时待核销借支应为 0");
        assertEquals(0, vo.getPendingAdvanceCount(), "无授权时待核销借支数量应为 0");
        assertEquals(BigDecimal.ZERO, vo.getPendingProfitShareAmount(), "无授权时待分润应为 0");
        assertEquals(0, vo.getPendingProfitShareCount(), "无授权时待分润数量应为 0");
    }

    // ── 辅助方法 ──

    /**
     * 创建一个所有字段为零的 summary VO
     */
    private CashflowDashboardVO baseSummary() {
        CashflowDashboardVO vo = new CashflowDashboardVO();
        vo.setCashInAmount(BigDecimal.ZERO);
        vo.setCashOutAmount(BigDecimal.ZERO);
        vo.setPendingExpenseAmount(BigDecimal.ZERO);
        vo.setPendingExpenseCount(0);
        vo.setPendingAdvanceAmount(BigDecimal.ZERO);
        vo.setPendingAdvanceCount(0);
        vo.setPendingProfitShareAmount(BigDecimal.ZERO);
        vo.setPendingProfitShareCount(0);
        return vo;
    }

    /**
     * 通过反射注入 fake mapper 创建 service
     */
    private CashflowDashboardServiceImpl createService(CashflowDashboardMapper mapper) throws Exception {
        CashflowDashboardServiceImpl service = new CashflowDashboardServiceImpl();
        setField(service, "cashflowDashboardMapper", mapper);
        return service;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(name + " not found in " + target.getClass().getName());
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Fake Mapper ──

    /**
     * 手写的 fake mapper，返回可配置的结果
     */
    static class FakeMapper implements CashflowDashboardMapper {

        CashflowDashboardVO summary = new CashflowDashboardVO();
        List<CashflowDashboardVO.CashflowTrendRowVO> trendRows = new ArrayList<>();
        List<CashflowDashboardVO.CashflowPendingItemVO> pendingItems = new ArrayList<>();
        boolean tableExists = true;

        @Override
        public CashflowDashboardVO selectCashflowSummary(List<Long> deptIds, Date startTime, Date endTime) {
            return summary;
        }

        @Override
        public List<CashflowDashboardVO.CashflowTrendRowVO> selectCashflowTrendRows(List<Long> deptIds, Date startTime, Date endTime) {
            return trendRows;
        }

        @Override
        public List<CashflowDashboardVO.CashflowPendingItemVO> selectPendingItems(List<Long> deptIds) {
            return pendingItems;
        }

        @Override
        public int checkTableExists(String tableName) {
            return tableExists ? 1 : 0;
        }
    }
}
