package com.junsong.finance.service.impl;

import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.FinStocktakeHistory;
import com.junsong.finance.domain.FinStocktakeItem;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.mapper.FinStocktakeMapper;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinStocktakeServiceImpl 单元测试（Task 3：创建、分配、列表、详情）。
 *
 * 手写 fake mapper（无 Mockito），覆盖：
 * - 租户缺失拒绝
 * - 未授权部门拒绝
 * - 空商品范围拒绝
 * - 重复 takeNo 拒绝
 * - 期望数量从权威 position 冻结
 * - 零库存选中商品仍包含
 * - counter 详情隐藏期望值（盲盘保护）
 * - 分配盘点人成功（乐观锁）
 * - 列表按授权部门过滤
 */
class FinStocktakeServiceImplTest {

    private static final Long T1 = 1L;
    private static final Long DEPT_10 = 10L;
    private static final Long DEPT_20 = 20L; // 未授权部门
    private static final Long PRODUCT_100 = 100L;
    private static final Long PRODUCT_200 = 200L;
    private static final Long COUNTER_USER = 3001L;

    private FakeFinStocktakeMapper stocktakeMapper;
    private FakeFinStockLedgerMapper ledgerMapper;
    private FakeFinProductMapper productMapper;
    private FakeRemoteUserService remoteUserService;
    private FinStocktakeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        stocktakeMapper = new FakeFinStocktakeMapper();
        ledgerMapper = new FakeFinStockLedgerMapper();
        productMapper = new FakeFinProductMapper();
        remoteUserService = new FakeRemoteUserService();
        service = new FinStocktakeServiceImpl();

        inject("finStocktakeMapper", stocktakeMapper);
        inject("finStockLedgerMapper", ledgerMapper);
        inject("finProductMapper", productMapper);
        inject("remoteUserService", remoteUserService);

        // 默认 admin 上下文
        TenantContext.setTenantId(T1);
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.remove();
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = FinStocktakeServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    private StocktakeCreateRequest buildCreateRequest() {
        StocktakeCreateRequest req = new StocktakeCreateRequest();
        req.setTakeNo("PD-20260725-001");
        req.setDeptId(DEPT_10);
        req.setScopeType("SELECTED_PRODUCTS");
        req.setProductIds(Arrays.asList(PRODUCT_100, PRODUCT_200));
        req.setCounterUserId(COUNTER_USER);
        return req;
    }

    // ===== 创建 =====

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThrows(ServiceException.class, () -> service.createStocktake(buildCreateRequest()));
    }

    @Test
    void create_unauthorizedDept_throws() {
        // 非 admin，授权部门仅 DEPT_10，请求 DEPT_20
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        StocktakeCreateRequest req = buildCreateRequest();
        req.setDeptId(DEPT_20);
        assertThrows(ServiceException.class, () -> service.createStocktake(req));
        assertTrue(stocktakeMapper.insertedHeaders.isEmpty(), "未授权部门创建不得写入头表");
    }

    @Test
    void create_emptyProductScope_throws() {
        StocktakeCreateRequest req = buildCreateRequest();
        req.setProductIds(new ArrayList<>());
        assertThrows(ServiceException.class, () -> service.createStocktake(req));
    }

    @Test
    void create_duplicateTakeNo_throws() {
        stocktakeMapper.existingTakeNos.add(T1 + ":" + "PD-20260725-001");

        assertThrows(ServiceException.class, () -> service.createStocktake(buildCreateRequest()));
    }

    @Test
    void create_freezesExpectedQuantityFromPosition() {
        // position 中 PRODUCT_100 有 50 件
        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 50);
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        productMapper.products.put(PRODUCT_200, buildProduct(PRODUCT_200, "雪碧"));

        Long id = service.createStocktake(buildCreateRequest());

        assertNotNull(id);
        assertEquals(1, stocktakeMapper.insertedHeaders.size());
        FinStocktake header = stocktakeMapper.insertedHeaders.get(0);
        assertEquals("DRAFT", header.getStatus());
        assertEquals(T1, header.getTenantId());
        assertEquals(DEPT_10, header.getDeptId());
        assertEquals(0, header.getVersion());
        assertNotNull(header.getFreezeTime());

        // 2 个商品行
        assertEquals(2, stocktakeMapper.insertedItems.size());
        FinStocktakeItem item100 = stocktakeMapper.insertedItems.get(0);
        assertEquals(PRODUCT_100, item100.getProductId());
        assertEquals(50, item100.getExpectedQuantity(), "期望数量必须从 position 冻结");
        assertEquals(0, item100.getMovementQuantityAfterFreeze());

        // 历史
        assertEquals(1, stocktakeMapper.insertedHistories.size());
        assertEquals("CREATE", stocktakeMapper.insertedHistories.get(0).getAction());
    }

    @Test
    void create_zeroStockProductStillIncluded() {
        // PRODUCT_200 在 position 中不存在（零库存），仍应被包含
        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 30);
        // PRODUCT_200 无 position 记录
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        productMapper.products.put(PRODUCT_200, buildProduct(PRODUCT_200, "雪碧"));

        service.createStocktake(buildCreateRequest());

        assertEquals(2, stocktakeMapper.insertedItems.size(), "零库存商品仍应被包含");
        FinStocktakeItem item200 = stocktakeMapper.insertedItems.get(1);
        assertEquals(PRODUCT_200, item200.getProductId());
        assertEquals(0, item200.getExpectedQuantity(), "零库存商品期望数量应为 0");
    }

    @Test
    void create_productNotBelongToDept_throws() {
        // 商品不属于该门店
        StocktakeCreateRequest req = buildCreateRequest();
        assertThrows(ServiceException.class, () -> service.createStocktake(req));
    }

    // ===== 列表 =====

    @Test
    void list_filtersByAuthorizedDepts() {
        // 非 admin，授权部门仅 DEPT_10
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        // 数据库中有 DEPT_10 和 DEPT_20 两个任务
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT"));
        stocktakeMapper.headers.add(buildHeader(T1, 2L, DEPT_20, "PD-002", "DRAFT"));

        StocktakeQuery query = new StocktakeQuery();
        List<FinStocktake> result = service.listStocktakes(query);

        assertEquals(1, result.size(), "未授权部门 DEPT_20 的任务不应返回");
        assertEquals(DEPT_10, result.get(0).getDeptId());
    }

    @Test
    void list_adminReturnsAllDepts() {
        // admin 跳过部门过滤
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT"));
        stocktakeMapper.headers.add(buildHeader(T1, 2L, DEPT_20, "PD-002", "DRAFT"));

        StocktakeQuery query = new StocktakeQuery();
        List<FinStocktake> result = service.listStocktakes(query);

        assertEquals(2, result.size(), "admin 应返回所有部门的任务");
    }

    // ===== 详情 =====

    @Test
    void detail_counterRoleHidesExpectedValues() {
        // 非 admin，是 counter，任务 DRAFT/COUNTING 状态
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(2L); // 当前用户是 counter
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = new FinStocktakeItem();
        item.setItemId(11L);
        item.setStocktakeId(1L);
        item.setTenantId(T1);
        item.setDeptId(DEPT_10);
        item.setProductId(PRODUCT_100);
        item.setExpectedQuantity(50);
        item.setActualQuantity(48);
        item.setVarianceQuantity(-2);
        item.setUnitCost(new java.math.BigDecimal("3.50"));
        item.setVarianceAmount(new java.math.BigDecimal("-7.00"));
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeDetailVO vo = service.getStocktakeDetail(1L);

        assertTrue(vo.isHideExpected(), "counter 视角且未提交时应隐藏期望值");
        assertNull(vo.getItems().get(0).getExpectedQuantity(), "expectedQuantity 应置 null");
        assertNull(vo.getItems().get(0).getVarianceQuantity(), "varianceQuantity 应置 null");
        assertNull(vo.getItems().get(0).getVarianceAmount(), "varianceAmount 应置 null");
        assertNull(vo.getItems().get(0).getUnitCost(), "unitCost 应置 null");
        assertEquals(48, vo.getItems().get(0).getActualQuantity(), "actualQuantity 应保留");
    }

    @Test
    void detail_submittedTaskShowsExpectedValues() {
        // 任务已 SUBMITTED，counter 视角也应显示期望值
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED");
        header.setCounterUserId(2L);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = new FinStocktakeItem();
        item.setItemId(11L);
        item.setStocktakeId(1L);
        item.setTenantId(T1);
        item.setDeptId(DEPT_10);
        item.setProductId(PRODUCT_100);
        item.setExpectedQuantity(50);
        item.setActualQuantity(48);
        item.setVarianceQuantity(-2);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeDetailVO vo = service.getStocktakeDetail(1L);

        assertFalse(vo.isHideExpected(), "已提交任务不应隐藏期望值");
        assertEquals(50, vo.getItems().get(0).getExpectedQuantity());
        assertEquals(-2, vo.getItems().get(0).getVarianceQuantity());
    }

    @Test
    void detail_unauthorizedDept_throws() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_20, "PD-001", "DRAFT"));

        assertThrows(ServiceException.class, () -> service.getStocktakeDetail(1L));
    }

    // ===== 分配 =====

    @Test
    void assign_draftStatusSucceeds() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT");
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        StocktakeAssignRequest req = new StocktakeAssignRequest();
        req.setCounterUserId(COUNTER_USER);
        req.setRecountUserId(3002L);
        req.setVersion(0);

        int affected = service.assignCounter(1L, req);
        assertEquals(1, affected);
        assertEquals(COUNTER_USER, header.getCounterUserId());
        assertEquals(3002L, header.getRecountUserId());
        assertEquals(1, header.getVersion(), "version 应递增");
    }

    @Test
    void assign_nonDraftStatusThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        stocktakeMapper.headers.add(header);

        StocktakeAssignRequest req = new StocktakeAssignRequest();
        req.setCounterUserId(COUNTER_USER);
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.assignCounter(1L, req));
    }

    @Test
    void assign_versionMismatchThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT");
        header.setVersion(2); // 数据库已更新到 v2
        stocktakeMapper.headers.add(header);

        StocktakeAssignRequest req = new StocktakeAssignRequest();
        req.setCounterUserId(COUNTER_USER);
        req.setVersion(0); // 客户端持有的旧版本

        assertThrows(ServiceException.class, () -> service.assignCounter(1L, req));
    }

    @Test
    void assign_unauthorizedDeptThrows() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_20, "PD-001", "DRAFT"));

        StocktakeAssignRequest req = new StocktakeAssignRequest();
        req.setCounterUserId(COUNTER_USER);
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.assignCounter(1L, req));
    }

    // ===== 辅助方法 =====

    private FinProduct buildProduct(Long id, String name) {
        FinProduct p = new FinProduct();
        p.setProductId(id);
        p.setProductName(name);
        return p;
    }

    private FinStocktake buildHeader(Long tenantId, Long id, Long deptId, String takeNo, String status) {
        FinStocktake h = new FinStocktake();
        h.setTenantId(tenantId);
        h.setStocktakeId(id);
        h.setDeptId(deptId);
        h.setTakeNo(takeNo);
        h.setStatus(status);
        h.setVersion(0);
        return h;
    }

    // ===== Fake Mapper =====

    static class FakeFinStocktakeMapper implements FinStocktakeMapper {
        final List<FinStocktake> insertedHeaders = new ArrayList<>();
        final List<FinStocktakeItem> insertedItems = new ArrayList<>();
        final List<FinStocktakeHistory> insertedHistories = new ArrayList<>();
        final List<FinStocktake> headers = new ArrayList<>();
        final Map<Long, List<FinStocktakeItem>> itemsByStocktake = new HashMap<>();
        final List<String> existingTakeNos = new ArrayList<>();
        long nextId = 1L;

        @Override
        public int insertStocktake(FinStocktake stocktake) {
            stocktake.setStocktakeId(nextId++);
            insertedHeaders.add(stocktake);
            headers.add(stocktake);
            return 1;
        }

        @Override
        public FinStocktake selectStocktakeById(Long tenantId, Long stocktakeId) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()) && stocktakeId.equals(h.getStocktakeId()))
                    .findFirst().orElse(null);
        }

        @Override
        public FinStocktake selectStocktakeForUpdate(Long tenantId, Long stocktakeId) {
            return selectStocktakeById(tenantId, stocktakeId);
        }

        @Override
        public List<FinStocktake> listStocktakes(Long tenantId, List<Long> deptIds, String status, Long counterUserId) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()))
                    .filter(h -> deptIds == null || deptIds.isEmpty() || deptIds.contains(h.getDeptId()))
                    .filter(h -> status == null || status.equals(h.getStatus()))
                    .filter(h -> counterUserId == null || counterUserId.equals(h.getCounterUserId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public int countByTakeNo(Long tenantId, String takeNo) {
            if (existingTakeNos.contains(tenantId + ":" + takeNo)) return 1;
            return insertedHeaders.stream()
                    .anyMatch(h -> tenantId.equals(h.getTenantId()) && takeNo.equals(h.getTakeNo())) ? 1 : 0;
        }

        @Override
        public int updateStocktakeStatus(Long tenantId, Long stocktakeId, String fromStatus, String toStatus,
                                          Integer version, String updateBy, String submittedBy, String approvedBy,
                                          String postedBy, String reversedBy, String reversalReason) {
            return 0;
        }

        @Override
        public int assignCounter(Long tenantId, Long stocktakeId, Long counterUserId, Long recountUserId,
                                  Integer version, String updateBy) {
            FinStocktake h = selectStocktakeById(tenantId, stocktakeId);
            if (h == null || !"DRAFT".equals(h.getStatus()) || !version.equals(h.getVersion())) {
                return 0;
            }
            h.setCounterUserId(counterUserId);
            h.setRecountUserId(recountUserId);
            h.setVersion(h.getVersion() + 1);
            return 1;
        }

        @Override
        public int insertStocktakeItem(FinStocktakeItem item) {
            item.setItemId(nextId++);
            insertedItems.add(item);
            itemsByStocktake.computeIfAbsent(item.getStocktakeId(), k -> new ArrayList<>()).add(item);
            return 1;
        }

        @Override
        public FinStocktakeItem selectStocktakeItemById(Long tenantId, Long itemId) {
            return insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
        }

        @Override
        public List<FinStocktakeItem> listStocktakeItems(Long tenantId, Long stocktakeId) {
            return itemsByStocktake.getOrDefault(stocktakeId, new ArrayList<>()).stream()
                    .filter(i -> tenantId.equals(i.getTenantId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<FinStocktakeItem> selectStocktakeItemsForUpdate(Long tenantId, Long stocktakeId) {
            return listStocktakeItems(tenantId, stocktakeId);
        }

        @Override
        public int updateStocktakeItemCount(Long tenantId, Long itemId, Integer actualQuantity, String reasonCode,
                                            String reason, String attachments, String countIdempotencyKey,
                                            String countedBy, Integer version) { return 0; }

        @Override
        public int updateStocktakeItemRecount(Long tenantId, Long itemId, Integer recountQuantity,
                                               String recountedBy, Integer version) { return 0; }

        @Override
        public int updateStocktakeItemFinal(Long tenantId, Long itemId, Integer finalQuantity, Integer varianceQuantity,
                                            java.math.BigDecimal unitCost, java.math.BigDecimal varianceAmount,
                                            String reasonCode, String reason, Integer movementQuantityAfterFreeze,
                                            Integer adjustedExpectedQuantity, Integer version) { return 0; }

        @Override
        public int updateStocktakeItemPostingRefs(Long tenantId, Long itemId, Long stockLedgerId,
                                                   Long costLedgerId, Integer version) { return 0; }

        @Override
        public int updateStocktakeItemReverseRefs(Long tenantId, Long itemId, Long reverseStockLedgerId,
                                                   Long reverseCostLedgerId, Integer version) { return 0; }

        @Override
        public int countByCountIdempotencyKey(Long tenantId, String countIdempotencyKey) { return 0; }

        @Override
        public int insertStocktakeHistory(FinStocktakeHistory history) {
            history.setHistoryId(nextId++);
            insertedHistories.add(history);
            return 1;
        }

        @Override
        public List<FinStocktakeHistory> listStocktakeHistory(Long tenantId, Long stocktakeId) {
            return insertedHistories.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()) && stocktakeId.equals(h.getStocktakeId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public Integer sumMovementAfterFreeze(Long tenantId, Long deptId, Long productId, java.util.Date freezeTime) {
            return 0;
        }
    }

    static class FakeFinStockLedgerMapper implements FinStockLedgerMapper {
        final Map<String, Integer> positions = new HashMap<>();

        @Override public int insertPositionIfAbsent(Long t, Long d, Long p) { return 0; }
        @Override public Integer selectPositionQuantityForUpdate(Long t, Long d, Long p) { return positions.get(t+":"+d+":"+p); }
        @Override public Integer selectPositionQuantity(Long t, Long d, Long p) { return positions.get(t+":"+d+":"+p); }
        @Override public int updatePositionQuantity(Long t, Long d, Long p, Integer q) { return 0; }
        @Override public Integer sumRecordedNet(Long t, String rt, Long ri, Long p) { return 0; }
        @Override public List<Long> selectRecordedProductIds(Long t, String rt, Long ri) { return new ArrayList<>(); }
        @Override public int insertFinStockLedger(com.junsong.finance.domain.FinStockLedger l) { return 0; }
        @Override public com.junsong.finance.domain.vo.DailyFlowView sumDailyFlow(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public List<Long> selectSnapshotProductIds(Long t, java.time.LocalDate d, Long dept) { return new ArrayList<>(); }
        @Override public com.junsong.finance.domain.FinStockSnapshot selectPreviousSnapshot(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public com.junsong.finance.domain.FinStockLedger selectFirstDailyLedger(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public com.junsong.finance.domain.FinStockLedger selectLastLedgerBeforeDate(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public List<com.junsong.finance.domain.vo.FinStockPositionView> selectAllTenantDeptScopesWithPosition() { return new ArrayList<>(); }
        @Override public int upsertSnapshot(com.junsong.finance.domain.FinStockSnapshot s) { return 0; }
        @Override public java.math.BigDecimal selectSaleOutUnitCost(Long t, Long ri, Long p) { return null; }
        @Override public int updateLedgerUnitCost(Long id, java.math.BigDecimal uc) { return 0; }
        @Override public int countByReferenceNo(Long t, String rn) { return 0; }
    }

    static class FakeFinProductMapper implements FinProductMapper {
        final Map<Long, FinProduct> products = new HashMap<>();

        @Override
        public FinProduct selectFinProductByProductIdAndDeptId(Long productId, Long deptId) {
            return products.get(productId);
        }

        // 以下为接口其他方法桩实现
        @Override public List<FinProduct> selectFinProductList(FinProduct p) { return new ArrayList<>(); }
        @Override public FinProduct selectFinProductByProductId(Long productId) { return products.get(productId); }
        @Override public int insertFinProduct(FinProduct p) { return 0; }
        @Override public int updateFinProduct(FinProduct p) { return 0; }
        @Override public int deleteFinProductByProductId(Long productId) { return 0; }
        @Override public int deleteFinProductByProductIds(Long[] ids) { return 0; }
        @Override public FinProduct checkProductCodeUnique(String productCode) { return null; }
        @Override public int updateFinProductByDeptId(FinProduct p, Long deptId) { return 0; }
        @Override public int deleteFinProductByProductIdsAndDeptId(Long[] ids, Long deptId) { return 0; }
    }

    static class FakeRemoteUserService implements RemoteUserService {
        List<Long> authorizedDepts = new ArrayList<>();

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> depts = new ArrayList<>();
            for (Long deptId : authorizedDepts) {
                SysDept d = new SysDept();
                d.setDeptId(deptId);
                depts.add(d);
            }
            return R.ok(depts);
        }

        // 以下为接口其他方法桩实现
        @Override public R<LoginUser> getUserInfo(String username, String source) { return R.fail("not implemented"); }
        @Override public R<LoginUser> getUserInfoById(Long userId, String source) { return R.fail("not implemented"); }
        @Override public R<Boolean> registerUserInfo(SysUser user, String source) { return R.ok(false); }
        @Override public R<Boolean> recordUserLogin(SysUser user, String source) { return R.ok(false); }
        @Override public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.ok(new ArrayList<>()); }
        @Override public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) { return R.ok(false); }
    }
}
