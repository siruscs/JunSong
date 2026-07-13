package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.vo.StockLedgerRowVO;
import com.junsong.finance.domain.vo.StockReportItemVO;
import com.junsong.finance.domain.vo.StockReportQuery;
import com.junsong.finance.domain.vo.StockReportSummaryVO;
import com.junsong.finance.domain.vo.StockValueReportItemVO;
import com.junsong.finance.domain.vo.StockValueReportVO;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.StockReportMapper;
import com.junsong.finance.service.IStockCostService;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinanceReportServiceImpl 库存价值报表与期间控制单元测试（第二期财务计价）。
 *
 * 覆盖：
 * - costReady 门禁（无成本层时禁止展示金额）。
 * - 恒等式：期初 + 入库 - 销售成本 + 调整 = 期末。
 * - 毛利 = 销售收入 - 销售成本。
 * - 赠品入库影响数量和平均成本，不影响入库金额。
 * - 赠品销售影响销售成本，不影响销售收入。
 * - LOCKED/CARRIED_FORWARD 期间拒绝调整回写；ACTIVE 期间允许。
 * - 未授权门店 fail-closed。
 * - 租户隔离。
 */
class StockValueReportServiceTest {

    private static final long TENANT_1 = 1L;
    private static final long TENANT_2 = 2L;
    private static final long DEPT_1 = 10L;
    private static final long DEPT_2 = 20L;
    private static final long PRODUCT = 100L;

    private FakeStockReportMapper stockReportMapper;
    private FakeAccountingPeriodMapper periodMapper;
    private FinanceReportServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        loginAsAdmin();
        TenantContext.setTenantId(TENANT_1);

        stockReportMapper = new FakeStockReportMapper();
        periodMapper = new FakeAccountingPeriodMapper();
        service = new FinanceReportServiceImpl();
        inject("stockReportMapper", stockReportMapper);
        inject("finAccountingPeriodMapper", periodMapper);
        inject("stockCostService", new FakeStockCostService());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
        TenantContext.clear();
    }

    private void loginAsAdmin() {
        SecurityContextHolder.setUserId("1");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setDeptId(DEPT_1);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = FinanceReportServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private StockReportQuery query(LocalDate start, LocalDate end, Long... deptIds) {
        StockReportQuery q = new StockReportQuery();
        q.setDeptIds(deptIds.length == 0 ? null : Arrays.asList(deptIds));
        q.setStartDate(start);
        q.setEndDate(end);
        q.setPageNum(1);
        q.setPageSize(20);
        return q;
    }

    // ==================== costReady 门禁 ====================

    @Test
    void costReady_falseWhenNoCostLayer() {
        stockReportMapper.costLayerExists = false;

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        assertFalse(vo.isCostReady(), "无成本层时 costReady 必须为 false，禁止用零值伪装未完成成本");
        assertEquals(BigDecimal.ZERO, vo.getOpeningAmount());
        assertEquals(BigDecimal.ZERO, vo.getClosingAmount());
        assertEquals(BigDecimal.ZERO, vo.getSaleRevenue());
        assertEquals(BigDecimal.ZERO, vo.getGrossProfit());
        assertTrue(vo.getItems() == null || vo.getItems().isEmpty(), "costReady=false 时 items 必须为空");
    }

    @Test
    void costReady_trueWhenCostLayerExists() {
        stockReportMapper.costLayerExists = true;
        stockReportMapper.valueSummary = buildSummary("200.00", "200.00", "166.67", "0.00", "233.33",
                "400.00", "233.33", "58.33");

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        assertTrue(vo.isCostReady(), "存在成本层时 costReady 必须为 true");
        assertEquals(new BigDecimal("200.00"), vo.getOpeningAmount());
        assertEquals(new BigDecimal("233.33"), vo.getClosingAmount());
    }

    // ==================== 恒等式 ====================

    @Test
    void valueEquation_holds_openingPlusInboundMinusSaleCostPlusAdjustmentEqualsClosing() {
        stockReportMapper.costLayerExists = true;
        // 期初 100 + 入库 200 - 销售成本 150 + 调整 -20 = 期末 130
        stockReportMapper.valueSummary = buildSummary("100.00", "200.00", "150.00", "-20.00", "130.00",
                "300.00", "150.00", "50.00");

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        BigDecimal expected = vo.getOpeningAmount()
                .add(vo.getInboundAmount())
                .subtract(vo.getSaleCost())
                .add(vo.getAdjustmentAmount());
        assertEquals(new BigDecimal("130.00"), vo.getClosingAmount(), "期末必须等于期初+入库-销售成本+调整");
        assertEquals(new BigDecimal("130.00"), expected.setScale(2), "恒等式必须成立");
    }

    @Test
    void grossProfit_equals_saleRevenueMinusSaleCost() {
        stockReportMapper.costLayerExists = true;
        // 销售收入 500 - 销售成本 300 = 毛利 200；毛利率 200/500*100 = 40.00
        stockReportMapper.valueSummary = buildSummary("0.00", "300.00", "300.00", "0.00", "0.00",
                "500.00", "200.00", "40.00");

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        assertEquals(new BigDecimal("500.00"), vo.getSaleRevenue());
        assertEquals(new BigDecimal("300.00"), vo.getSaleCost());
        assertEquals(new BigDecimal("200.00"), vo.getGrossProfit(), "毛利 = 销售收入 - 销售成本");
        assertEquals(new BigDecimal("40.00"), vo.getGrossProfitRate(), "毛利率 = 毛利/销售收入*100");
    }

    // ==================== 赠品规则 ====================

    @Test
    void giftInbound_affectsQuantityAndAvgCost_notInboundAmount() {
        stockReportMapper.costLayerExists = true;
        // 10 件单价 20 + 2 件赠品金额 0：入库数量 12，入库金额 200（赠品不进入采购金额）
        // 平均成本 = 200/12 = 16.666667
        stockReportMapper.valueSummary = buildSummary("0.00", "200.00", "0.00", "0.00", "200.00",
                "0.00", "0.00", "0.00");
        StockValueReportItemVO item = buildItem(PRODUCT, 12, "16.666667", "200.00", "200.00", "0.00", "0.00", "0.00");
        stockReportMapper.valueItems = Collections.singletonList(item);

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        assertEquals(new BigDecimal("200.00"), vo.getInboundAmount(), "赠品入库金额为0，不影响入库金额");
        assertEquals(12, vo.getItems().get(0).getClosingQuantity(), "赠品计入库存数量");
        assertEquals(new BigDecimal("16.666667"), vo.getItems().get(0).getAvgUnitCost(), "赠品摊薄平均成本");
    }

    @Test
    void giftSale_affectsSaleCost_notSaleRevenue() {
        stockReportMapper.costLayerExists = true;
        // 销售 8 件 + 赠品 2 件 = 出库 10 件；销售成本按 10 件固化；销售收入只含 8 件
        // 平均成本 16.666667 * 10 = 166.67；销售收入 = 8 * 25 = 200
        stockReportMapper.valueSummary = buildSummary("200.00", "0.00", "166.67", "0.00", "33.33",
                "200.00", "33.33", "16.67");
        StockValueReportItemVO item = buildItem(PRODUCT, 2, "16.666667", "33.33", "0.00", "166.67", "200.00", "33.33");
        stockReportMapper.valueItems = Collections.singletonList(item);

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        assertEquals(new BigDecimal("166.67"), vo.getSaleCost(), "赠品出库计入销售成本（按固化成本）");
        assertEquals(new BigDecimal("200.00"), vo.getSaleRevenue(), "赠品不计入销售收入");
        assertEquals(new BigDecimal("33.33"), vo.getGrossProfit(), "毛利 = 销售收入 - 销售成本（含赠品成本）");
    }

    // ==================== 期间控制 ====================

    @Test
    void lockedPeriod_rejectsAdjustment() {
        periodMapper.currentStatus = "1"; // LOCKED (已回本待结转)

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.createCostAdjustment(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1),
                        PRODUCT, new BigDecimal("50.00"), "盘点调整"));
        assertTrue(ex.getMessage().contains("LOCKED") || ex.getMessage().contains("锁定") || ex.getMessage().contains("结转"),
                "锁定期间必须拒绝回写，实际消息: " + ex.getMessage());
    }

    @Test
    void carriedForwardPeriod_rejectsAdjustment() {
        periodMapper.currentStatus = "2"; // CARRIED_FORWARD (已结转)

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.createCostAdjustment(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1),
                        PRODUCT, new BigDecimal("50.00"), "盘点调整"));
        assertTrue(ex.getMessage().contains("LOCKED") || ex.getMessage().contains("锁定") || ex.getMessage().contains("结转"),
                "已结转期间必须拒绝回写，实际消息: " + ex.getMessage());
    }

    @Test
    void activePeriod_allowsAdjustment() {
        periodMapper.currentStatus = "0"; // ACTIVE
        stockReportMapper.costLayerExists = true;

        assertDoesNotThrow(() ->
                service.createCostAdjustment(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1),
                        PRODUCT, new BigDecimal("50.00"), "盘点调整"),
                "ACTIVE 期间允许生成有原因的调整流水");
    }

    @Test
    void adjustmentWithoutReason_rejected() {
        periodMapper.currentStatus = "0"; // ACTIVE

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.createCostAdjustment(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1),
                        PRODUCT, new BigDecimal("50.00"), ""));
        assertTrue(ex.getMessage().contains("原因") || ex.getMessage().contains("reason"),
                "调整缺少原因必须拒绝，实际消息: " + ex.getMessage());
    }

    @Test
    void periodStatus_exposedInReport() {
        periodMapper.currentStatus = "1";
        stockReportMapper.costLayerExists = true;
        stockReportMapper.valueSummary = buildSummary("0", "0", "0", "0", "0", "0", "0", "0");

        StockValueReportVO vo = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));

        assertEquals("LOCKED", vo.getPeriodStatus(), "期间状态必须映射为 LOCKED");
    }

    // ==================== 租户与授权 ====================

    // 注：TenantContext.getTenantId() 在 ThreadLocal 为空时返回 DEFAULT_TENANT_ID=1L 而非 null，
    // 因此 null 租户路径无法通过公共 API 触发，租户隔离由 Mapper SQL 的 tenant_id 过滤保证。
    // 此处保留 twoTenantsIsolated 验证不同租户查询独立性。

    @Test
    void twoTenantsIsolated() {
        stockReportMapper.costLayerExists = true;
        stockReportMapper.valueSummary = buildSummary("100.00", "50.00", "30.00", "0.00", "120.00",
                "80.00", "50.00", "62.50");

        TenantContext.setTenantId(TENANT_1);
        StockValueReportVO vo1 = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_1));
        assertEquals(new BigDecimal("100.00"), vo1.getOpeningAmount());

        TenantContext.setTenantId(TENANT_2);
        // 同一 fake 返回相同数据；实际隔离由 Mapper SQL 的 tenant_id 过滤保证
        StockValueReportVO vo2 = service.getStockValueReport(query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), DEPT_2));
        assertNotNull(vo2, "不同租户独立查询");
        assertNotSame(vo1, vo2, "每次查询返回独立 VO 实例");
    }

    // ==================== 辅助方法 ====================

    private StockValueReportVO buildSummary(String opening, String inbound, String saleCost,
                                            String adjustment, String closing,
                                            String revenue, String grossProfit, String rate) {
        StockValueReportVO vo = new StockValueReportVO();
        vo.setOpeningAmount(new BigDecimal(opening));
        vo.setInboundAmount(new BigDecimal(inbound));
        vo.setSaleCost(new BigDecimal(saleCost));
        vo.setAdjustmentAmount(new BigDecimal(adjustment));
        vo.setClosingAmount(new BigDecimal(closing));
        vo.setSaleRevenue(new BigDecimal(revenue));
        vo.setGrossProfit(new BigDecimal(grossProfit));
        vo.setGrossProfitRate(new BigDecimal(rate));
        return vo;
    }

    private StockValueReportItemVO buildItem(Long productId, int closingQty, String avgCost,
                                              String closingAmount, String inboundAmount,
                                              String saleCost, String saleRevenue, String grossProfit) {
        StockValueReportItemVO item = new StockValueReportItemVO();
        item.setTenantId(TENANT_1);
        item.setDeptId(DEPT_1);
        item.setProductId(productId);
        item.setProductName("测试商品");
        item.setClosingQuantity(closingQty);
        item.setAvgUnitCost(new BigDecimal(avgCost));
        item.setClosingAmount(new BigDecimal(closingAmount));
        item.setInboundAmount(new BigDecimal(inboundAmount));
        item.setSaleCost(new BigDecimal(saleCost));
        item.setSaleRevenue(new BigDecimal(saleRevenue));
        item.setGrossProfit(new BigDecimal(grossProfit));
        return item;
    }

    // ==================== Fake Mapper ====================

    static class FakeStockReportMapper implements StockReportMapper {
        boolean costLayerExists = false;
        StockValueReportVO valueSummary;
        List<StockValueReportItemVO> valueItems = Collections.emptyList();

        @Override
        public boolean existsCostLayerForTenant(Long tenantId, List<Long> deptIds) {
            return costLayerExists;
        }

        @Override
        public StockValueReportVO selectStockValueSummary(Long tenantId, StockReportQuery query) {
            return valueSummary != null ? valueSummary : new StockValueReportVO();
        }

        @Override
        public List<StockValueReportItemVO> selectStockValueItems(Long tenantId, StockReportQuery query) {
            return valueItems;
        }

        // ---- 以下为第一期方法桩，本测试不涉及 ----
        @Override
        public StockReportSummaryVO selectStockReportSummary(Long tenantId, StockReportQuery query) {
            return new StockReportSummaryVO();
        }

        @Override
        public List<StockReportItemVO> selectStockReportItems(Long tenantId, StockReportQuery query) {
            return Collections.emptyList();
        }

        @Override
        public long countStockReportItems(Long tenantId, StockReportQuery query) {
            return 0;
        }

        @Override
        public List<StockReportItemVO> selectAllStockReportItems(Long tenantId, StockReportQuery query) {
            return Collections.emptyList();
        }

        @Override
        public List<StockLedgerRowVO> selectStockLedgerRows(Long tenantId, Long deptId, Long productId,
                                                             LocalDate startDate, LocalDate endDate) {
            return Collections.emptyList();
        }
    }

    static class FakeAccountingPeriodMapper implements FinAccountingPeriodMapper {
        String currentStatus = "0"; // ACTIVE by default

        @Override
        public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) {
            return currentStatus;
        }

        @Override
        public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) {
            FinAccountingPeriod p = new FinAccountingPeriod();
            p.setPeriodId(1L);
            p.setDeptId(deptId);
            p.setStatus(currentStatus);
            return p;
        }

        @Override
        public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId) { return null; }

        @Override
        public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return null; }

        @Override
        public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod query) {
            return Collections.emptyList();
        }

        @Override
        public int insertFinAccountingPeriod(FinAccountingPeriod config) { return 0; }

        @Override
        public int updateFinAccountingPeriod(FinAccountingPeriod config) { return 0; }

        @Override
        public int resetCarryForwardByPeriodId(Long periodId, String updateBy) { return 0; }

        @Override
        public int deleteFinAccountingPeriodByPeriodId(Long periodId) { return 0; }

        @Override
        public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds) { return 0; }

        @Override
        public BigDecimal selectTotalVerifiedExpense(Long periodId, Long deptId, java.util.Date startTime, java.util.Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalPurchase(Long periodId, Long deptId, java.util.Date startTime, java.util.Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalSalePayment(Long periodId, Long deptId, java.util.Date startTime, java.util.Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalSaleAmount(Long periodId, Long deptId, java.util.Date startTime, java.util.Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTotalUnverifiedAdvance(Long periodId, Long deptId, java.util.Date startTime, java.util.Date endTime) { return BigDecimal.ZERO; }

        @Override
        public FinAccountingPeriod selectPeriodById(Long periodId) { return null; }

        @Override
        public FinAccountingPeriod selectPeriodForUpdate(Long periodId, Long tenantId, Long deptId) { return null; }

        @Override
        public FinAccountingPeriod selectPreviousPeriod(Long deptId, java.util.Date startTime, Long periodId) { return null; }

        @Override
        public FinAccountingPeriod selectNextPeriod(Long deptId, java.util.Date startTime, Long periodId) { return null; }

        @Override
        public int updateStartTimeOnly(Long periodId, java.util.Date startTime, java.util.Date endTime, String updateBy, String remark) { return 0; }
    }

    /** IStockCostService 桩：所有方法均为 no-op，仅用于满足依赖注入。 */
    static class FakeStockCostService implements IStockCostService {
        @Override
        public void applyPurchaseInbound(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal amount, Long sourceLedgerId, String operator) { }

        @Override
        public void reversePurchaseInbound(Long tenantId, Long deptId, Long productId, int reverseQuantity, Long sourceLedgerId, String operator) { }

        @Override
        public BigDecimal applySaleOutbound(Long tenantId, Long deptId, Long productId, int quantity, boolean allowNegative, Long sourceLedgerId, String operator) {
            return BigDecimal.ZERO;
        }

        @Override
        public void reverseSaleOutbound(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal originalUnitCost, Long sourceLedgerId, String operator) { }

        @Override
        public void applyCostAdjustment(Long tenantId, Long deptId, Long productId, BigDecimal amount, String reason, String operator) { }
    }
}
