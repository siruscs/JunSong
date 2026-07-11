package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinStockLedgerServiceImpl 单元测试（对账模型）。
 * 使用手写 fake mapper（无 Mockito），内存维护 position 与已记录净额。
 * 对账语义：每次调用给定"目标数量"，服务计算与已记录净额的差额并生成流水，天然幂等，
 * 支持修改差额、删除反向。
 */
class FinStockLedgerServiceImplTest {

    private FakeStockLedgerMapper mapper;
    private FinStockLedgerServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new FakeStockLedgerMapper();
        service = new FinStockLedgerServiceImpl();
        Field field = FinStockLedgerServiceImpl.class.getDeclaredField("finStockLedgerMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @Test
    void firstPurchase_recordsPurchaseInAndSetsPosition() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 10, new BigDecimal("3.00"), "alice");

        assertEquals(1, mapper.inserted.size());
        FinStockLedger led = mapper.inserted.get(0);
        assertEquals("PURCHASE_IN", led.getChangeType());
        assertEquals(0, led.getBeforeQuantity());
        assertEquals(10, led.getChangeQuantity());
        assertEquals(10, led.getAfterQuantity());
        assertEquals(10, mapper.position(1L, 100L));
    }

    @Test
    void repeatedPurchaseSameTarget_isIdempotent() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 10, new BigDecimal("3.00"), "alice");
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 10, new BigDecimal("3.00"), "alice");

        assertEquals(1, mapper.inserted.size(), "目标数量未变，重复对账不应生成新流水");
        assertEquals(10, mapper.position(1L, 100L));
    }

    @Test
    void purchaseQuantityIncreased_generatesPositiveDelta() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 10, new BigDecimal("3.00"), "alice");
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 15, new BigDecimal("3.00"), "alice");

        assertEquals(2, mapper.inserted.size());
        FinStockLedger delta = mapper.inserted.get(1);
        assertEquals("PURCHASE_IN", delta.getChangeType());
        assertEquals(5, delta.getChangeQuantity());
        assertEquals(15, delta.getAfterQuantity());
        assertEquals(15, mapper.position(1L, 100L));
    }

    @Test
    void purchaseQuantityDecreased_generatesReverseDelta() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 10, new BigDecimal("3.00"), "alice");
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 4, new BigDecimal("3.00"), "alice");

        FinStockLedger delta = mapper.inserted.get(1);
        assertEquals("PURCHASE_REVERSE", delta.getChangeType());
        assertEquals(-6, delta.getChangeQuantity());
        assertEquals(4, delta.getAfterQuantity());
        assertEquals(4, mapper.position(1L, 100L));
    }

    @Test
    void purchaseDeleted_reversesAllToZero() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 10, new BigDecimal("3.00"), "alice");
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 0, null, "alice");

        FinStockLedger delta = mapper.inserted.get(1);
        assertEquals("PURCHASE_REVERSE", delta.getChangeType());
        assertEquals(-10, delta.getChangeQuantity());
        assertEquals(0, mapper.position(1L, 100L));
    }

    @Test
    void firstSale_recordsSaleOutAndReducesPosition() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 20, new BigDecimal("3.00"), "alice");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 3, "bob");

        FinStockLedger led = mapper.inserted.get(1);
        assertEquals("SALE_OUT", led.getChangeType());
        assertEquals(20, led.getBeforeQuantity());
        assertEquals(-3, led.getChangeQuantity());
        assertEquals(17, led.getAfterQuantity());
        assertEquals(17, mapper.position(1L, 100L));
    }

    @Test
    void saleQuantityIncreased_generatesFurtherSaleOut() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 20, new BigDecimal("3.00"), "alice");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 3, "bob");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 5, "bob");

        FinStockLedger delta = mapper.inserted.get(2);
        assertEquals("SALE_OUT", delta.getChangeType());
        assertEquals(-2, delta.getChangeQuantity());
        assertEquals(15, mapper.position(1L, 100L));
    }

    @Test
    void saleQuantityDecreased_generatesSaleReverse() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 20, new BigDecimal("3.00"), "alice");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 5, "bob");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 2, "bob");

        FinStockLedger delta = mapper.inserted.get(2);
        assertEquals("SALE_REVERSE", delta.getChangeType());
        assertEquals(3, delta.getChangeQuantity());
        assertEquals(18, mapper.position(1L, 100L));
    }

    @Test
    void saleDeleted_restoresStock() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 20, new BigDecimal("3.00"), "alice");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 5, "bob");
        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 0, "bob");

        FinStockLedger delta = mapper.inserted.get(2);
        assertEquals("SALE_REVERSE", delta.getChangeType());
        assertEquals(5, delta.getChangeQuantity());
        assertEquals(20, mapper.position(1L, 100L));
    }

    @Test
    void saleExceedingStock_throwsAndDoesNotInsert() {
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 2, new BigDecimal("3.00"), "alice");
        mapper.inserted.clear();

        assertThrows(ServiceException.class,
                () -> service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 5, "bob"));
        assertTrue(mapper.inserted.isEmpty(), "库存不足时不应插入出库流水");
        assertEquals(2, mapper.position(1L, 100L), "阻断后库存不变");
    }

    @Test
    void saleExceedingStock_whenConfigEnabled_allowsNegativePosition() {
        service.setAllowNegativeSaleOutForTest(true);
        service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", 2, new BigDecimal("3.00"), "alice");
        mapper.inserted.clear();

        service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", 5, "bob");

        assertEquals(1, mapper.inserted.size(), "允许超库存时应继续写销售出库流水");
        FinStockLedger led = mapper.inserted.get(0);
        assertEquals("SALE_OUT", led.getChangeType());
        assertEquals(2, led.getBeforeQuantity());
        assertEquals(-5, led.getChangeQuantity());
        assertEquals(-3, led.getAfterQuantity());
        assertEquals(-3, mapper.position(1L, 100L), "允许先销售后进货时，当前库存可以暂时为负");
    }

    @Test
    void negativeTargetQuantity_throws() {
        assertThrows(ServiceException.class,
                () -> service.reconcilePurchaseStock(1L, 100L, "可乐", 9001L, "PO-1", -1, BigDecimal.ONE, "a"));
        assertThrows(ServiceException.class,
                () -> service.reconcileSaleStock(1L, 100L, "可乐", 8001L, "SO-1", -1, "b"));
    }

    /** 手写 fake mapper：内存维护 position 与已插入流水。 */
    static class FakeStockLedgerMapper implements FinStockLedgerMapper {
        final List<FinStockLedger> inserted = new ArrayList<>();
        final Map<String, Integer> positions = new HashMap<>();

        private String key(Long deptId, Long productId) {
            return deptId + ":" + productId;
        }

        int position(Long deptId, Long productId) {
            return positions.getOrDefault(key(deptId, productId), 0);
        }

        @Override
        public int insertPositionIfAbsent(Long deptId, Long productId) {
            positions.putIfAbsent(key(deptId, productId), 0);
            return 1;
        }

        @Override
        public Integer selectPositionQuantityForUpdate(Long deptId, Long productId) {
            return positions.get(key(deptId, productId));
        }

        @Override
        public int updatePositionQuantity(Long deptId, Long productId, Integer quantity) {
            positions.put(key(deptId, productId), quantity);
            return 1;
        }

        @Override
        public Integer sumRecordedNet(String referenceType, Long referenceId, Long productId) {
            int sum = 0;
            for (FinStockLedger l : inserted) {
                if (referenceType.equals(l.getReferenceType())
                        && referenceId.equals(l.getReferenceId())
                        && productId.equals(l.getProductId())) {
                    sum += l.getChangeQuantity();
                }
            }
            return sum;
        }

        @Override
        public List<Long> selectRecordedProductIds(String referenceType, Long referenceId) {
            List<Long> ids = new ArrayList<>();
            for (FinStockLedger l : inserted) {
                if (referenceType.equals(l.getReferenceType())
                        && referenceId.equals(l.getReferenceId())
                        && !ids.contains(l.getProductId())) {
                    ids.add(l.getProductId());
                }
            }
            return ids;
        }

        @Override
        public int insertFinStockLedger(FinStockLedger ledger) {
            inserted.add(ledger);
            return 1;
        }

        // ---- R7-E 快照相关方法桩实现（本测试不涉及，仅为满足接口契约） ----
        @Override
        public java.util.List<com.junsong.finance.domain.vo.FinStockPositionView> selectPositionsByDept(Long deptId) {
            return new java.util.ArrayList<>();
        }

        @Override
        public com.junsong.finance.domain.vo.DailyFlowView sumDailyFlow(java.time.LocalDate snapshotDate,
                                                                          Long deptId, Long productId) {
            return null;
        }

        @Override
        public int upsertSnapshot(com.junsong.finance.domain.FinStockSnapshot snapshot) {
            return 0;
        }

        @Override
        public java.util.List<Long> selectAllDeptIdsWithPosition() {
            return java.util.Collections.emptyList();
        }
    }
}
