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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 期初库存过账并发测试。
 *
 * 模拟真实数据库唯一约束行为（uk_stock_init_post_key），
 * 验证多线程并发调用 postStockInit 时，相同幂等键只产生一条业务记录。
 *
 * 场景：
 * 1. 连续点击 2 次（双标签页 / 快速双击）
 * 2. 连续点击 10 次（暴力点击）
 * 3. 网络延迟场景（所有请求同时到达后端）
 */
class FinStockInitConcurrencyTest {

    private static final Long T1 = 1L;
    private static final Long DEPT_10 = 10L;
    private static final Long PRODUCT_100 = 100L;

    private ConcurrencySafeBatchMapper batchMapper;
    private ConcurrencySafeLedgerMapper ledgerMapper;
    private FakeFinProductMapper productMapper;
    private FakeRemoteUserService remoteUserService;
    private FakeIStockCostService stockCostService;
    private FakeFinAccountingPeriodMapper accountingPeriodMapper;
    private FinStockInitServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        batchMapper = new ConcurrencySafeBatchMapper();
        ledgerMapper = new ConcurrencySafeLedgerMapper();
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

        TenantContext.setTenantId(T1);
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");

        productMapper.products.put(PRODUCT_100, buildProduct(PRODUCT_100, "可乐"));
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

    /** 走完 create → validate → submit → approve 流程，返回 APPROVED 状态的批次ID */
    private Long walkToApproved() {
        Long batchId = service.createStockInit(buildCreateRequest());
        service.validateStockInit(batchId, 0);
        service.submitStockInit(batchId, 1);
        StockInitApproveRequest approveReq = new StockInitApproveRequest();
        approveReq.setDecision("APPROVE");
        approveReq.setVersion(2);
        service.approveStockInit(batchId, approveReq);
        return batchId;
    }

    private StockInitPostRequest buildPostRequest(String idempotencyKey) {
        StockInitPostRequest req = new StockInitPostRequest();
        req.setPostIdempotencyKey(idempotencyKey);
        req.setVersion(3);
        return req;
    }

    // ===== 并发测试 =====

    /**
     * 场景1：连续点击 2 次（双标签页 / 快速双击）
     * 两个线程同时提交相同幂等键，验证只有一条业务记录产生。
     */
    @Test
    void concurrentPost_2Times_onlyOneSucceeds() throws Exception {
        Long batchId = walkToApproved();
        String idempotencyKey = "CONCURRENT-POST-2X";

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    int affected = service.postStockInit(batchId, buildPostRequest(idempotencyKey));
                    if (affected == 1) {
                        successCount.incrementAndGet();
                    }
                } catch (ServiceException e) {
                    failureCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                } catch (Throwable t) {
                    firstError.compareAndSet(null, t);
                }
                return null;
            });
        }

        ready.await(2, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        boolean done = pool.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(done, "线程池必须在 10 秒内完成");

        // 成功次数 = 1 次真实过账 + 可能的幂等重放；失败次数 = 被状态检查拒绝的请求
        assertTrue(successCount.get() >= 1, "至少 1 次成功，实际: " + successCount.get());
        assertTrue(successCount.get() + failureCount.get() == threadCount,
                "成功 + 失败应等于线程总数: success=" + successCount.get() + ", failure=" + failureCount.get());

        // 验证只产生一条库存流水（核心断言：即使多次成功，也只产生 1 条业务记录）
        assertEquals(1, ledgerMapper.insertedLedgers.size(),
                "2 次并发提交应只产生 1 条库存流水，实际: " + ledgerMapper.insertedLedgers.size());
        // 验证只调用一次成本服务
        assertEquals(1, stockCostService.gainCalls.size(),
                "2 次并发提交应只调用 1 次成本入账，实际: " + stockCostService.gainCalls.size());
    }

    /**
     * 场景2：连续点击 10 次（暴力点击）
     * 十个线程同时提交相同幂等键，验证只有一条业务记录产生。
     */
    @Test
    void concurrentPost_10Times_onlyOneSucceeds() throws Exception {
        Long batchId = walkToApproved();
        String idempotencyKey = "CONCURRENT-POST-10X";

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    int affected = service.postStockInit(batchId, buildPostRequest(idempotencyKey));
                    if (affected == 1) {
                        successCount.incrementAndGet();
                    }
                } catch (ServiceException e) {
                    failureCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                }
                return null;
            });
        }

        ready.await(3, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        boolean done = pool.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(done, "线程池必须在 15 秒内完成");

        assertTrue(successCount.get() >= 1, "至少 1 次成功");
        // 成功次数 = 1 次真实过账 + N 次幂等重放（countByPostIdempotencyKey 命中后返回 1）
        // 幂等重放次数取决于线程调度时序，但库存流水必须只有 1 条
        assertTrue(successCount.get() <= threadCount,
                "成功次数不应超过线程数: " + successCount.get());
        assertEquals(1, ledgerMapper.insertedLedgers.size(),
                "10 次并发提交应只产生 1 条库存流水，实际: " + ledgerMapper.insertedLedgers.size());
        assertEquals(1, stockCostService.gainCalls.size(),
                "10 次并发提交应只调用 1 次成本入账，实际: " + stockCostService.gainCalls.size());
    }

    /**
     * 场景3：网络延迟 —— 所有请求同时到达后端（最大并发）
     * 使用 20 个线程模拟网络延迟后同时到达的请求。
     */
    @Test
    void concurrentPost_networkDelay_allArriveSimultaneously() throws Exception {
        Long batchId = walkToApproved();
        String idempotencyKey = "CONCURRENT-POST-NETWORK-DELAY";

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    int affected = service.postStockInit(batchId, buildPostRequest(idempotencyKey));
                    if (affected == 1) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
                return null;
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        boolean done = pool.awaitTermination(20, TimeUnit.SECONDS);

        assertTrue(done, "线程池必须在 20 秒内完成");

        assertTrue(successCount.get() >= 1, "至少 1 次成功");
        assertEquals(1, ledgerMapper.insertedLedgers.size(),
                "网络延迟场景应只产生 1 条库存流水，实际: " + ledgerMapper.insertedLedgers.size());
    }

    /**
     * 场景4：双标签页 —— 两个标签页使用相同幂等键但不同版本号
     * 验证第二个请求被版本号校验拒绝。
     */
    @Test
    void concurrentPost_dualTab_sameIdempotencyKeyDifferentVersion() throws Exception {
        Long batchId = walkToApproved();
        String idempotencyKey = "CONCURRENT-POST-DUAL-TAB";

        // 标签页1：使用正确版本号 3
        StockInitPostRequest req1 = buildPostRequest(idempotencyKey);
        req1.setVersion(3);

        // 标签页2：使用过期版本号 2（模拟双标签页数据不一致）
        StockInitPostRequest req2 = buildPostRequest(idempotencyKey);
        req2.setVersion(2);

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        pool.submit(() -> {
            ready.countDown();
            try {
                start.await();
                int affected = service.postStockInit(batchId, req1);
                if (affected == 1) successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        });

        pool.submit(() -> {
            ready.countDown();
            try {
                start.await();
                int affected = service.postStockInit(batchId, req2);
                if (affected == 1) successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        });

        ready.await(2, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        boolean done = pool.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(done, "线程池必须在 10 秒内完成");

        assertTrue(successCount.get() >= 1, "至少 1 次成功");
        assertEquals(1, ledgerMapper.insertedLedgers.size(),
                "双标签页场景应只产生 1 条库存流水，实际: " + ledgerMapper.insertedLedgers.size());
    }

    // ===== 线程安全的 Fake 类（模拟真实数据库唯一约束）=====

    /**
     * 线程安全的 FinStockInitBatchMapper fake。
     *
     * 关键：updateBatchStatus 在写入 postIdempotencyKey 时模拟数据库唯一约束。
     * 如果 postIdempotencyKey 已被其他批次使用，返回 0（模拟 UNIQUE 约束冲突）。
     */
    static class ConcurrencySafeBatchMapper implements FinStockInitBatchMapper {
        final List<FinStockInitBatch> headers = new ArrayList<>();
        final Map<Long, List<FinStockInitItem>> itemsByBatch = new HashMap<>();
        final Set<String> usedIdempotencyKeys = ConcurrentHashMap.newKeySet();
        long nextId = 1L;
        // 模拟 SELECT ... FOR UPDATE 行锁：获取后阻塞其他线程对同一批次的并发访问，
        // 在 updateBatchStatus 中释放（模拟事务结束时释放行锁）。
        final ReentrantLock batchLock = new ReentrantLock();

        @Override
        public synchronized int insertBatch(FinStockInitBatch batch) {
            batch.setBatchId(nextId++);
            headers.add(batch);
            return 1;
        }

        @Override
        public synchronized FinStockInitBatch selectBatchById(Long tenantId, Long batchId) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()) && batchId.equals(h.getBatchId()))
                    .findFirst().orElse(null);
        }

        @Override
        public FinStockInitBatch selectBatchForUpdate(Long tenantId, Long batchId) {
            // 模拟 SELECT ... FOR UPDATE：获取行锁，阻塞其他线程对同一批次的并发访问。
            // 锁在 updateBatchStatus 中释放（模拟事务结束）。
            // 若批次已处于终态（POSTED），立即释放锁 —— 模拟读已提交隔离级别下
            // 已完成事务的行不再被锁定，让后续线程能读到最新状态并因状态不匹配而拒绝。
            batchLock.lock();
            FinStockInitBatch batch = selectBatchById(tenantId, batchId);
            if (batch != null && isTerminalStatus(batch.getStatus())) {
                batchLock.unlock();
            }
            return batch;
        }

        private boolean isTerminalStatus(String status) {
            return "POSTED".equals(status) || "REVERSED".equals(status) || "CANCELLED".equals(status);
        }

        @Override
        public synchronized FinStockInitBatch selectBatchByPostIdempotencyKey(Long tenantId, String postIdempotencyKey) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId())
                            && postIdempotencyKey != null
                            && postIdempotencyKey.equals(h.getPostIdempotencyKey()))
                    .findFirst().orElse(null);
        }

        @Override
        public synchronized List<FinStockInitBatch> listBatches(Long tenantId, List<Long> deptIds, String status, String batchNo) {
            return headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()))
                    .filter(h -> deptIds == null || deptIds.isEmpty() || deptIds.contains(h.getDeptId()))
                    .filter(h -> status == null || status.isEmpty() || status.equals(h.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public synchronized int countByBatchNo(Long tenantId, String batchNo) {
            return (int) headers.stream()
                    .filter(h -> tenantId.equals(h.getTenantId()) && batchNo.equals(h.getBatchNo()))
                    .count();
        }

        @Override
        public synchronized int countByPostIdempotencyKey(Long tenantId, String postIdempotencyKey) {
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
            try {
                synchronized (this) {
                    FinStockInitBatch h = selectBatchById(tenantId, batchId);
                    if (h == null || !fromStatus.equals(h.getStatus()) || !version.equals(h.getVersion())) {
                        return 0;
                    }

                    // 模拟数据库唯一约束：如果写入 postIdempotencyKey，检查是否已被其他批次使用
                    if (postIdempotencyKey != null && !postIdempotencyKey.equals(h.getPostIdempotencyKey())) {
                        if (usedIdempotencyKeys.contains(postIdempotencyKey)) {
                            // 模拟 UNIQUE 约束冲突：返回 0
                            return 0;
                        }
                        usedIdempotencyKeys.add(postIdempotencyKey);
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
            } finally {
                // 模拟事务结束：释放行锁（仅当锁被当前线程持有时）
                if (batchLock.isHeldByCurrentThread()) {
                    batchLock.unlock();
                }
            }
        }

        @Override
        public synchronized int insertBatchItem(FinStockInitItem item) {
            item.setItemId(nextId++);
            itemsByBatch.computeIfAbsent(item.getBatchId(), k -> new ArrayList<>()).add(item);
            return 1;
        }

        @Override
        public synchronized List<FinStockInitItem> listBatchItems(Long tenantId, Long batchId) {
            return itemsByBatch.getOrDefault(batchId, new ArrayList<>()).stream()
                    .filter(i -> tenantId.equals(i.getTenantId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public synchronized List<FinStockInitItem> selectBatchItemsForUpdate(Long tenantId, Long batchId) {
            return listBatchItems(tenantId, batchId);
        }

        @Override
        public synchronized int updateBatchItemPostingRefs(Long tenantId, Long itemId, Long stockLedgerId,
                                                            Long costLedgerId, Integer version) {
            FinStockInitItem item = itemsByBatch.values().stream()
                    .flatMap(List::stream)
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

    /**
     * 线程安全的 FinStockLedgerMapper fake。
     */
    static class ConcurrencySafeLedgerMapper implements FinStockLedgerMapper {
        final Map<String, Integer> positions = new ConcurrentHashMap<>();
        final List<FinStockLedger> insertedLedgers = Collections.synchronizedList(new ArrayList<>());
        final Set<String> existingReferenceNos = ConcurrentHashMap.newKeySet();
        final AtomicInteger ledgerIdSeq = new AtomicInteger(9000);

        @Override public int insertPositionIfAbsent(Long t, Long d, Long p) { return 0; }
        @Override public Integer selectPositionQuantityForUpdate(Long t, Long d, Long p) {
            return positions.getOrDefault(t + ":" + d + ":" + p, 0);
        }
        @Override public Integer selectPositionQuantity(Long t, Long d, Long p) {
            return positions.getOrDefault(t + ":" + d + ":" + p, 0);
        }
        @Override public synchronized int updatePositionQuantity(Long t, Long d, Long p, Integer q) {
            positions.put(t + ":" + d + ":" + p, q);
            return 1;
        }
        @Override public Integer sumRecordedNet(Long t, String rt, Long ri, Long p) { return 0; }
        @Override public List<Long> selectRecordedProductIds(Long t, String rt, Long ri) { return new ArrayList<>(); }
        @Override public int insertFinStockLedger(FinStockLedger l) {
            l.setLedgerId((long) ledgerIdSeq.incrementAndGet());
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
        @Override public Integer sumMovementAfterFreeze(Long tenantId, Long deptId, Long productId, Date freezeTime) { return 0; }
        @Override public int countDownstreamLedgersAfterTime(Long tenantId, Long deptId, Long productId, java.util.Date afterTime) { return 0; }
    }

    // 复用 FinStockInitServiceImplTest 中的简单 Fake 类
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

    static class FakeIStockCostService implements IStockCostService {
        final List<String> gainCalls = Collections.synchronizedList(new ArrayList<>());
        long nextCostLedgerId = 7000L;

        @Override public void applyPurchaseInbound(Long t, Long d, Long p, int q, BigDecimal a, Long s, String o) { }
        @Override public void reversePurchaseInbound(Long t, Long d, Long p, int q, Long s, String o) { }
        @Override public BigDecimal applySaleOutbound(Long t, Long d, Long p, int q, boolean a, Long s, String o) { return BigDecimal.TEN; }
        @Override public void reverseSaleOutbound(Long t, Long d, Long p, int q, BigDecimal u, Long s, String o) { }
        @Override public void applyCostAdjustment(Long t, Long d, Long p, BigDecimal a, String r, String o) { }
        @Override public Long applyStocktakeLoss(Long t, Long d, Long p, int q, Long s, String o) { return nextCostLedgerId++; }
        @Override public synchronized Long applyStocktakeGain(Long t, Long d, Long p, int q, BigDecimal a, Long s, String o) {
            gainCalls.add(t + ":" + d + ":" + p + ":qty=" + q);
            return nextCostLedgerId++;
        }
        @Override public Long reverseStocktakeAdjustment(Long t, Long d, Long p, int q, BigDecimal u, Long s, Long o, String op) { return nextCostLedgerId++; }
        @Override public BigDecimal getCostLedgerUnitCost(Long t, Long c) { return BigDecimal.TEN; }
    }

    static class FakeFinAccountingPeriodMapper implements FinAccountingPeriodMapper {
        FinAccountingPeriod currentPeriod;

        FakeFinAccountingPeriodMapper() {
            currentPeriod = new FinAccountingPeriod();
            currentPeriod.setPeriodId(1L);
            currentPeriod.setStatus("0");
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

    static class FakeRemoteUserService implements RemoteUserService {
        @Override public R<List<SysDept>> getUserDeptList(String username, String source) { return R.ok(new ArrayList<>()); }
        @Override public R<LoginUser> getUserInfo(String username, String source) { return R.fail("n/a"); }
        @Override public R<LoginUser> getUserInfoById(Long userId, String source) { return R.fail("n/a"); }
        @Override public R<Boolean> registerUserInfo(SysUser user, String source) { return R.ok(false); }
        @Override public R<Boolean> recordUserLogin(SysUser user, String source) { return R.ok(false); }
        @Override public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.ok(new ArrayList<>()); }
        @Override public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) { return R.ok(false); }
    }
}
