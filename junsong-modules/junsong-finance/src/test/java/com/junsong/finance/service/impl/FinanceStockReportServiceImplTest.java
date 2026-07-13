package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.mapper.StockReportMapper;
import com.junsong.finance.service.IStockHealthService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存报表服务层单元测试。
 *
 * <p>使用手写 Fake Mapper（无 Mockito），通过反射注入依赖。
 * 覆盖数据权限交集、fail-closed、参数校验、汇总/分页/导出/对账/流水下钻等场景。</p>
 */
class FinanceStockReportServiceImplTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.remove();
        TenantContext.clear();
    }

    // ── 辅助方法 ──

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = FinanceReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static FinanceReportServiceImpl createService(
            StockReportMapper stockReportMapper,
            IStockHealthService stockHealthService,
            RemoteUserService remoteUserService) throws Exception {
        FinanceReportServiceImpl service = new FinanceReportServiceImpl();
        setField(service, "stockReportMapper", stockReportMapper);
        setField(service, "stockHealthService", stockHealthService);
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    private static SysDept makeDept(Long deptId) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        return dept;
    }

    private static StockReportItemVO makeItem(Long deptId, Long productId, String name) {
        StockReportItemVO item = new StockReportItemVO();
        item.setDeptId(deptId);
        item.setProductId(productId);
        item.setProductName(name);
        item.setClosingQuantity(10);
        item.setStockStatus("NORMAL");
        return item;
    }

    private static StockReportSummaryVO makeSummary() {
        StockReportSummaryVO s = new StockReportSummaryVO();
        s.setOpeningQuantity(100);
        s.setPurchaseNetInQuantity(50);
        s.setSaleNetOutQuantity(30);
        s.setClosingQuantity(120);
        return s;
    }

    // ── 数据权限：请求部门与授权部门取交集 ──

    @Test
    void summary_requestedDeptsIntersectedWithAuthorized() throws Exception {
        setupNonAdmin("store-mgr", 10L);
        TenantContext.setTenantId(1L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Arrays.asList(makeDept(10L), makeDept(20L)));

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        fakeMapper.summaryResult = makeSummary();

        FinanceReportServiceImpl service = createService(fakeMapper, new NoOpStockHealthService(), remoteService);

        StockReportQuery query = new StockReportQuery();
        query.setDeptIds(Arrays.asList(10L, 30L)); // 30L 不在授权列表
        service.getStockReportSummary(query);

        assertEquals(Arrays.asList(10L), query.getDeptIds(),
                "请求 [10,30]、授权 [10,20] 时，交集应为 [10]");
    }

    // ── 数据权限：交集为空时 fail-closed ──

    @Test
    void summary_emptyIntersectionFailsClosed() throws Exception {
        setupNonAdmin("store-mgr", 10L);
        TenantContext.setTenantId(1L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();

        FinanceReportServiceImpl service = createService(fakeMapper, new NoOpStockHealthService(), remoteService);

        StockReportQuery query = new StockReportQuery();
        query.setDeptIds(Collections.singletonList(99L)); // 不在授权列表

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getStockReportSummary(query));
        assertTrue(ex.getMessage().contains("授权") || ex.getMessage().contains("门店"),
                "空交集应抛出含'授权'或'门店'的 ServiceException");
    }

    // ── 缺失租户 fail-closed ──

    @Test
    void missingTenantFailsClosed() {
        StockReportQuery query = new StockReportQuery();
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FinanceReportServiceImpl.validateStockReportRequest(null, query));
        assertTrue(ex.getMessage().contains("租户"),
                "缺失租户应抛出含'租户'的 ServiceException");
    }

    // ── 日期区间超限 fail ──

    @Test
    void invalidDateRangeFails() {
        StockReportQuery query = new StockReportQuery();
        query.setStartDate(LocalDate.of(2026, 1, 1));
        query.setEndDate(LocalDate.of(2027, 1, 3)); // 367 天 > 366

        ServiceException ex = assertThrows(ServiceException.class,
                () -> FinanceReportServiceImpl.validateStockReportRequest(1L, query));
        assertTrue(ex.getMessage().contains("日期") || ex.getMessage().contains("366"),
                "日期区间超限应抛出含'日期'或'366'的 ServiceException");
    }

    // ── pageSize 超限 fail ──

    @Test
    void invalidPageSizeFails() {
        StockReportQuery query = new StockReportQuery();
        query.setPageSize(0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FinanceReportServiceImpl.validateStockReportRequest(1L, query));
        assertTrue(ex.getMessage().contains("页") || ex.getMessage().contains("size"),
                "pageSize=0 应抛出含'页'或'size'的 ServiceException");

        StockReportQuery query2 = new StockReportQuery();
        query2.setPageSize(201);
        assertThrows(ServiceException.class,
                () -> FinanceReportServiceImpl.validateStockReportRequest(1L, query2));
    }

    // ── 汇总返回正确数据 ──

    @Test
    void summaryReturnsCorrectData() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(1L);

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        StockReportSummaryVO expected = makeSummary();
        fakeMapper.summaryResult = expected;

        FinanceReportServiceImpl service = createService(fakeMapper,
                new NoOpStockHealthService(), new ConfigurableRemoteUserService());

        StockReportQuery query = new StockReportQuery();
        StockReportSummaryVO result = service.getStockReportSummary(query);

        assertNotNull(result);
        assertEquals(100, result.getOpeningQuantity());
        assertEquals(50, result.getPurchaseNetInQuantity());
        assertEquals(30, result.getSaleNetOutQuantity());
        assertEquals(120, result.getClosingQuantity());
        assertEquals(1L, fakeMapper.lastTenantId, "Mapper 应收到 TenantContext 的 tenantId");
    }

    // ── 分页返回正确数据 ──

    @Test
    void pageReturnsCorrectData() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(2L);

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        List<StockReportItemVO> items = Arrays.asList(
                makeItem(10L, 1L, "商品A"),
                makeItem(10L, 2L, "商品B"));
        fakeMapper.itemsResult = items;

        FinanceReportServiceImpl service = createService(fakeMapper,
                new NoOpStockHealthService(), new ConfigurableRemoteUserService());

        StockReportQuery query = new StockReportQuery();
        query.setPageNum(1);
        query.setPageSize(20);
        List<StockReportItemVO> result = service.getStockReportPage(query);

        assertEquals(2, result.size());
        assertEquals("商品A", result.get(0).getProductName());
        assertEquals(2L, fakeMapper.lastTenantId, "Mapper 应收到 TenantContext 的 tenantId=2L");
    }

    // ── 导出返回全部数据（不分页） ──

    @Test
    void exportReturnsAllItemsWithoutPagination() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(1L);

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        List<StockReportItemVO> items = Arrays.asList(
                makeItem(10L, 1L, "商品A"),
                makeItem(10L, 2L, "商品B"),
                makeItem(10L, 3L, "商品C"));
        fakeMapper.allItemsResult = items;

        FinanceReportServiceImpl service = createService(fakeMapper,
                new NoOpStockHealthService(), new ConfigurableRemoteUserService());

        StockReportQuery query = new StockReportQuery();
        query.setPageNum(1);
        query.setPageSize(20);
        List<StockReportItemVO> result = service.exportStockReport(query);

        assertEquals(3, result.size(), "导出应返回全部 3 条数据");
        assertTrue(fakeMapper.selectAllCalled, "导出应调用 selectAllStockReportItems（不分页）");
    }

    // ── 对账委托给 stockHealthService ──

    @Test
    void reconciliationDelegatesToStockHealthService() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(3L);

        FakeStockHealthService fakeHealthService = new FakeStockHealthService();
        StockReconciliationResultVO expectedResult = new StockReconciliationResultVO();
        expectedResult.setStatus("HEALTHY");
        fakeHealthService.reconcileResult = expectedResult;

        FinanceReportServiceImpl service = createService(new FakeStockReportMapper(),
                fakeHealthService, new ConfigurableRemoteUserService());

        StockReportQuery query = new StockReportQuery();
        query.setDeptIds(Arrays.asList(10L, 20L));
        StockReconciliationResultVO result = service.getStockReconciliation(query);

        assertNotNull(result);
        assertEquals("HEALTHY", result.getStatus());
        assertEquals(3L, fakeHealthService.lastTenantId,
                "对账应使用 TenantContext 的 tenantId=3L");
        assertEquals(Arrays.asList(10L, 20L), fakeHealthService.lastDeptIds,
                "对账应传递授权后的 deptIds");
    }

    // ── 流水下钻校验必填参数 ──

    @Test
    void ledgerPageValidatesRequiredParams() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(1L);
        FinanceReportServiceImpl service = createService(new FakeStockReportMapper(),
                new NoOpStockHealthService(), new ConfigurableRemoteUserService());

        // deptId 为 null
        ServiceException ex1 = assertThrows(ServiceException.class,
                () -> service.getStockLedgerPage(null, 1L, null, null, 1, 20));
        assertTrue(ex1.getMessage().contains("门店") || ex1.getMessage().contains("商品"),
                "deptId 为 null 时应抛出含'门店'或'商品'的 ServiceException");

        // productId 为 null
        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> service.getStockLedgerPage(10L, null, null, null, 1, 20));
        assertTrue(ex2.getMessage().contains("门店") || ex2.getMessage().contains("商品"),
                "productId 为 null 时应抛出含'门店'或'商品'的 ServiceException");
    }

    // ── 流水下钻：非 admin 访问未授权门店 fail-closed ──

    @Test
    void ledgerPage_unauthorizedDeptFailsClosed() throws Exception {
        setupNonAdmin("store-mgr", 10L);
        TenantContext.setTenantId(1L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        FinanceReportServiceImpl service = createService(new FakeStockReportMapper(),
                new NoOpStockHealthService(), remoteService);

        // deptId=99 不在授权 [10] 范围内
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getStockLedgerPage(99L, 5L, null, null, 1, 20));
        assertTrue(ex.getMessage().contains("授权") || ex.getMessage().contains("门店"),
                "非 admin 访问未授权门店应 fail-closed");
    }

    // ── 流水下钻：非 admin 访问已授权门店成功 ──

    @Test
    void ledgerPage_authorizedDeptSucceeds() throws Exception {
        setupNonAdmin("store-mgr", 10L);
        TenantContext.setTenantId(1L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Arrays.asList(makeDept(10L), makeDept(20L)));

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        StockLedgerRowVO row = new StockLedgerRowVO();
        row.setChangeType("PURCHASE_IN");
        row.setChangeQuantity(10);
        fakeMapper.ledgerResult = Collections.singletonList(row);

        FinanceReportServiceImpl service = createService(fakeMapper,
                new NoOpStockHealthService(), remoteService);

        Map<String, Object> result = service.getStockLedgerPage(
                20L, 5L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1, 20);

        assertNotNull(result);
        assertEquals(20L, fakeMapper.lastDeptId, "应传入授权的 deptId=20");
        assertEquals(5L, fakeMapper.lastProductId);
        @SuppressWarnings("unchecked")
        List<StockLedgerRowVO> rows = (List<StockLedgerRowVO>) result.get("rows");
        assertEquals(1, rows.size());
        assertEquals(1, result.get("total"));
    }

    // ── 流水下钻返回分页包装数据 ──

    @Test
    void ledgerPageReturnsPaginatedWrapper() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(1L);

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        StockLedgerRowVO row = new StockLedgerRowVO();
        row.setChangeType("PURCHASE_IN");
        row.setChangeQuantity(10);
        fakeMapper.ledgerResult = Collections.singletonList(row);

        FinanceReportServiceImpl service = createService(fakeMapper,
                new NoOpStockHealthService(), new ConfigurableRemoteUserService());

        Map<String, Object> result = service.getStockLedgerPage(
                10L, 5L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1, 20);

        assertNotNull(result);
        assertNotNull(result.get("rows"), "返回结果应包含 rows 字段");
        assertNotNull(result.get("total"), "返回结果应包含 total 字段");
        assertNotNull(result.get("pageNum"), "返回结果应包含 pageNum 字段");
        assertNotNull(result.get("pageSize"), "返回结果应包含 pageSize 字段");
        assertEquals(1, result.get("total"));
        assertEquals(1, result.get("pageNum"));
        assertEquals(20, result.get("pageSize"));
    }

    // ── 复合报表：getStockReport 组合 summary + items + total ──

    @Test
    void getStockReportCombinesSummaryAndPage() throws Exception {
        setupAdmin();
        TenantContext.setTenantId(1L);

        FakeStockReportMapper fakeMapper = new FakeStockReportMapper();
        fakeMapper.summaryResult = makeSummary();
        fakeMapper.itemsResult = Arrays.asList(
                makeItem(10L, 1L, "商品A"),
                makeItem(10L, 2L, "商品B"));
        fakeMapper.countResult = 25L;

        FinanceReportServiceImpl service = createService(fakeMapper,
                new NoOpStockHealthService(), new ConfigurableRemoteUserService());

        StockReportQuery query = new StockReportQuery();
        query.setPageNum(1);
        query.setPageSize(20);
        StockReportVO vo = service.getStockReport(query);

        assertNotNull(vo.getSummary());
        assertEquals(100, vo.getSummary().getOpeningQuantity());
        assertEquals(2, vo.getItems().size());
        assertEquals(25L, vo.getTotal());
        assertEquals(1, vo.getPageNum());
        assertEquals(20, vo.getPageSize());
    }

    // ── Fake：StockReportMapper ──

    static class FakeStockReportMapper implements StockReportMapper {
        StockReportSummaryVO summaryResult;
        List<StockReportItemVO> itemsResult = Collections.emptyList();
        List<StockReportItemVO> allItemsResult = Collections.emptyList();
        long countResult = 0;
        List<StockLedgerRowVO> ledgerResult = Collections.emptyList();
        boolean selectAllCalled = false;

        StockReportQuery lastQuery;
        Long lastTenantId;
        Long lastDeptId;
        Long lastProductId;
        LocalDate lastStartDate;
        LocalDate lastEndDate;

        @Override
        public StockReportSummaryVO selectStockReportSummary(Long tenantId, StockReportQuery query) {
            lastTenantId = tenantId;
            lastQuery = query;
            return summaryResult;
        }

        @Override
        public List<StockReportItemVO> selectStockReportItems(Long tenantId, StockReportQuery query) {
            lastTenantId = tenantId;
            lastQuery = query;
            return itemsResult;
        }

        @Override
        public long countStockReportItems(Long tenantId, StockReportQuery query) {
            lastTenantId = tenantId;
            lastQuery = query;
            return countResult;
        }

        @Override
        public List<StockReportItemVO> selectAllStockReportItems(Long tenantId, StockReportQuery query) {
            lastTenantId = tenantId;
            lastQuery = query;
            selectAllCalled = true;
            return allItemsResult;
        }

        @Override
        public List<StockLedgerRowVO> selectStockLedgerRows(Long tenantId, Long deptId, Long productId,
                                                              LocalDate startDate, LocalDate endDate) {
            lastTenantId = tenantId;
            lastDeptId = deptId;
            lastProductId = productId;
            lastStartDate = startDate;
            lastEndDate = endDate;
            return ledgerResult;
        }

        // 第二期方法桩：本测试不覆盖价值报表，返回安全空值
        @Override
        public StockValueReportVO selectStockValueSummary(Long tenantId, StockReportQuery query) {
            return new StockValueReportVO();
        }

        @Override
        public List<StockValueReportItemVO> selectStockValueItems(Long tenantId, StockReportQuery query) {
            return Collections.emptyList();
        }

        @Override
        public boolean existsCostLayerForTenant(Long tenantId, List<Long> deptIds) {
            return false;
        }
    }

    // ── Fake：IStockHealthService ──

    static class FakeStockHealthService implements IStockHealthService {
        StockReconciliationResultVO reconcileResult;
        Long lastTenantId;
        List<Long> lastDeptIds;

        @Override
        public StockHealthVO checkHealth(Long tenantId, List<Long> deptIds) {
            return new StockHealthVO();
        }

        @Override
        public StockReconciliationResultVO reconcileStock(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return reconcileResult;
        }
    }

    static class NoOpStockHealthService implements IStockHealthService {
        @Override
        public StockHealthVO checkHealth(Long tenantId, List<Long> deptIds) {
            return new StockHealthVO();
        }

        @Override
        public StockReconciliationResultVO reconcileStock(Long tenantId, List<Long> deptIds) {
            StockReconciliationResultVO r = new StockReconciliationResultVO();
            r.setStatus("HEALTHY");
            r.setRows(Collections.emptyList());
            return r;
        }
    }

    // ── Fake：RemoteUserService ──

    static class ConfigurableRemoteUserService implements RemoteUserService {
        R<List<SysDept>> deptListResponse = R.ok(Collections.emptyList());

        @Override
        public R<LoginUser> getUserInfo(String username, String source) {
            return R.fail();
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) {
            return R.fail();
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) {
            return R.fail();
        }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            return deptListResponse;
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) {
            return R.fail();
        }
    }
}
