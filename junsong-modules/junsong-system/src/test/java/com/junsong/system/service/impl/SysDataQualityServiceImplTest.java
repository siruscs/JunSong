package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.vo.DataQualityDashboardVO;
import com.junsong.system.domain.vo.DataQualityIssueVO;
import com.junsong.system.mapper.SysDataQualityMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据质量服务测试。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class SysDataQualityServiceImplTest {

    private FakeDataQualityMapper mapper;
    private SysDataQualityServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = new FakeDataQualityMapper();
        service = new SysDataQualityServiceImpl(mapper);
    }

    @Test
    void dashboardAggregatesIssuesAndWorstSeverity() {
        mapper.saleWithoutDept = 2L;
        mapper.negativeStock = 1L;

        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals("BLOCKED", dashboard.getStatus());
        assertEquals(2, dashboard.getHighIssueCount());
        assertTrue(dashboard.getIssues().stream()
                .anyMatch(i -> "FINANCE_SALE_WITHOUT_DEPT".equals(i.getIssueType())));
        assertTrue(dashboard.getIssues().stream()
                .anyMatch(i -> "STOCK_NEGATIVE_POSITION".equals(i.getIssueType())));
    }

    @Test
    void dashboardIsHealthyWhenAllCountsAreZero() {
        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals("HEALTHY", dashboard.getStatus());
        assertEquals(0, dashboard.getTotalIssueCount());
        assertTrue(dashboard.getIssues().isEmpty());
    }

    @Test
    void dashboardStatusIsWarnWhenOnlyMediumIssues() {
        mapper.menuComponentEmpty = 3L;

        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals("WARN", dashboard.getStatus());
        assertEquals(1, dashboard.getMediumIssueCount());
        assertEquals(0, dashboard.getHighIssueCount());
        assertEquals(1, dashboard.getTotalIssueCount());
    }

    @Test
    void dashboardStatusIsWarnWhenOnlyLowIssues() {
        mapper.growthActionWithoutEffect = 5L;

        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals("WARN", dashboard.getStatus());
        assertEquals(1, dashboard.getLowIssueCount());
        assertEquals(0, dashboard.getHighIssueCount());
        assertEquals(0, dashboard.getMediumIssueCount());
    }

    @Test
    void nullCountFromMapperTreatedAsZero() {
        // FakeDataQualityMapper 默认返回 null 给 saleWithoutDept，验证 null → 0 处理
        mapper.saleWithoutDept = null;

        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals("HEALTHY", dashboard.getStatus());
        assertTrue(dashboard.getIssues().stream()
                .noneMatch(i -> "FINANCE_SALE_WITHOUT_DEPT".equals(i.getIssueType())));
    }

    @Test
    void issueDetailFieldsArePopulated() {
        mapper.saleWithoutDept = 10L;

        DataQualityDashboardVO dashboard = service.getDashboard();

        DataQualityIssueVO issue = dashboard.getIssues().stream()
                .filter(i -> "FINANCE_SALE_WITHOUT_DEPT".equals(i.getIssueType()))
                .findFirst().orElseThrow();
        assertEquals("finance", issue.getModule());
        assertEquals("HIGH", issue.getSeverity());
        assertEquals(10L, issue.getIssueCount());
        assertEquals("fin_sale_record", issue.getSourceTables());
        assertNotNull(issue.getReason());
        assertEquals("/finance/sale", issue.getDrilldownPath());
    }

    @Test
    void totalIssueCountMatchesIssuesListSize() {
        mapper.saleWithoutDept = 1L;
        mapper.paymentWithoutSale = 2L;
        mapper.menuComponentEmpty = 3L;
        mapper.growthActionWithoutEffect = 4L;

        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals(dashboard.getIssues().size(), dashboard.getTotalIssueCount());
        assertEquals(4, dashboard.getTotalIssueCount());
    }

    @Test
    void mapperExceptionSetsErrorStatusAndRecordsDbError() {
        mapper.throwOnSaleWithoutDept = true;

        DataQualityDashboardVO dashboard = service.getDashboard();

        assertEquals("ERROR", dashboard.getStatus(), "mapper 异常时 status 必须是 ERROR，不能是 HEALTHY");
        assertEquals(1, dashboard.getDbErrorCount());
        assertTrue(dashboard.getDbErrors().get(0).contains("countFinanceSaleWithoutDept"));
        // 异常查询不应出现在 issues 列表中（count = -1，被过滤）
        assertTrue(dashboard.getIssues().stream()
                .noneMatch(i -> "FINANCE_SALE_WITHOUT_DEPT".equals(i.getIssueType())));
    }

    @Test
    void mapperExceptionWithOtherIssuesStillReportsError() {
        mapper.throwOnSaleWithoutDept = true;
        mapper.negativeStock = 5L;

        DataQualityDashboardVO dashboard = service.getDashboard();

        // ERROR 优先级高于 BLOCKED
        assertEquals("ERROR", dashboard.getStatus());
        assertEquals(1, dashboard.getDbErrorCount());
        assertTrue(dashboard.getIssues().stream()
                .anyMatch(i -> "STOCK_NEGATIVE_POSITION".equals(i.getIssueType())));
    }

    // ==================== Fake Mapper ====================

    static class FakeDataQualityMapper implements SysDataQualityMapper {
        Long saleWithoutDept = 0L;
        Long paymentWithoutSale = 0L;
        Long receivableOverdueWithoutOwner = 0L;
        Long memberWithoutPhoneAndOpenid = 0L;
        Long growthActionWithoutEffect = 0L;
        Long negativeStock = 0L;
        Long menuComponentEmpty = 0L;
        boolean throwOnSaleWithoutDept = false;

        @Override
        public Long countFinanceSaleWithoutDept() {
            if (throwOnSaleWithoutDept) {
                throw new RuntimeException("simulated DB error: Table 'fin_sale_record' doesn't exist");
            }
            return saleWithoutDept;
        }

        @Override
        public Long countFinancePaymentWithoutSale() { return paymentWithoutSale; }

        @Override
        public Long countFinanceReceivableOverdueWithoutOwner() { return receivableOverdueWithoutOwner; }

        @Override
        public Long countMemberWithoutPhoneAndOpenid() { return memberWithoutPhoneAndOpenid; }

        @Override
        public Long countMemberGrowthActionWithoutEffect() { return growthActionWithoutEffect; }

        @Override
        public Long countNegativeStockPosition() { return negativeStock; }

        @Override
        public Long countSystemMenuComponentEmpty() { return menuComponentEmpty; }
    }
}
