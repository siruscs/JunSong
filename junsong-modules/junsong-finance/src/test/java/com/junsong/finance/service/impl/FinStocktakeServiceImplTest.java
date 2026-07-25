package com.junsong.finance.service.impl;

import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.FinStocktakeHistory;
import com.junsong.finance.domain.FinStocktakeItem;
import com.junsong.finance.domain.vo.StocktakeApprovalRequest;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCountRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;
import com.junsong.finance.domain.vo.StocktakeRecountRequest;
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

    // ===== 启动盘点 (Task 4) =====

    @Test
    void start_draftWithCounterSucceeds() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT");
        header.setCounterUserId(COUNTER_USER);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        int affected = service.startStocktake(1L, 0);
        assertEquals(1, affected);
        assertEquals("COUNTING", header.getStatus());
        assertEquals(1, header.getVersion(), "version 应递增");
        assertEquals("START", stocktakeMapper.insertedHistories.get(0).getAction());
    }

    @Test
    void start_nonDraftStatusThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(COUNTER_USER);
        stocktakeMapper.headers.add(header);

        assertThrows(ServiceException.class, () -> service.startStocktake(1L, 0));
    }

    @Test
    void start_versionMismatchThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT");
        header.setCounterUserId(COUNTER_USER);
        header.setVersion(2);
        stocktakeMapper.headers.add(header);

        assertThrows(ServiceException.class, () -> service.startStocktake(1L, 0));
    }

    @Test
    void start_missingCounterThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT");
        header.setCounterUserId(null); // 未分配盘点人
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        assertThrows(ServiceException.class, () -> service.startStocktake(1L, 0));
    }

    @Test
    void start_unauthorizedDeptThrows() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_20, "PD-001", "DRAFT"));

        assertThrows(ServiceException.class, () -> service.startStocktake(1L, 0));
    }

    // ===== 行录入 (Task 4) =====

    private FinStocktakeItem buildCountingItem(Long itemId, Long stocktakeId, Long productId, int expectedQty) {
        FinStocktakeItem item = new FinStocktakeItem();
        item.setItemId(itemId);
        item.setStocktakeId(stocktakeId);
        item.setTenantId(T1);
        item.setDeptId(DEPT_10);
        item.setProductId(productId);
        item.setExpectedQuantity(expectedQty);
        item.setVersion(0);
        stocktakeMapper.insertedItems.add(item);
        return item;
    }

    private StocktakeCountRequest buildCountRequest(int actualQty, String idempotencyKey, Integer version) {
        StocktakeCountRequest req = new StocktakeCountRequest();
        req.setActualQuantity(actualQty);
        req.setIdempotencyKey(idempotencyKey);
        req.setVersion(version);
        return req;
    }

    @Test
    void count_countingStatusSucceeds() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L); // admin 是用户 1
        stocktakeMapper.headers.add(header);
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req = buildCountRequest(48, "PD-001-100-count-1", 0);
        req.setReasonCode("DAMAGED");
        req.setReason("外包装破损");

        int affected = service.countItem(1L, 11L, req);
        assertEquals(1, affected);
        FinStocktakeItem item = stocktakeMapper.insertedItems.get(0);
        assertEquals(48, item.getActualQuantity());
        assertEquals("DAMAGED", item.getReasonCode());
        assertEquals("PD-001-100-count-1", item.getCountIdempotencyKey());
        assertEquals(1, item.getVersion(), "行版本应递增");
    }

    @Test
    void count_nonCountingStatusThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req = buildCountRequest(48, "PD-001-100-count-1", 0);
        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_nonCounterUserThrows() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(999L); // 不是当前用户
        stocktakeMapper.headers.add(header);
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req = buildCountRequest(48, "PD-001-100-count-1", 0);
        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_negativeQuantityThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req = buildCountRequest(-1, "PD-001-100-count-1", 0);
        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_missingIdempotencyKeyThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req = new StocktakeCountRequest();
        req.setActualQuantity(48);
        req.setVersion(0);
        // idempotencyKey 未设置
        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_nonZeroVarianceWithoutReasonCodeThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50); // expected=50

        StocktakeCountRequest req = buildCountRequest(48, "PD-001-100-count-1", 0);
        req.setReason("外包装破损");
        // reasonCode 未设置
        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_nonZeroVarianceWithoutReasonThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req = buildCountRequest(48, "PD-001-100-count-1", 0);
        req.setReasonCode("DAMAGED");
        // reason 未设置
        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_zeroVariancePersistsWithoutReason() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50); // expected=50

        // actualQuantity=50，方差为 0，无需 reasonCode/reason
        StocktakeCountRequest req = buildCountRequest(50, "PD-001-100-count-1", 0);

        int affected = service.countItem(1L, 11L, req);
        assertEquals(1, affected, "零方差应成功持久化");
        assertEquals(50, stocktakeMapper.insertedItems.get(0).getActualQuantity());
    }

    @Test
    void count_idempotentReplayReturnsSame() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        StocktakeCountRequest req1 = buildCountRequest(48, "PD-001-100-count-1", 0);
        req1.setReasonCode("DAMAGED");
        req1.setReason("外包装破损");

        int first = service.countItem(1L, 11L, req1);
        assertEquals(1, first);

        // 相同幂等键、相同负载再次请求 —— 应返回 1（幂等重放），不重复更新
        StocktakeCountRequest req2 = buildCountRequest(48, "PD-001-100-count-1", 0);
        req2.setReasonCode("DAMAGED");
        req2.setReason("外包装破损");

        int second = service.countItem(1L, 11L, req2);
        assertEquals(1, second, "幂等重放应返回成功");
        // version 不应再次递增（第一次更新后 version=1，第二次是幂等重放不再更新）
        assertEquals(1, stocktakeMapper.insertedItems.get(0).getVersion(), "幂等重放不应再次递增版本");
    }

    @Test
    void count_idempotentKeyWithDifferentPayloadThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        // 第一次录入 actualQuantity=48
        StocktakeCountRequest req1 = buildCountRequest(48, "PD-001-100-count-1", 0);
        req1.setReasonCode("DAMAGED");
        req1.setReason("外包装破损");
        service.countItem(1L, 11L, req1);

        // 相同幂等键但 actualQuantity 不同（负载不同）—— 应拒绝
        StocktakeCountRequest req2 = buildCountRequest(45, "PD-001-100-count-1", 0);
        req2.setReasonCode("DAMAGED");
        req2.setReason("外包装破损");

        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req2));
    }

    @Test
    void count_versionConflictThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));
        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        // item.version=0，但请求传 version=3
        StocktakeCountRequest req = buildCountRequest(48, "PD-001-100-count-1", 3);
        req.setReasonCode("DAMAGED");
        req.setReason("外包装破损");

        assertThrows(ServiceException.class, () -> service.countItem(1L, 11L, req));
    }

    @Test
    void count_idempotentKeyOccupiedByOtherItemThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));

        // item1 已用幂等键 "SHARED-KEY"
        FinStocktakeItem item1 = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item1.setCountIdempotencyKey("SHARED-KEY");
        item1.setActualQuantity(48);

        // item2 尝试用相同幂等键
        buildCountingItem(12L, 1L, PRODUCT_200, 30);

        StocktakeCountRequest req = buildCountRequest(25, "SHARED-KEY", 0);
        req.setReasonCode("DAMAGED");
        req.setReason("测试");

        assertThrows(ServiceException.class, () -> service.countItem(1L, 12L, req),
                "幂等键被其他行占用时应拒绝");
    }

    // ===== 提交 (Task 5) =====

    @Test
    void submit_countingStatusWithAllItemsCountedGoesSubmitted() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        // 一行 actualQuantity=50, expected=50, variance=0（低于阈值）
        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(50);
        item.setVersion(1); // count 后版本递增
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        int affected = service.submitStocktake(1L, 0);
        assertEquals(1, affected);
        assertEquals("SUBMITTED", header.getStatus(), "方差为 0 应流转至 SUBMITTED");
        assertEquals(1, header.getVersion(), "version 应递增");
        assertNotNull(header.getSubmittedBy());
        assertNotNull(header.getSubmittedTime());

        // 行表应已计算临时方差
        assertEquals(0, item.getVarianceQuantity(), "方差应为 0");
        assertEquals(50, item.getAdjustedExpectedQuantity(), "adjustedExpected 应等于 expected + movement");
        // 历史
        assertEquals("SUBMIT", stocktakeMapper.insertedHistories.get(stocktakeMapper.insertedHistories.size() - 1).getAction());
    }

    @Test
    void submit_withUncountedItemsThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        // 行未录入 actualQuantity
        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(null);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        assertThrows(ServiceException.class, () -> service.submitStocktake(1L, 0),
                "存在未录入行时应拒绝提交");
        assertEquals("COUNTING", header.getStatus(), "失败时状态不应改变");
    }

    @Test
    void submit_withVarianceAboveQuantityThresholdGoesRecounting() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L); // 已分配复盘人
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        // actual=40, expected=50, variance=-10，|variance|>5 阈值
        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setReasonCode("DAMAGED");
        item.setReason("破损");
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        int affected = service.submitStocktake(1L, 0);
        assertEquals(1, affected);
        assertEquals("RECOUNTING", header.getStatus(), "方差超过阈值且已分配复盘人时应流转至 RECOUNTING");
        assertEquals(-10, item.getVarianceQuantity());
    }

    @Test
    void submit_withVarianceAboveThresholdButNoRecountUserGoesSubmitted() {
        // 方差超过阈值但未分配复盘人，无法触发复盘，只能 SUBMITTED（由审批人决定是否驳回重盘）
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(null); // 未分配复盘人
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setReasonCode("DAMAGED");
        item.setReason("破损");
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        int affected = service.submitStocktake(1L, 0);
        assertEquals(1, affected);
        assertEquals("SUBMITTED", header.getStatus(), "未分配复盘人时即使方差超阈也走 SUBMITTED");
    }

    @Test
    void submit_nonCountingStatusThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "DRAFT"));
        assertThrows(ServiceException.class, () -> service.submitStocktake(1L, 0));
    }

    @Test
    void submit_versionMismatchThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L);
        header.setVersion(2); // 数据库已更新
        stocktakeMapper.headers.add(header);

        assertThrows(ServiceException.class, () -> service.submitStocktake(1L, 0));
    }

    @Test
    void submit_unauthorizedDeptThrows() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_20, "PD-001", "COUNTING"));

        assertThrows(ServiceException.class, () -> service.submitStocktake(1L, 0));
    }

    // ===== 复盘 (Task 5) =====

    @Test
    void recount_recountingStatusSucceeds() {
        SecurityContextHolder.setUserId("3002"); // 复盘人
        SecurityContextHolder.setUserName("recounter");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L);
        header.setVersion(1);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setAdjustedExpectedQuantity(50);
        item.setVarianceQuantity(-10);
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeRecountRequest req = new StocktakeRecountRequest();
        req.setRecountQuantity(48);
        req.setReasonCode("DAMAGED");
        req.setReason("复盘确认 48 件");
        req.setIdempotencyKey("PD-001-100-recount-1");
        req.setVersion(1);

        int affected = service.recountItem(1L, 11L, req);
        assertEquals(1, affected);
        assertEquals(48, item.getRecountQuantity());
        assertEquals("recounter", item.getRecountedBy());
        assertEquals(2, item.getVersion(), "行版本应递增");
    }

    @Test
    void recount_sameCounterUserThrows() {
        // 复盘人 = 盘点人，禁止
        SecurityContextHolder.setUserId("1"); // admin
        SecurityContextHolder.setUserName("admin");

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(1L); // 同一人（不应发生，但服务端要防御）
        header.setVersion(1);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeRecountRequest req = new StocktakeRecountRequest();
        req.setRecountQuantity(48);
        req.setIdempotencyKey("PD-001-100-recount-1");
        req.setVersion(1);

        assertThrows(ServiceException.class, () -> service.recountItem(1L, 11L, req),
                "复盘人与盘点人相同时应拒绝");
    }

    @Test
    void recount_nonRecountingStatusThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED"));

        StocktakeRecountRequest req = new StocktakeRecountRequest();
        req.setRecountQuantity(48);
        req.setIdempotencyKey("PD-001-100-recount-1");
        req.setVersion(1);

        assertThrows(ServiceException.class, () -> service.recountItem(1L, 11L, req));
    }

    @Test
    void recount_unauthorizedUserThrows() {
        SecurityContextHolder.setUserId("9999"); // 非复盘人
        SecurityContextHolder.setUserName("stranger");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L);
        header.setVersion(1);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeRecountRequest req = new StocktakeRecountRequest();
        req.setRecountQuantity(48);
        req.setIdempotencyKey("PD-001-100-recount-1");
        req.setVersion(1);

        assertThrows(ServiceException.class, () -> service.recountItem(1L, 11L, req),
                "非分配的复盘人不应录入复盘");
    }

    @Test
    void recount_versionMismatchThrows() {
        SecurityContextHolder.setUserId("3002");
        SecurityContextHolder.setUserName("recounter");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L);
        header.setVersion(1);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setVersion(5); // 数据库已更新
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeRecountRequest req = new StocktakeRecountRequest();
        req.setRecountQuantity(48);
        req.setIdempotencyKey("PD-001-100-recount-1");
        req.setVersion(1); // 客户端旧版本

        assertThrows(ServiceException.class, () -> service.recountItem(1L, 11L, req));
    }

    @Test
    void recount_idempotentReplayReturnsSame() {
        SecurityContextHolder.setUserId("3002");
        SecurityContextHolder.setUserName("recounter");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L);
        header.setVersion(1);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeRecountRequest req1 = new StocktakeRecountRequest();
        req1.setRecountQuantity(48);
        req1.setReasonCode("DAMAGED");
        req1.setReason("复盘 48");
        req1.setIdempotencyKey("PD-001-100-recount-1");
        req1.setVersion(1);

        int first = service.recountItem(1L, 11L, req1);
        assertEquals(1, first);

        // 相同幂等键、相同负载再次请求 —— 应返回 1，不重复更新
        StocktakeRecountRequest req2 = new StocktakeRecountRequest();
        req2.setRecountQuantity(48);
        req2.setReasonCode("DAMAGED");
        req2.setReason("复盘 48");
        req2.setIdempotencyKey("PD-001-100-recount-1");
        req2.setVersion(1);

        int second = service.recountItem(1L, 11L, req2);
        assertEquals(1, second, "幂等重放应返回成功");
    }

    // ===== 审批 (Task 5) =====

    @Test
    void approve_submittedStatusWithApproveGoesApproved() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED");
        header.setCounterUserId(1L);
        header.setVersion(2);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(48);
        item.setAdjustedExpectedQuantity(50);
        item.setVarianceQuantity(-2);
        item.setReasonCode("DAMAGED");
        item.setVersion(2);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("APPROVE");
        req.setComment("复盘一致，同意过账");
        req.setVersion(2);

        int affected = service.approveStocktake(1L, req);
        assertEquals(1, affected);
        assertEquals("APPROVED", header.getStatus());
        assertEquals(48, item.getFinalQuantity(), "finalQuantity 应使用 actualQuantity");
        assertNotNull(header.getApprovedBy());
        assertNotNull(header.getApprovedTime());
    }

    @Test
    void approve_recountingStatusWithApproveUsesRecountQuantity() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(3002L);
        header.setVersion(3);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setRecountQuantity(48);
        item.setAdjustedExpectedQuantity(50);
        item.setVarianceQuantity(-2); // 重新计算后方差
        item.setVersion(3);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("APPROVE");
        req.setComment("复盘 48 件，同意过账");
        req.setVersion(3);

        int affected = service.approveStocktake(1L, req);
        assertEquals(1, affected);
        assertEquals("APPROVED", header.getStatus());
        assertEquals(48, item.getFinalQuantity(), "finalQuantity 应优先使用 recountQuantity");
        assertEquals(-2, item.getVarianceQuantity(), "variance 应基于 recountQuantity 重算");
    }

    @Test
    void approve_withRejectGoesCounting() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED");
        header.setCounterUserId(1L);
        header.setVersion(2);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(48);
        item.setVersion(2);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("REJECT");
        req.setComment("盘点不规范，请重盘");
        req.setVersion(2);

        int affected = service.approveStocktake(1L, req);
        assertEquals(1, affected);
        assertEquals("COUNTING", header.getStatus(), "REJECT 应回到 COUNTING 重盘");
    }

    @Test
    void approve_invalidDecisionThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED"));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("UNKNOWN");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.approveStocktake(1L, req));
    }

    @Test
    void approve_nonSubmittedStatusThrows() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING"));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("APPROVE");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.approveStocktake(1L, req));
    }

    @Test
    void approve_versionMismatchThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED");
        header.setVersion(5);
        stocktakeMapper.headers.add(header);

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("APPROVE");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.approveStocktake(1L, req));
    }

    @Test
    void approve_unauthorizedDeptThrows() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_20, "PD-001", "SUBMITTED"));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("APPROVE");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.approveStocktake(1L, req));
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
            FinStocktake h = selectStocktakeById(tenantId, stocktakeId);
            if (h == null || !fromStatus.equals(h.getStatus()) || !version.equals(h.getVersion())) {
                return 0;
            }
            h.setStatus(toStatus);
            h.setVersion(h.getVersion() + 1);
            java.util.Date now = new java.util.Date();
            if (submittedBy != null) { h.setSubmittedBy(submittedBy); h.setSubmittedTime(now); }
            if (approvedBy != null) { h.setApprovedBy(approvedBy); h.setApprovedTime(now); }
            if (postedBy != null) { h.setPostedBy(postedBy); h.setPostedTime(now); }
            if (reversedBy != null) { h.setReversedBy(reversedBy); h.setReversedTime(now); }
            if (reversalReason != null) h.setReversalReason(reversalReason);
            return 1;
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
                                            String countedBy, Integer version) {
            FinStocktakeItem item = insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
            if (item == null || !version.equals(item.getVersion())) {
                return 0;
            }
            item.setActualQuantity(actualQuantity);
            item.setReasonCode(reasonCode);
            item.setReason(reason);
            item.setAttachments(attachments);
            item.setCountIdempotencyKey(countIdempotencyKey);
            item.setCountedBy(countedBy);
            item.setCountedTime(new java.util.Date());
            item.setVersion(item.getVersion() + 1);
            return 1;
        }

        @Override
        public int updateStocktakeItemRecount(Long tenantId, Long itemId, Integer recountQuantity,
                                               String reasonCode, String reason,
                                               String recountIdempotencyKey,
                                               String recountedBy, Integer version) {
            FinStocktakeItem item = insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
            if (item == null || !version.equals(item.getVersion())) {
                return 0;
            }
            item.setRecountQuantity(recountQuantity);
            item.setReasonCode(reasonCode);
            item.setReason(reason);
            item.setCountIdempotencyKey(recountIdempotencyKey);
            item.setRecountedBy(recountedBy);
            item.setRecountedTime(new java.util.Date());
            item.setVersion(item.getVersion() + 1);
            return 1;
        }

        @Override
        public int updateStocktakeItemFinal(Long tenantId, Long itemId, Integer finalQuantity, Integer varianceQuantity,
                                            java.math.BigDecimal unitCost, java.math.BigDecimal varianceAmount,
                                            String reasonCode, String reason, Integer movementQuantityAfterFreeze,
                                            Integer adjustedExpectedQuantity, Integer version) {
            FinStocktakeItem item = insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
            if (item == null || !version.equals(item.getVersion())) {
                return 0;
            }
            if (finalQuantity != null) item.setFinalQuantity(finalQuantity);
            item.setVarianceQuantity(varianceQuantity);
            if (unitCost != null) item.setUnitCost(unitCost);
            if (varianceAmount != null) item.setVarianceAmount(varianceAmount);
            if (reasonCode != null) item.setReasonCode(reasonCode);
            if (reason != null) item.setReason(reason);
            if (movementQuantityAfterFreeze != null) item.setMovementQuantityAfterFreeze(movementQuantityAfterFreeze);
            if (adjustedExpectedQuantity != null) item.setAdjustedExpectedQuantity(adjustedExpectedQuantity);
            item.setVersion(item.getVersion() + 1);
            return 1;
        }

        @Override
        public int updateStocktakeItemPostingRefs(Long tenantId, Long itemId, Long stockLedgerId,
                                                   Long costLedgerId, Integer version) { return 0; }

        @Override
        public int updateStocktakeItemReverseRefs(Long tenantId, Long itemId, Long reverseStockLedgerId,
                                                   Long reverseCostLedgerId, Integer version) { return 0; }

        @Override
        public int countByCountIdempotencyKey(Long tenantId, String countIdempotencyKey) {
            return (int) insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId())
                            && countIdempotencyKey != null
                            && countIdempotencyKey.equals(i.getCountIdempotencyKey()))
                    .count();
        }

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
