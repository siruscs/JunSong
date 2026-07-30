package com.junsong.finance.service.impl;

import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStockLedger;
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
import com.junsong.finance.domain.vo.StocktakeReverseRequest;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.mapper.FinStocktakeMapper;
import com.junsong.finance.service.IStockCostService;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    private FakeIStockCostService stockCostService;
    private FakeFinAccountingPeriodMapper accountingPeriodMapper;
    private FinStocktakeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        stocktakeMapper = new FakeFinStocktakeMapper();
        ledgerMapper = new FakeFinStockLedgerMapper();
        productMapper = new FakeFinProductMapper();
        remoteUserService = new FakeRemoteUserService();
        stockCostService = new FakeIStockCostService();
        accountingPeriodMapper = new FakeFinAccountingPeriodMapper();
        service = new FinStocktakeServiceImpl();

        inject("finStocktakeMapper", stocktakeMapper);
        inject("finStockLedgerMapper", ledgerMapper);
        inject("finProductMapper", productMapper);
        inject("remoteUserService", remoteUserService);
        inject("stockCostService", stockCostService);
        inject("accountingPeriodMapper", accountingPeriodMapper);

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
    void submit_withVarianceAboveThresholdButNoRecountUserThrows() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-001", "COUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(null);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setReasonCode("DAMAGED");
        item.setReason("破损");
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submitStocktake(1L, 0));
        assertTrue(ex.getMessage().contains("必须先分配独立复盘人"),
                "超过阈值且未分配复盘人时应禁止提交");
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

    // ===== Task 6：过账（postStocktake）=====

    @Test
    void post_missingTenant_throws() {
        TenantContext.clear();
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_versionNull_throws() {
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, null));
    }

    @Test
    void post_notFound_throws() {
        assertThrows(ServiceException.class, () -> service.postStocktake(999L, 0));
    }

    @Test
    void post_unauthorizedDept_throws() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_20, "PD-001", "APPROVED"));
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_nonApprovedStatus_throws() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "SUBMITTED"));
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_versionMismatch_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED");
        h.setVersion(5);
        stocktakeMapper.headers.add(h);
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_noActivePeriod_throws() {
        accountingPeriodMapper.currentPeriod = null;
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED"));
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_periodClosed_throws() {
        accountingPeriodMapper.currentPeriod.setStatus("2"); // 已结转
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED"));
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_idempotent_duplicateRejected() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED");
        stocktakeMapper.headers.add(h);
        ledgerMapper.existingReferenceNos.add(T1 + ":PD-001");
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_noItems_throws() {
        stocktakeMapper.headers.add(buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED"));
        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_lossMakesStockNegative_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 期望 5，实际 0（盘亏 5），当前库存 3 → 盘亏后 -2 拒绝
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 5);
        item.setFinalQuantity(0);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 3);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
        assertTrue(ex.getMessage().contains("盘亏后库存为负"));
        // 成本服务未被调用（事务回滚）
        assertTrue(stockCostService.lossCalls.isEmpty());
    }

    @Test
    void post_loss_success_updatesStockAndCost() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-001", "APPROVED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 期望 10，实际 7（盘亏 3），当前库存 10 → 盘亏后 7
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(7);
        item.setReasonCode("DAMAGE");
        item.setReason("破损");
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 10);

        int affected = service.postStocktake(1L, 0);

        assertEquals(1, affected);
        // 状态流转 APPROVED → POSTED
        assertEquals("POSTED", h.getStatus());
        // 库存更新：10 → 7
        assertEquals(7, ledgerMapper.positions.get(T1 + ":" + DEPT_10 + ":" + PRODUCT_100).intValue());
        // 写入一笔库存流水
        assertEquals(1, ledgerMapper.insertedLedgers.size());
        FinStockLedger ledger = ledgerMapper.insertedLedgers.get(0);
        assertEquals("STOCK_TAKE_LOSS", ledger.getChangeType());
        assertEquals(-3, ledger.getChangeQuantity().intValue());
        assertEquals(7, ledger.getAfterQuantity().intValue());
        assertEquals("PD-001", ledger.getReferenceNo());
        // 成本服务被调用一次（盘亏）
        assertEquals(1, stockCostService.lossCalls.size());
        assertTrue(stockCostService.gainCalls.isEmpty());
    }

    @Test
    void post_gain_success_updatesStockAndCost() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-002", "APPROVED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 期望 5，实际 8（盘盈 3），当前库存 5 → 盘盈后 8
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 5);
        item.setFinalQuantity(8);
        item.setReasonCode("FOUND");
        item.setReason("盘盈入库");
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 5);

        int affected = service.postStocktake(1L, 0);

        assertEquals(1, affected);
        assertEquals("POSTED", h.getStatus());
        // 库存更新：5 → 8
        assertEquals(8, ledgerMapper.positions.get(T1 + ":" + DEPT_10 + ":" + PRODUCT_100).intValue());
        // 写入一笔库存流水
        assertEquals(1, ledgerMapper.insertedLedgers.size());
        FinStockLedger ledger = ledgerMapper.insertedLedgers.get(0);
        assertEquals("STOCK_TAKE_GAIN", ledger.getChangeType());
        assertEquals(3, ledger.getChangeQuantity().intValue());
        assertEquals(8, ledger.getAfterQuantity().intValue());
        // 成本服务被调用一次（盘盈）
        assertEquals(1, stockCostService.gainCalls.size());
        assertTrue(stockCostService.lossCalls.isEmpty());
        // 关键：盘盈金额 = 当前平均成本 × 数量（10 × 3 = 30.00），不是 0
        assertTrue(stockCostService.gainCalls.get(0).contains("amount=30.00"),
                "盘盈金额应为 30.00，实际: " + stockCostService.gainCalls.get(0));
    }

    @Test
    void post_zeroVariance_noLedgerWritten() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-003", "APPROVED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 期望 10，实际 10（无差异）
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(10);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 10);

        int affected = service.postStocktake(1L, 0);

        assertEquals(1, affected);
        assertEquals("POSTED", h.getStatus());
        // 无库存流水
        assertTrue(ledgerMapper.insertedLedgers.isEmpty());
        // 无成本调用
        assertTrue(stockCostService.lossCalls.isEmpty());
        assertTrue(stockCostService.gainCalls.isEmpty());
    }

    @Test
    void post_missingFinalQuantity_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-004", "APPROVED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(null); // 缺失最终数量
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        assertThrows(ServiceException.class, () -> service.postStocktake(1L, 0));
    }

    @Test
    void post_historyRecorded() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-005", "APPROVED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(10);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        service.postStocktake(1L, 0);

        // 历史记录包含 POST 动作
        boolean hasPost = stocktakeMapper.insertedHistories.stream()
                .anyMatch(h2 -> "POST".equals(h2.getAction()) && "POSTED".equals(h2.getToStatus()));
        assertTrue(hasPost, "过账应写入 POST 历史");
    }

    // ===== Task 7: 取消 =====

    @Test
    void cancel_draft_success() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C1", "DRAFT");
        stocktakeMapper.headers.add(h);

        int affected = service.cancelStocktake(1L, 0);

        assertEquals(1, affected);
        assertEquals("CANCELLED", h.getStatus());
    }

    @Test
    void cancel_counting_success() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C2", "COUNTING");
        stocktakeMapper.headers.add(h);

        int affected = service.cancelStocktake(1L, 0);

        assertEquals(1, affected);
        assertEquals("CANCELLED", h.getStatus());
    }

    @Test
    void cancel_posted_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C3", "POSTED");
        stocktakeMapper.headers.add(h);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.cancelStocktake(1L, 0));
        assertTrue(ex.getMessage().contains("仅过账前"));
    }

    @Test
    void cancel_cancelled_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C4", "CANCELLED");
        stocktakeMapper.headers.add(h);

        assertThrows(ServiceException.class, () -> service.cancelStocktake(1L, 0));
    }

    @Test
    void cancel_reversed_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C5", "REVERSED");
        stocktakeMapper.headers.add(h);

        assertThrows(ServiceException.class, () -> service.cancelStocktake(1L, 0));
    }

    @Test
    void cancel_versionMismatch_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C6", "DRAFT");
        stocktakeMapper.headers.add(h);

        assertThrows(ServiceException.class, () -> service.cancelStocktake(1L, 99));
    }

    @Test
    void cancel_tenantMissing_throws() {
        // TenantContext.getTenantId() 在 ThreadLocal 为空时返回 DEFAULT_TENANT_ID=1L 而非 null，
        // 因此 null 租户路径无法通过公共 API 触发，租户隔离由 Mapper SQL 的 tenant_id 过滤保证。
        // 改为验证跨租户隔离：T1 的任务在 T2 上下文中不可取消。
        TenantContext.setTenantId(2L);
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C7", "DRAFT");
        stocktakeMapper.headers.add(h);

        assertThrows(ServiceException.class, () -> service.cancelStocktake(1L, 0));
        TenantContext.setTenantId(T1);
    }

    @Test
    void cancel_historyRecorded() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-C8", "DRAFT");
        stocktakeMapper.headers.add(h);

        service.cancelStocktake(1L, 0);

        boolean hasCancel = stocktakeMapper.insertedHistories.stream()
                .anyMatch(h2 -> "CANCEL".equals(h2.getAction()) && "CANCELLED".equals(h2.getToStatus()));
        assertTrue(hasCancel, "取消应写入 CANCEL 历史");
    }

    // ===== Task 7: 整单冲销 =====

    @Test
    void reverse_postedLoss_success_restoresStockAndCost() {
        // 准备一个已过账的盘亏任务
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R1", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 期望 10，实际 7（盘亏 3），过账后库存 7
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(7);
        item.setVarianceQuantity(-3);
        item.setUnitCost(new BigDecimal("10.000000"));
        item.setStockLedgerId(9001L);
        item.setCostLedgerId(7001L);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        // 当前库存 7（盘亏后）
        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 7);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("录入错误，需冲销");
        req.setIdempotencyKey("PD-R1-REVERSE-1");
        req.setVersion(0);

        int affected = service.reverseStocktake(1L, req);

        assertEquals(1, affected);
        assertEquals("REVERSED", h.getStatus());
        // 库存恢复：7 → 10
        assertEquals(10, ledgerMapper.positions.get(T1 + ":" + DEPT_10 + ":" + PRODUCT_100).intValue());
        // 写入一笔冲销库存流水
        assertEquals(1, ledgerMapper.insertedLedgers.size());
        FinStockLedger reverseLedger = ledgerMapper.insertedLedgers.get(0);
        assertEquals("STOCK_TAKE_REVERSE", reverseLedger.getChangeType());
        assertEquals(3, reverseLedger.getChangeQuantity().intValue(), "盘亏冲销应为正数恢复");
        assertEquals(10, reverseLedger.getAfterQuantity().intValue());
        assertEquals("PD-R1:REVERSE", reverseLedger.getReferenceNo());
        // 成本冲销被调用
        assertEquals(1, stockCostService.reverseCalls.size());
        assertTrue(stockCostService.reverseCalls.get(0).contains("qty=3"));
        // 行表冲销引用被设置
        assertNotNull(item.getReverseStockLedgerId());
        assertNotNull(item.getReverseCostLedgerId());
    }

    @Test
    void reverse_postedGain_success_reducesStockAndCost() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R2", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 期望 5，实际 8（盘盈 3），过账后库存 8
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 5);
        item.setFinalQuantity(8);
        item.setVarianceQuantity(3);
        item.setUnitCost(new BigDecimal("10.000000"));
        item.setStockLedgerId(9002L);
        item.setCostLedgerId(7002L);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        // 当前库存 8（盘盈后）
        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 8);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("盘盈数据错误");
        req.setIdempotencyKey("PD-R2-REVERSE-1");
        req.setVersion(0);

        int affected = service.reverseStocktake(1L, req);

        assertEquals(1, affected);
        assertEquals("REVERSED", h.getStatus());
        // 库存扣减：8 → 5
        assertEquals(5, ledgerMapper.positions.get(T1 + ":" + DEPT_10 + ":" + PRODUCT_100).intValue());
        // 冲销流水为负数
        assertEquals(-3, ledgerMapper.insertedLedgers.get(0).getChangeQuantity().intValue());
        // 成本冲销被调用
        assertEquals(1, stockCostService.reverseCalls.size());
    }

    @Test
    void reverse_nonPosted_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R3", "APPROVED");
        stocktakeMapper.headers.add(h);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R3-REVERSE-1");
        req.setVersion(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
        assertTrue(ex.getMessage().contains("仅已过账状态可冲销"));
    }

    @Test
    void reverse_alreadyReversed_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R4", "REVERSED");
        stocktakeMapper.headers.add(h);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("二次冲销");
        req.setIdempotencyKey("PD-R4-REVERSE-2");
        req.setVersion(1);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
        assertTrue(ex.getMessage().contains("已冲销") || ex.getMessage().contains("仅已过账"));
    }

    @Test
    void reverse_missingReason_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R5", "POSTED");
        stocktakeMapper.headers.add(h);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("");
        req.setIdempotencyKey("PD-R5-REVERSE-1");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
    }

    @Test
    void reverse_missingIdempotencyKey_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R6", "POSTED");
        stocktakeMapper.headers.add(h);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
    }

    @Test
    void reverse_versionMismatch_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R7", "POSTED");
        stocktakeMapper.headers.add(h);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R7-REVERSE-1");
        req.setVersion(99);

        assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
    }

    @Test
    void reverse_lockedPeriod_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R8", "POSTED");
        stocktakeMapper.headers.add(h);

        // 期间已结转
        accountingPeriodMapper.currentPeriod.setStatus("2");

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R8-REVERSE-1");
        req.setVersion(0);

        try {
            assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
        } finally {
            accountingPeriodMapper.currentPeriod.setStatus("0");
        }
    }

    @Test
    void reverse_negativeStock_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R9", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 原盘盈 3（冲销需扣减 3），但当前库存只有 1（被销售消耗）
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 5);
        item.setFinalQuantity(8);
        item.setVarianceQuantity(3);
        item.setUnitCost(new BigDecimal("10.000000"));
        item.setStockLedgerId(9003L);
        item.setCostLedgerId(7003L);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        // 当前库存 1，冲销需扣 3 → 负库存拒绝
        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 1);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R9-REVERSE-1");
        req.setVersion(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
        assertTrue(ex.getMessage().contains("冲销后库存为负"));
        // 状态未变
        assertEquals("POSTED", h.getStatus());
    }

    @Test
    void reverse_missingUnitCost_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R10", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(7);
        item.setVarianceQuantity(-3);
        item.setUnitCost(null); // 行表未固化成本
        item.setStockLedgerId(9004L);
        item.setCostLedgerId(7004L);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 7);

        // 让成本服务返回 null（模拟原成本流水丢失）
        stockCostService.avgUnitCost = null;

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R10-REVERSE-1");
        req.setVersion(0);

        try {
            ServiceException ex = assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
            assertTrue(ex.getMessage().contains("缺少原固化成本"));
        } finally {
            stockCostService.avgUnitCost = new BigDecimal("10.000000");
        }
    }

    @Test
    void reverse_historyRecorded() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R11", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(7);
        item.setVarianceQuantity(-3);
        item.setUnitCost(new BigDecimal("10.000000"));
        item.setStockLedgerId(9005L);
        item.setCostLedgerId(7005L);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 7);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("冲销测试");
        req.setIdempotencyKey("PD-R11-REVERSE-1");
        req.setVersion(0);

        service.reverseStocktake(1L, req);

        boolean hasReverse = stocktakeMapper.insertedHistories.stream()
                .anyMatch(h2 -> "REVERSE".equals(h2.getAction()) && "REVERSED".equals(h2.getToStatus()));
        assertTrue(hasReverse, "冲销应写入 REVERSE 历史");
        // 冲销理由被记录
        FinStocktakeHistory revHistory = stocktakeMapper.insertedHistories.stream()
                .filter(h2 -> "REVERSE".equals(h2.getAction()))
                .findFirst().orElse(null);
        assertNotNull(revHistory);
        assertTrue(revHistory.getComment().contains("冲销测试"));
    }

    @Test
    void reverse_originalEvidencePreserved() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R12", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(7);
        item.setVarianceQuantity(-3);
        item.setUnitCost(new BigDecimal("10.000000"));
        item.setStockLedgerId(9006L);
        item.setCostLedgerId(7006L);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 7);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R12-REVERSE-1");
        req.setVersion(0);

        service.reverseStocktake(1L, req);

        // 原始过账引用保留（不删除）
        assertEquals(9006L, item.getStockLedgerId());
        assertEquals(7006L, item.getCostLedgerId());
        // 冲销引用已设置
        assertNotNull(item.getReverseStockLedgerId());
        assertNotNull(item.getReverseCostLedgerId());
        // 原始过账字段未被覆盖
        assertEquals(-3, item.getVarianceQuantity().intValue());
        assertEquals(7, item.getFinalQuantity().intValue());
    }

    @Test
    void reverse_zeroVarianceLine_skipped() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R13", "POSTED");
        h.setFreezeTime(new Date());
        stocktakeMapper.headers.add(h);

        // 无差异行（过账时未写流水）
        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 10);
        item.setFinalQuantity(10);
        item.setVarianceQuantity(0);
        item.setStockLedgerId(null); // 无差异不写流水
        item.setCostLedgerId(null);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 10);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R13-REVERSE-1");
        req.setVersion(0);

        int affected = service.reverseStocktake(1L, req);

        assertEquals(1, affected);
        assertEquals("REVERSED", h.getStatus());
        // 无差异行不写冲销流水
        assertTrue(ledgerMapper.insertedLedgers.isEmpty());
        // 无成本冲销调用
        assertTrue(stockCostService.reverseCalls.isEmpty());
    }

    @Test
    void reverse_tenantMissing_throws() {
        TenantContext.clear();
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R14", "POSTED");
        stocktakeMapper.headers.add(h);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R14-REVERSE-1");
        req.setVersion(0);

        assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
        TenantContext.setTenantId(T1);
    }

    @Test
    void reverse_unauthorizedDept_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_20, "PD-R15", "POSTED");
        stocktakeMapper.headers.add(h);

        // 非 admin，授权部门不含 DEPT_20
        SecurityContextHolder.setUserId("100");
        SecurityContextHolder.setUserName("non-admin");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("test");
        req.setIdempotencyKey("PD-R15-REVERSE-1");
        req.setVersion(0);

        try {
            assertThrows(ServiceException.class, () -> service.reverseStocktake(1L, req));
        } finally {
            // 恢复 admin
            SecurityContextHolder.setUserId("1");
            SecurityContextHolder.setUserName("admin");
        }
    }

    // ===== 复核补充测试 =====

    @Test
    void approve_recountingWithIncompleteRecount_throws() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-APR-01", "RECOUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(2L);
        header.setVersion(2);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(40);
        item.setRecountQuantity(null);
        item.setVersion(2);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        StocktakeApprovalRequest req = new StocktakeApprovalRequest();
        req.setDecision("APPROVE");
        req.setVersion(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.approveStocktake(1L, req));
        assertTrue(ex.getMessage().contains("复盘") || ex.getMessage().contains("未完成"),
                "复盘未完成时应禁止审批，实际错误：" + ex.getMessage());
    }

    @Test
    void post_loss_solidifiesUnitCostAndVarianceAmount() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-POST-01", "APPROVED");
        h.setVersion(0);
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 50);
        item.setFinalQuantity(40);
        item.setVarianceQuantity(-10);
        item.setUnitCost(null);
        item.setVarianceAmount(null);
        item.setVersion(1);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 50);
        stockCostService.avgUnitCost = new BigDecimal("12.50");
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "商品A"));

        int affected = service.postStocktake(1L, 0);

        assertEquals(1, affected);
        assertEquals("POSTED", h.getStatus());
        assertNotNull(item.getUnitCost(), "过账后应固化单位成本");
        assertEquals(0, item.getUnitCost().compareTo(new BigDecimal("12.50")),
                "单位成本应等于移动加权平均成本");
        assertNotNull(item.getVarianceAmount(), "过账后应计算差异金额");
        assertEquals(0, item.getVarianceAmount().compareTo(new BigDecimal("125.00")),
                "差异金额取绝对值 = |差异数量| * 单位成本 = 10 * 12.50 = 125.00，实际：" + item.getVarianceAmount());
    }

    @Test
    void post_gain_solidifiesUnitCostAndVarianceAmount() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-POST-02", "APPROVED");
        h.setVersion(0);
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 40);
        item.setFinalQuantity(50);
        item.setVarianceQuantity(10);
        item.setUnitCost(null);
        item.setVarianceAmount(null);
        item.setVersion(1);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 40);
        stockCostService.avgUnitCost = new BigDecimal("10.00");
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "商品A"));

        int affected = service.postStocktake(1L, 0);

        assertEquals(1, affected);
        assertEquals("POSTED", h.getStatus());
        assertNotNull(item.getUnitCost(), "盘盈过账后也应固化单位成本");
        assertNotNull(item.getVarianceAmount(), "盘盈过账后也应计算差异金额");
    }

    @Test
    void reverse_downstreamLedgerExists_throws() {
        FinStocktake h = buildHeader(T1, 1L, DEPT_10, "PD-R-DS", "POSTED");
        h.setPostedTime(new Date());
        h.setVersion(0);
        stocktakeMapper.headers.add(h);

        FinStocktakeItem item = buildItem(T1, 1L, DEPT_10, PRODUCT_100, 50);
        item.setFinalQuantity(40);
        item.setVarianceQuantity(-10);
        item.setUnitCost(new BigDecimal("10.00"));
        item.setVarianceAmount(new BigDecimal("-100.00"));
        item.setStockLedgerId(100L);
        item.setCostLedgerId(200L);
        item.setVersion(1);
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.computeIfAbsent(1L, k -> new ArrayList<>()).add(item);

        ledgerMapper.positions.put(T1 + ":" + DEPT_10 + ":" + PRODUCT_100, 40);
        stockCostService.avgUnitCost = new BigDecimal("10.00");

        // 设置下游流水存在（> 0）
        ledgerMapper.downstreamCount = 5;

        StocktakeReverseRequest req = new StocktakeReverseRequest();
        req.setReason("冲销测试");
        req.setIdempotencyKey("PD-R-DS-REV-1");
        req.setVersion(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reverseStocktake(1L, req));
        assertTrue(ex.getMessage().contains("下游") || ex.getMessage().contains("已使用"),
                "存在下游流水时应禁止冲销，实际错误：" + ex.getMessage());
    }

    @Test
    void count_adminNotAssignedAsCounter_throws() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-BLIND-01", "COUNTING");
        header.setCounterUserId(999L);
        header.setRecountUserId(null);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        buildCountingItem(11L, 1L, PRODUCT_100, 50);

        // admin 用户，但不是指定盘点人
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");

        StocktakeCountRequest req = buildCountRequest(45, "BLIND-COUNT-001", 0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.countItem(1L, 11L, req));
        assertTrue(ex.getMessage().contains("盘点人") || ex.getMessage().contains("权限"),
                "管理员不是指定盘点人时也应禁止录入，实际错误：" + ex.getMessage());
    }

    @Test
    void count_adminAsAssignedCounter_succeeds() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-BLIND-02", "COUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(null);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 50);
        item.setActualQuantity(null);
        item.setVersion(1);

        // admin 用户，同时也是指定盘点人
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");

        StocktakeCountRequest req = buildCountRequest(45, "BLIND-COUNT-002", 1);
        req.setReasonCode("OTHER");
        req.setReason("正常盘点");

        int affected = service.countItem(1L, 11L, req);
        assertEquals(1, affected);
        assertEquals(45, item.getActualQuantity());
    }

    @Test
    void submit_amountAboveThresholdTriggersRecount() {
        FinStocktake header = buildHeader(T1, 1L, DEPT_10, "PD-AMT-TH", "COUNTING");
        header.setCounterUserId(1L);
        header.setRecountUserId(2L);
        header.setVersion(0);
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = buildCountingItem(11L, 1L, PRODUCT_100, 100);
        item.setActualQuantity(90);
        item.setUnitCost(new BigDecimal("50.00"));
        item.setReasonCode("DAMAGED");
        item.setReason("破损");
        item.setVersion(1);
        stocktakeMapper.itemsByStocktake.put(1L, Arrays.asList(item));

        // 数量差异 10 未超数量阈值（默认 20），但金额差异 10*50=500 超金额阈值（默认 100）
        int affected = service.submitStocktake(1L, 0);
        assertEquals(1, affected);
        assertEquals("RECOUNTING", header.getStatus(),
                "金额超阈值且已分配复盘人时应进入复盘状态");
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
        h.setCounterUserId(1L);
        return h;
    }

    private FinStocktakeItem buildItem(Long tenantId, Long stocktakeId, Long deptId, Long productId, int expectedQty) {
        FinStocktakeItem item = new FinStocktakeItem();
        item.setTenantId(tenantId);
        item.setStocktakeId(stocktakeId);
        item.setItemId((long) (stocktakeMapper.nextId++));
        item.setDeptId(deptId);
        item.setProductId(productId);
        item.setProductName("product-" + productId);
        item.setExpectedQuantity(expectedQty);
        item.setMovementQuantityAfterFreeze(0);
        item.setVersion(0);
        return item;
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
        public FinStocktake selectByReverseIdempotencyKey(Long tenantId, String reverseIdempotencyKey) {
            return insertedHeaders.stream()
                    .filter(h -> tenantId.equals(h.getTenantId())
                            && reverseIdempotencyKey != null
                            && reverseIdempotencyKey.equals(h.getReverseIdempotencyKey()))
                    .findFirst().orElse(null);
        }

        @Override
        public int updateStocktakeStatus(Long tenantId, Long stocktakeId, String fromStatus, String toStatus,
                                          Integer version, String updateBy, String submittedBy, String approvedBy,
                                          String postedBy, String reversedBy, String reversalReason,
                                          String reverseIdempotencyKey) {
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
            if (reverseIdempotencyKey != null) h.setReverseIdempotencyKey(reverseIdempotencyKey);
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
                                                   Long costLedgerId, Integer version) {
            FinStocktakeItem item = insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
            if (item == null || !version.equals(item.getVersion())) {
                return 0;
            }
            item.setStockLedgerId(stockLedgerId);
            item.setCostLedgerId(costLedgerId);
            // version 不变（postingRefs 在 updateStocktakeItemFinal 之后调用，version 已 +1）
            return 1;
        }

        @Override
        public int updateStocktakeItemReverseRefs(Long tenantId, Long itemId, Long reverseStockLedgerId,
                                                   Long reverseCostLedgerId, Integer version) {
            FinStocktakeItem item = insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
            if (item == null || !version.equals(item.getVersion())) {
                return 0;
            }
            item.setReverseStockLedgerId(reverseStockLedgerId);
            item.setReverseCostLedgerId(reverseCostLedgerId);
            return 1;
        }

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
        public int updateWorkflowInfo(Long tenantId, Long stocktakeId, String processInstanceId, String currentNode) {
            FinStocktake h = selectStocktakeById(tenantId, stocktakeId);
            if (h == null) {
                return 0;
            }
            if (processInstanceId != null) h.setProcessInstanceId(processInstanceId);
            if (currentNode != null) h.setCurrentNode(currentNode);
            return 1;
        }

        @Override
        public FinStocktake selectByProcessInstanceId(Long tenantId, String processInstanceId) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId())
                            && processInstanceId != null
                            && processInstanceId.equals(h.getProcessInstanceId()))
                    .findFirst().orElse(null);
        }
    }

    static class FakeFinStockLedgerMapper implements FinStockLedgerMapper {
        final Map<String, Integer> positions = new HashMap<>();
        final List<FinStockLedger> insertedLedgers = new ArrayList<>();
        final java.util.Set<String> existingReferenceNos = new java.util.HashSet<>();
        long nextLedgerId = 9000L;
        int downstreamCount = 0;

        @Override public int insertPositionIfAbsent(Long t, Long d, Long p) { return 0; }
        @Override public Integer selectPositionQuantityForUpdate(Long t, Long d, Long p) { return positions.getOrDefault(t+":"+d+":"+p, 0); }
        @Override public Integer selectPositionQuantity(Long t, Long d, Long p) { return positions.getOrDefault(t+":"+d+":"+p, 0); }
        @Override public int updatePositionQuantity(Long t, Long d, Long p, Integer q) {
            positions.put(t+":"+d+":"+p, q);
            return 1;
        }
        @Override public Integer sumRecordedNet(Long t, String rt, Long ri, Long p) { return 0; }
        @Override public List<Long> selectRecordedProductIds(Long t, String rt, Long ri) { return new ArrayList<>(); }
        @Override public int insertFinStockLedger(FinStockLedger l) {
            l.setLedgerId(nextLedgerId++);
            insertedLedgers.add(l);
            return 1;
        }
        @Override public com.junsong.finance.domain.vo.DailyFlowView sumDailyFlow(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public List<Long> selectSnapshotProductIds(Long t, java.time.LocalDate d, Long dept) { return new ArrayList<>(); }
        @Override public com.junsong.finance.domain.FinStockSnapshot selectPreviousSnapshot(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public com.junsong.finance.domain.FinStockLedger selectFirstDailyLedger(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public com.junsong.finance.domain.FinStockLedger selectLastLedgerBeforeDate(Long t, java.time.LocalDate d, Long dept, Long p) { return null; }
        @Override public List<com.junsong.finance.domain.vo.FinStockPositionView> selectAllTenantDeptScopesWithPosition() { return new ArrayList<>(); }
        @Override public int upsertSnapshot(com.junsong.finance.domain.FinStockSnapshot s) { return 0; }
        @Override public java.math.BigDecimal selectSaleOutUnitCost(Long t, Long ri, Long p) { return null; }
        @Override public int updateLedgerUnitCost(Long id, java.math.BigDecimal uc) { return 0; }
        @Override public int countByReferenceNo(Long t, String rn) {
            return existingReferenceNos.contains(t + ":" + rn) ? 1 : 0;
        }

        @Override
        public Integer sumMovementAfterFreeze(Long tenantId, Long deptId, Long productId, Date freezeTime) {
            return 0;
        }

    @Override
    public int countDownstreamLedgersAfterTime(Long tenantId, Long deptId, Long productId, java.util.Date afterTime) {
        return downstreamCount;
    }

}

    /**
     * Fake IStockCostService：记录调用参数，模拟成本层行为。
     * - avgUnitCost 配置当前平均成本（用于盘盈金额计算）
     * - lossCalls/gainCalls 记录调用
     * - 默认返回递增的 costLedgerId
     */
    static class FakeIStockCostService implements IStockCostService {
        final List<String> lossCalls = new ArrayList<>();
        final List<String> gainCalls = new ArrayList<>();
        final List<String> reverseCalls = new ArrayList<>();
        BigDecimal avgUnitCost = new BigDecimal("10.000000");
        long nextCostLedgerId = 7000L;

        @Override
        public void applyPurchaseInbound(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal amount, Long sourceLedgerId, String operator) { }

        @Override
        public void reversePurchaseInbound(Long tenantId, Long deptId, Long productId, int reverseQuantity, Long sourceLedgerId, String operator) { }

        @Override
        public BigDecimal applySaleOutbound(Long tenantId, Long deptId, Long productId, int quantity, boolean allowNegative, Long sourceLedgerId, String operator) {
            return avgUnitCost;
        }

        @Override
        public void reverseSaleOutbound(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal originalUnitCost, Long sourceLedgerId, String operator) { }

        @Override
        public void applyCostAdjustment(Long tenantId, Long deptId, Long productId, BigDecimal amount, String reason, String operator) { }

        @Override
        public Long applyStocktakeLoss(Long tenantId, Long deptId, Long productId, int quantity, Long sourceLedgerId, String operator) {
            lossCalls.add(tenantId + ":" + deptId + ":" + productId + ":qty=" + quantity + ":src=" + sourceLedgerId);
            return nextCostLedgerId++;
        }

        @Override
        public Long applyStocktakeGain(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal amount, Long sourceLedgerId, String operator) {
            // 模拟 amount=null 时按当前平均成本计算的契约
            BigDecimal effectiveAmount = (amount == null)
                    ? avgUnitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, java.math.RoundingMode.HALF_UP)
                    : amount;
            gainCalls.add(tenantId + ":" + deptId + ":" + productId + ":qty=" + quantity
                    + ":amount=" + effectiveAmount + ":src=" + sourceLedgerId);
            return nextCostLedgerId++;
        }

        @Override
        public Long reverseStocktakeAdjustment(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal unitCost, Long sourceLedgerId, Long originalCostLedgerId, String operator) {
            reverseCalls.add(tenantId + ":" + deptId + ":" + productId + ":qty=" + quantity
                    + ":unitCost=" + unitCost + ":src=" + sourceLedgerId + ":orig=" + originalCostLedgerId);
            return nextCostLedgerId++;
        }

        @Override
        public BigDecimal getCostLedgerUnitCost(Long tenantId, Long costLedgerId) {
            // 测试默认返回配置的平均成本，模拟原过账固化成本
            return avgUnitCost;
        }
    }

    /**
     * Fake FinAccountingPeriodMapper：默认返回 ACTIVE 期间（status="0"）。
     */
    static class FakeFinAccountingPeriodMapper implements FinAccountingPeriodMapper {
        FinAccountingPeriod currentPeriod;

        FakeFinAccountingPeriodMapper() {
            currentPeriod = new FinAccountingPeriod();
            currentPeriod.setPeriodId(1L);
            currentPeriod.setStatus("0"); // ACTIVE
        }

        @Override
        public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) {
            return currentPeriod;
        }

        @Override
        public FinAccountingPeriod selectCurrentPeriodByDeptIdForUpdate(Long tenantId, Long deptId) {
            return currentPeriod;
        }

        // 以下为接口其他方法桩实现
        @Override public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId) { return null; }
        @Override public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return null; }
        @Override public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod p) { return new ArrayList<>(); }
        @Override public int insertFinAccountingPeriod(FinAccountingPeriod p) { return 0; }
        @Override public int updateFinAccountingPeriod(FinAccountingPeriod p) { return 0; }
        @Override public int resetCarryForwardByPeriodId(Long periodId, String updateBy) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodId(Long periodId) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds) { return 0; }
        @Override public BigDecimal selectTotalVerifiedExpense(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalPurchase(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSalePayment(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSaleAmount(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalUnverifiedAdvance(Long periodId, Long deptId, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) { return "0"; }
        @Override public FinAccountingPeriod selectPeriodById(Long periodId) { return null; }
        @Override public FinAccountingPeriod selectPeriodForUpdate(Long periodId, Long tenantId, Long deptId) { return null; }
        @Override public FinAccountingPeriod selectPreviousPeriod(Long deptId, Date startTime, Long periodId) { return null; }
        @Override public FinAccountingPeriod selectNextPeriod(Long deptId, Date startTime, Long periodId) { return null; }
        @Override public int updateStartTimeOnly(Long periodId, Date startTime, Date endTime, String updateBy, String remark) { return 0; }
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
