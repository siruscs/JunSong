package com.junsong.finance.service.impl;

import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStockInitBatch;
import com.junsong.finance.domain.FinStockInitItem;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.domain.vo.StockInitApproveRequest;
import com.junsong.finance.domain.vo.StockInitCreateRequest;
import com.junsong.finance.domain.vo.StockInitItemInput;
import com.junsong.finance.domain.vo.StockInitPostRequest;
import com.junsong.finance.domain.vo.StockInitQuery;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockInitBatchMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinStockInitServiceImpl 单元测试。
 *
 * 手写 fake mapper（无 Mockito），覆盖：
 * - 租户缺失拒绝
 * - 未授权部门拒绝
 * - batchNo 服务端生成（不接受客户端传入）
 * - 金额 = 数量 × 单位成本（scale 2 HALF_UP）
 * - 商品归属校验
 * - 状态机：DRAFT → VALIDATED → SUBMITTED → APPROVED → POSTED
 * - 过账幂等键
 * - 会计期间锁定
 * - 重复过账拒绝
 * - 审批人不能是创建人
 */
class FinStockInitServiceImplTest {

    private static final Long T1 = 1L;
    private static final Long DEPT_10 = 10L;
    private static final Long DEPT_20 = 20L; // 未授权部门
    private static final Long PRODUCT_100 = 100L;
    private static final Long PRODUCT_200 = 200L;

    private FakeFinStockInitBatchMapper batchMapper;
    private FakeFinStockLedgerMapper ledgerMapper;
    private FakeFinProductMapper productMapper;
    private FakeRemoteUserService remoteUserService;
    private FakeIStockCostService stockCostService;
    private FakeFinAccountingPeriodMapper accountingPeriodMapper;
    private FinStockInitServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        batchMapper = new FakeFinStockInitBatchMapper();
        ledgerMapper = new FakeFinStockLedgerMapper();
        productMapper = new FakeFinProductMapper();
        remoteUserService = new FakeRemoteUserService();
        stockCostService = new FakeIStockCostService();
        accountingPeriodMapper = new FakeFinAccountingPeriodMapper();
        service = new FinStockInitServiceImpl();

        inject("finStockInitBatchMapper", batchMapper);
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
        Field f = FinStockInitServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    private FinProduct buildProduct(Long id, String name) {
        FinProduct p = new FinProduct();
        p.setProductId(id);
        p.setProductName(name);
        return p;
    }

    private StockInitItemInput buildItemInput(Long productId, String quantity, String unitCost) {
        StockInitItemInput input = new StockInitItemInput();
        input.setProductId(productId);
        input.setQuantity(new BigDecimal(quantity));
        input.setUnitCost(new BigDecimal(unitCost));
        return input;
    }

    private StockInitCreateRequest buildCreateRequest() {
        StockInitCreateRequest req = new StockInitCreateRequest();
        req.setDeptId(DEPT_10);
        req.setInitDate(new Date());
        req.setItems(Arrays.asList(buildItemInput(PRODUCT_100, "10", "10.50")));
        req.setRemark("期初建账");
        return req;
    }

    /** 走完 create → validate → submit → approve 流程，返回 APPROVED 状态的批次ID（version=3） */
    private Long walkToApproved(StockInitCreateRequest createReq) {
        Long batchId = service.createStockInit(createReq);
        service.validateStockInit(batchId, 0);   // DRAFT → VALIDATED, version 0→1
        service.submitStockInit(batchId, 1);     // VALIDATED → SUBMITTED, version 1→2
        StockInitApproveRequest approveReq = new StockInitApproveRequest();
        approveReq.setDecision("APPROVE");
        approveReq.setVersion(2);
        service.approveStockInit(batchId, approveReq); // SUBMITTED → APPROVED, version 2→3
        return batchId;
    }

    // ===== 创建 =====

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThrows(ServiceException.class, () -> service.createStockInit(buildCreateRequest()));
    }

    @Test
    void create_unauthorizedDept_throws() {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);

        StockInitCreateRequest req = buildCreateRequest();
        req.setDeptId(DEPT_20);
        assertThrows(ServiceException.class, () -> service.createStockInit(req));
        assertTrue(batchMapper.insertedHeaders.isEmpty(), "未授权部门创建不得写入头表");
    }

    @Test
    void create_serverGeneratesBatchNo() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));

        Long batchId = service.createStockInit(buildCreateRequest());

        assertNotNull(batchId);
        assertEquals(1, batchMapper.insertedHeaders.size());
        FinStockInitBatch header = batchMapper.insertedHeaders.get(0);
        assertNotNull(header.getBatchNo(), "batchNo 必须由服务端生成");
        assertTrue(header.getBatchNo().startsWith("SI"), "batchNo 必须以 SI 开头");
        assertEquals("DRAFT", header.getStatus());
        assertEquals(T1, header.getTenantId());
        assertEquals(DEPT_10, header.getDeptId());
        assertEquals(0, header.getVersion());
    }

    @Test
    void create_computesAmountFromQuantityAndUnitCost() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));

        StockInitCreateRequest req = buildCreateRequest();
        // quantity=10, unitCost=10.50 → amount=105.00
        req.setItems(Arrays.asList(buildItemInput(PRODUCT_100, "10", "10.50")));

        service.createStockInit(req);

        assertEquals(1, batchMapper.insertedItems.size());
        FinStockInitItem item = batchMapper.insertedItems.get(0);
        assertEquals(0, item.getAmount().compareTo(new BigDecimal("105.00")),
                "amount = quantity * unitCost = 10 * 10.50 = 105.00");
    }

    @Test
    void create_validatesProductBelongsToDept() {
        // 商品未注册到该门店（productMapper 中无该商品）
        StockInitCreateRequest req = buildCreateRequest();
        assertThrows(ServiceException.class, () -> service.createStockInit(req));
        assertTrue(batchMapper.insertedHeaders.isEmpty(), "商品归属校验失败不得写入头表");
    }

    // ===== 校验 =====

    @Test
    void validate_missingItems_throws() {
        // 直接构造一个无明细的批次头
        FinStockInitBatch header = new FinStockInitBatch();
        header.setBatchId(1L);
        header.setTenantId(T1);
        header.setDeptId(DEPT_10);
        header.setBatchNo("SI-TEST-001");
        header.setStatus("DRAFT");
        header.setVersion(0);
        batchMapper.headers.add(header);

        assertThrows(ServiceException.class, () -> service.validateStockInit(1L, 0));
    }

    @Test
    void validate_negativeQuantity_throws() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = service.createStockInit(buildCreateRequest());

        // 篡改行数量为负（绕过创建时的校验）
        FinStockInitItem item = batchMapper.insertedItems.get(0);
        item.setQuantity(new BigDecimal("-5"));

        assertThrows(ServiceException.class, () -> service.validateStockInit(batchId, 0));
    }

    // ===== 提交 =====

    @Test
    void submit_fromDraftDirectly_throws() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = service.createStockInit(buildCreateRequest());
        // 未校验直接提交
        assertThrows(ServiceException.class, () -> service.submitStockInit(batchId, 0));
    }

    @Test
    void submit_fromValidated_success() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = service.createStockInit(buildCreateRequest());
        service.validateStockInit(batchId, 0); // DRAFT → VALIDATED

        int affected = service.submitStockInit(batchId, 1); // VALIDATED → SUBMITTED
        assertEquals(1, affected);
        FinStockInitBatch header = batchMapper.insertedHeaders.get(0);
        assertEquals("SUBMITTED", header.getStatus());
        assertNotNull(header.getSubmittedBy());
        assertNotNull(header.getSubmittedTime());
    }

    // ===== 审批 =====

    @Test
    void approve_notSubmitted_throws() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = service.createStockInit(buildCreateRequest());
        // DRAFT 状态直接审批
        StockInitApproveRequest req = new StockInitApproveRequest();
        req.setDecision("APPROVE");
        req.setVersion(0);
        assertThrows(ServiceException.class, () -> service.approveStockInit(batchId, req));
    }

    @Test
    void approve_approverIsCreator_throws() {
        // 非 admin 用户创建并尝试审批自己的批次
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("bob");
        remoteUserService.authorizedDepts = Arrays.asList(DEPT_10);
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));

        Long batchId = service.createStockInit(buildCreateRequest());
        service.validateStockInit(batchId, 0);
        service.submitStockInit(batchId, 1);

        StockInitApproveRequest req = new StockInitApproveRequest();
        req.setDecision("APPROVE");
        req.setVersion(2);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.approveStockInit(batchId, req));
        assertTrue(ex.getMessage().contains("审批人不能是创建人"),
                "创建人审批自己的批次应拒绝，实际错误：" + ex.getMessage());
    }

    @Test
    void approve_success() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = service.createStockInit(buildCreateRequest());
        service.validateStockInit(batchId, 0);
        service.submitStockInit(batchId, 1);

        // admin 审批（admin 创建，但 admin 跳过创建人校验）
        StockInitApproveRequest req = new StockInitApproveRequest();
        req.setDecision("APPROVE");
        req.setComment("同意过账");
        req.setVersion(2);

        int affected = service.approveStockInit(batchId, req);
        assertEquals(1, affected);
        FinStockInitBatch header = batchMapper.insertedHeaders.get(0);
        assertEquals("APPROVED", header.getStatus());
        assertNotNull(header.getApprovedBy());
        assertNotNull(header.getApprovedTime());
    }

    // ===== 过账 =====

    @Test
    void post_idempotentReplayReturnsSame() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = walkToApproved(buildCreateRequest());

        StockInitPostRequest req = new StockInitPostRequest();
        req.setPostIdempotencyKey("POST-KEY-1");
        req.setVersion(3);

        int first = service.postStockInit(batchId, req);
        assertEquals(1, first);
        assertEquals("POSTED", batchMapper.insertedHeaders.get(0).getStatus());

        // 相同幂等键、相同批次再次请求 —— 应返回 1（幂等重放）
        StockInitPostRequest req2 = new StockInitPostRequest();
        req2.setPostIdempotencyKey("POST-KEY-1");
        req2.setVersion(4);
        int second = service.postStockInit(batchId, req2);
        assertEquals(1, second, "幂等重放应返回成功");
    }

    @Test
    void post_idempotentKeyUsedByOtherBatch_throws() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));

        // 批次1 先过账使用 key "SHARED-POST-KEY"
        Long batch1 = walkToApproved(buildCreateRequest());
        StockInitPostRequest req1 = new StockInitPostRequest();
        req1.setPostIdempotencyKey("SHARED-POST-KEY");
        req1.setVersion(3);
        service.postStockInit(batch1, req1);

        // 批次2 尝试用相同 key
        Long batch2 = walkToApproved(buildCreateRequest());
        StockInitPostRequest req2 = new StockInitPostRequest();
        req2.setPostIdempotencyKey("SHARED-POST-KEY");
        req2.setVersion(3);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.postStockInit(batch2, req2));
        assertTrue(ex.getMessage().contains("已被其他批次使用"),
                "幂等键被其他批次占用时应拒绝，实际错误：" + ex.getMessage());
    }

    @Test
    void post_lockedPeriod_throws() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = walkToApproved(buildCreateRequest());

        accountingPeriodMapper.currentPeriod.setStatus("2"); // 已结转

        StockInitPostRequest req = new StockInitPostRequest();
        req.setPostIdempotencyKey("POST-KEY-LOCKED");
        req.setVersion(3);

        try {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> service.postStockInit(batchId, req));
            assertTrue(ex.getMessage().contains("会计期间已结转"),
                    "期间已结转应拒绝过账，实际错误：" + ex.getMessage());
        } finally {
            accountingPeriodMapper.currentPeriod.setStatus("0");
        }
    }

    @Test
    void post_duplicatePost_throws() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = walkToApproved(buildCreateRequest());

        FinStockInitBatch header = batchMapper.insertedHeaders.get(0);
        // 模拟 batchNo 已有库存流水（重复过账）
        ledgerMapper.existingReferenceNos.add(T1 + ":" + header.getBatchNo());

        StockInitPostRequest req = new StockInitPostRequest();
        req.setPostIdempotencyKey("POST-KEY-DUP");
        req.setVersion(3);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.postStockInit(batchId, req));
        assertTrue(ex.getMessage().contains("已过账"),
                "batchNo 已有流水时应拒绝重复过账，实际错误：" + ex.getMessage());
    }

    @Test
    void post_success_generatesLedgerAndCost() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        Long batchId = walkToApproved(buildCreateRequest());

        StockInitPostRequest req = new StockInitPostRequest();
        req.setPostIdempotencyKey("POST-KEY-OK");
        req.setVersion(3);

        int affected = service.postStockInit(batchId, req);

        assertEquals(1, affected);
        FinStockInitBatch header = batchMapper.insertedHeaders.get(0);
        assertEquals("POSTED", header.getStatus());
        assertNotNull(header.getPostedBy());
        assertNotNull(header.getPostedTime());
        assertEquals("POST-KEY-OK", header.getPostIdempotencyKey());

        // 写入一笔库存流水
        assertEquals(1, ledgerMapper.insertedLedgers.size());
        FinStockLedger ledger = ledgerMapper.insertedLedgers.get(0);
        assertEquals("STOCK_INIT", ledger.getChangeType());
        assertEquals("STOCK_INIT", ledger.getReferenceType());
        assertEquals(header.getBatchNo(), ledger.getReferenceNo());
        assertEquals(10, ledger.getChangeQuantity().intValue(), "数量=10");
        assertEquals(10, ledger.getAfterQuantity().intValue(), "期初后库存=10");

        // 库存结存更新
        assertEquals(10, ledgerMapper.positions.get(T1 + ":" + DEPT_10 + ":" + PRODUCT_100).intValue());

        // 成本服务被调用一次（applyStocktakeGain）
        assertEquals(1, stockCostService.gainCalls.size());
        assertTrue(stockCostService.lossCalls.isEmpty());

        // 行表过账引用已设置
        FinStockInitItem item = batchMapper.insertedItems.get(0);
        assertNotNull(item.getStockLedgerId());
        assertNotNull(item.getCostLedgerId());
    }

    @Test
    void post_amountPrecision() {
        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
        productMapper.products.put(PRODUCT_200, buildProduct(PRODUCT_200, "雪碧"));

        // quantity=3, unitCost=10.335 → amount = 3 * 10.335 = 31.005 → scale 2 HALF_UP = 31.01
        StockInitCreateRequest req = new StockInitCreateRequest();
        req.setDeptId(DEPT_10);
        req.setInitDate(new Date());
        req.setItems(Arrays.asList(buildItemInput(PRODUCT_100, "3", "10.335")));
        Long batchId = service.createStockInit(req);

        // 验证创建时金额精度
        FinStockInitItem item = batchMapper.insertedItems.get(0);
        assertEquals(0, item.getAmount().compareTo(new BigDecimal("31.01")),
                "amount = 3 * 10.335 = 31.005 → scale 2 HALF_UP = 31.01，实际：" + item.getAmount());

        // 走到 APPROVED 并过账
        service.validateStockInit(batchId, 0);
        service.submitStockInit(batchId, 1);
        StockInitApproveRequest approveReq = new StockInitApproveRequest();
        approveReq.setDecision("APPROVE");
        approveReq.setVersion(2);
        service.approveStockInit(batchId, approveReq);

        StockInitPostRequest postReq = new StockInitPostRequest();
        postReq.setPostIdempotencyKey("POST-PRECISION-1");
        postReq.setVersion(3);
        service.postStockInit(batchId, postReq);

        // 验证传给成本服务的金额也是 31.01
        assertEquals(1, stockCostService.gainCalls.size());
        assertTrue(stockCostService.gainCalls.get(0).contains("amount=31.01"),
                "过账时传给成本服务的金额应为 31.01，实际：" + stockCostService.gainCalls.get(0));
    }

    // ===== 辅助 Fake 类 =====

    static class FakeFinStockInitBatchMapper implements FinStockInitBatchMapper {
        final List<FinStockInitBatch> insertedHeaders = new ArrayList<>();
        final List<FinStockInitItem> insertedItems = new ArrayList<>();
        final List<FinStockInitBatch> headers = new ArrayList<>();
        final Map<Long, List<FinStockInitItem>> itemsByBatch = new HashMap<>();
        long nextId = 1L;

        @Override
        public int insertBatch(FinStockInitBatch batch) {
            batch.setBatchId(nextId++);
            insertedHeaders.add(batch);
            headers.add(batch);
            return 1;
        }

        @Override
        public FinStockInitBatch selectBatchById(Long tenantId, Long batchId) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()) && batchId.equals(h.getBatchId()))
                    .findFirst().orElse(null);
        }

        @Override
        public FinStockInitBatch selectBatchForUpdate(Long tenantId, Long batchId) {
            return selectBatchById(tenantId, batchId);
        }

        @Override
        public FinStockInitBatch selectBatchByPostIdempotencyKey(Long tenantId, String postIdempotencyKey) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId())
                            && postIdempotencyKey != null
                            && postIdempotencyKey.equals(h.getPostIdempotencyKey()))
                    .findFirst().orElse(null);
        }

        @Override
        public List<FinStockInitBatch> listBatches(Long tenantId, List<Long> deptIds, String status, String batchNo) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()))
                    .filter(h -> deptIds == null || deptIds.isEmpty() || deptIds.contains(h.getDeptId()))
                    .filter(h -> status == null || status.isEmpty() || status.equals(h.getStatus()))
                    .filter(h -> batchNo == null || batchNo.isEmpty() || (h.getBatchNo() != null && h.getBatchNo().contains(batchNo)))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public int countByBatchNo(Long tenantId, String batchNo) {
            return (int) headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()) && batchNo.equals(h.getBatchNo()))
                    .count();
        }

        @Override
        public int countByPostIdempotencyKey(Long tenantId, String postIdempotencyKey) {
            return (int) headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId())
                            && postIdempotencyKey != null
                            && postIdempotencyKey.equals(h.getPostIdempotencyKey()))
                    .count();
        }

        @Override
        public int updateBatchStatus(Long tenantId, Long batchId, String fromStatus, String toStatus,
                                      Integer version, String updateBy, String submittedBy, String approvedBy,
                                      String postedBy, String postIdempotencyKey) {
            FinStockInitBatch h = selectBatchById(tenantId, batchId);
            if (h == null || !fromStatus.equals(h.getStatus()) || !version.equals(h.getVersion())) {
                return 0;
            }
            h.setStatus(toStatus);
            h.setVersion(h.getVersion() + 1);
            java.util.Date now = new java.util.Date();
            if (submittedBy != null) { h.setSubmittedBy(submittedBy); h.setSubmittedTime(now); }
            if (approvedBy != null) { h.setApprovedBy(approvedBy); h.setApprovedTime(now); }
            if (postedBy != null) { h.setPostedBy(postedBy); h.setPostedTime(now); }
            if (postIdempotencyKey != null) h.setPostIdempotencyKey(postIdempotencyKey);
            return 1;
        }

        @Override
        public int insertBatchItem(FinStockInitItem item) {
            item.setItemId(nextId++);
            insertedItems.add(item);
            itemsByBatch.computeIfAbsent(item.getBatchId(), k -> new ArrayList<>()).add(item);
            return 1;
        }

        @Override
        public List<FinStockInitItem> listBatchItems(Long tenantId, Long batchId) {
            return itemsByBatch.getOrDefault(batchId, new ArrayList<>()).stream()
                    .filter(i -> tenantId.equals(i.getTenantId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<FinStockInitItem> selectBatchItemsForUpdate(Long tenantId, Long batchId) {
            return listBatchItems(tenantId, batchId);
        }

        @Override
        public int updateBatchItemPostingRefs(Long tenantId, Long itemId, Long stockLedgerId,
                                               Long costLedgerId, Integer version) {
            FinStockInitItem item = insertedItems.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && itemId.equals(i.getItemId()))
                    .findFirst().orElse(null);
            if (item == null || !version.equals(item.getVersion())) {
                return 0;
            }
            item.setStockLedgerId(stockLedgerId);
            item.setCostLedgerId(costLedgerId);
            return 1;
        }
    }

    static class FakeFinStockLedgerMapper implements FinStockLedgerMapper {
        final Map<String, Integer> positions = new HashMap<>();
        final List<FinStockLedger> insertedLedgers = new ArrayList<>();
        final java.util.Set<String> existingReferenceNos = new java.util.HashSet<>();
        long nextLedgerId = 9000L;

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
        @Override public FinStockLedger selectByIdempotencyKey(Long tenantId, String idempotencyKey) { return null; }
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
        @Override public Integer sumMovementAfterFreeze(Long tenantId, Long deptId, Long productId, Date freezeTime) { return 0; }
        @Override public int countDownstreamLedgersAfterTime(Long tenantId, Long deptId, Long productId, java.util.Date afterTime) { return 0; }
    }

    static class FakeIStockCostService implements IStockCostService {
        final List<String> lossCalls = new ArrayList<>();
        final List<String> gainCalls = new ArrayList<>();
        final List<String> reverseCalls = new ArrayList<>();
        BigDecimal avgUnitCost = new BigDecimal("10.000000");
        long nextCostLedgerId = 7000L;

        @Override public void applyPurchaseInbound(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal amount, Long sourceLedgerId, String operator) { }
        @Override public void reversePurchaseInbound(Long tenantId, Long deptId, Long productId, int reverseQuantity, Long sourceLedgerId, String operator) { }
        @Override public BigDecimal applySaleOutbound(Long tenantId, Long deptId, Long productId, int quantity, boolean allowNegative, Long sourceLedgerId, String operator) { return avgUnitCost; }
        @Override public void reverseSaleOutbound(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal originalUnitCost, Long sourceLedgerId, String operator) { }
        @Override public void applyCostAdjustment(Long tenantId, Long deptId, Long productId, BigDecimal amount, String reason, String operator) { }
        @Override public Long applyStocktakeLoss(Long tenantId, Long deptId, Long productId, int quantity, Long sourceLedgerId, String operator) {
            lossCalls.add(tenantId + ":" + deptId + ":" + productId + ":qty=" + quantity + ":src=" + sourceLedgerId);
            return nextCostLedgerId++;
        }
        @Override public Long applyStocktakeGain(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal amount, Long sourceLedgerId, String operator) {
            BigDecimal effectiveAmount = (amount == null)
                    ? avgUnitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP)
                    : amount;
            gainCalls.add(tenantId + ":" + deptId + ":" + productId + ":qty=" + quantity
                    + ":amount=" + effectiveAmount + ":src=" + sourceLedgerId);
            return nextCostLedgerId++;
        }
        @Override public Long reverseStocktakeAdjustment(Long tenantId, Long deptId, Long productId, int quantity, BigDecimal unitCost, Long sourceLedgerId, Long originalCostLedgerId, String operator) {
            reverseCalls.add(tenantId + ":" + deptId + ":" + productId + ":qty=" + quantity
                    + ":unitCost=" + unitCost + ":src=" + sourceLedgerId + ":orig=" + originalCostLedgerId);
            return nextCostLedgerId++;
        }
        @Override public BigDecimal getCostLedgerUnitCost(Long tenantId, Long costLedgerId) { return avgUnitCost; }
    }

    static class FakeFinAccountingPeriodMapper implements FinAccountingPeriodMapper {
        FinAccountingPeriod currentPeriod;

        FakeFinAccountingPeriodMapper() {
            currentPeriod = new FinAccountingPeriod();
            currentPeriod.setPeriodId(1L);
            currentPeriod.setStatus("0"); // ACTIVE
        }

        @Override public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return currentPeriod; }
        @Override public FinAccountingPeriod selectCurrentPeriodByDeptIdForUpdate(Long tenantId, Long deptId) { return currentPeriod; }
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

        @Override public FinProduct selectFinProductByProductIdAndDeptId(Long productId, Long deptId) { return products.get(productId); }
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

        @Override public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> depts = new ArrayList<>();
            for (Long deptId : authorizedDepts) {
                SysDept d = new SysDept();
                d.setDeptId(deptId);
                depts.add(d);
            }
            return R.ok(depts);
        }
        @Override public R<LoginUser> getUserInfo(String username, String source) { return R.fail("not implemented"); }
        @Override public R<LoginUser> getUserInfoById(Long userId, String source) { return R.fail("not implemented"); }
        @Override public R<Boolean> registerUserInfo(SysUser user, String source) { return R.ok(false); }
        @Override public R<Boolean> recordUserLogin(SysUser user, String source) { return R.ok(false); }
        @Override public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.ok(new ArrayList<>()); }
        @Override public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) { return R.ok(false); }
    }
}
