package com.junsong.finance.service.impl;

import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinPurchaseDetail;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinPurchaseServiceImpl 库存流水接入单元测试（对账模型）。
 * 仅覆盖 applyPurchaseStockIn 采购入库流水逻辑，使用手写 fake（无 Mockito）。
 */
class FinPurchaseServiceImplTest {

    private FakeStockLedgerMapper mapper;
    private FinStockLedgerServiceImpl stockLedgerService;
    private FinPurchaseServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new FakeStockLedgerMapper();
        stockLedgerService = new FinStockLedgerServiceImpl();
        inject(FinStockLedgerServiceImpl.class, stockLedgerService, "finStockLedgerMapper", mapper);

        service = new FinPurchaseServiceImpl();
        inject(FinPurchaseServiceImpl.class, service, "finStockLedgerService", stockLedgerService);
        inject(FinPurchaseServiceImpl.class, service, "finStockLedgerMapper", mapper);
    }

    private static void inject(Class<?> clazz, Object target, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private FinPurchase purchase(Long id, String no, Long deptId, String status, FinPurchaseDetail... details) {
        FinPurchase p = new FinPurchase();
        p.setPurchaseId(id);
        p.setPurchaseNo(no);
        p.setDeptId(deptId);
        p.setStatus(status);
        p.setDetails(new ArrayList<>(Arrays.asList(details)));
        return p;
    }

    private FinPurchaseDetail detail(Long productId, String name, int quantity, String price) {
        FinPurchaseDetail d = new FinPurchaseDetail();
        d.setProductId(productId);
        d.setProductName(name);
        d.setQuantity(quantity);
        d.setPrice(new BigDecimal(price));
        return d;
    }

    @Test
    void confirmedPurchase_generatesPurchaseInLedger() {
        FinPurchase p = purchase(9001L, "PO-1", 1L, "1",
                detail(100L, "可乐", 10, "3.00"),
                detail(200L, "雪碧", 5, "2.50"));

        service.applyPurchaseStockIn(p);

        assertEquals(2, mapper.inserted.size(), "已确认采购单应为每个明细生成入库流水");
        assertEquals("PURCHASE_IN", ledgerOf(100L).getChangeType());
        assertEquals(10, mapper.position(1L, 100L));
        assertEquals("PURCHASE", ledgerOf(100L).getReferenceType());
        assertEquals(9001L, ledgerOf(100L).getReferenceId());
    }

    @Test
    void draftPurchase_generatesNoLedger() {
        FinPurchase p = purchase(9002L, "PO-2", 1L, "0",
                detail(100L, "可乐", 10, "3.00"));

        service.applyPurchaseStockIn(p);

        assertTrue(mapper.inserted.isEmpty(), "草稿采购单不应生成库存流水");
    }

    @Test
    void repeatedConfirm_doesNotDuplicateLedger() {
        FinPurchase p = purchase(9003L, "PO-3", 1L, "1",
                detail(100L, "可乐", 10, "3.00"));

        service.applyPurchaseStockIn(p);
        service.applyPurchaseStockIn(p);

        assertEquals(1, mapper.inserted.size(), "重复确认同一采购单不应重复生成入库流水");
        assertEquals(10, mapper.position(1L, 100L));
    }

    @Test
    void modifyPurchaseQuantity_generatesDeltaLedger() {
        FinPurchase p = purchase(9004L, "PO-4", 1L, "1", detail(100L, "可乐", 10, "3.00"));
        service.applyPurchaseStockIn(p);

        // 修改数量 10 -> 15
        FinPurchase modified = purchase(9004L, "PO-4", 1L, "1", detail(100L, "可乐", 15, "3.00"));
        service.applyPurchaseStockIn(modified);

        assertEquals(2, mapper.inserted.size(), "数量变更应生成差额流水");
        assertEquals(5, mapper.inserted.get(1).getChangeQuantity());
        assertEquals(15, mapper.position(1L, 100L), "库存对齐到新数量");
    }

    @Test
    void removeDetailOnEdit_reversesRemovedProduct() {
        FinPurchase p = purchase(9005L, "PO-5", 1L, "1",
                detail(100L, "可乐", 10, "3.00"),
                detail(200L, "雪碧", 5, "2.50"));
        service.applyPurchaseStockIn(p);

        // 编辑后删除雪碧明细，只剩可乐
        FinPurchase modified = purchase(9005L, "PO-5", 1L, "1", detail(100L, "可乐", 10, "3.00"));
        service.applyPurchaseStockIn(modified);

        assertEquals(0, mapper.position(1L, 200L), "被删除的明细应反向冲销到 0");
        assertEquals(10, mapper.position(1L, 100L), "保留明细库存不变");
    }

    @Test
    void deletePurchase_reversesAllStock() {
        FinPurchase p = purchase(9006L, "PO-6", 1L, "1", detail(100L, "可乐", 10, "3.00"));
        service.applyPurchaseStockIn(p);

        service.reversePurchaseStock(9006L, "PO-6", 1L, "admin");

        assertEquals(0, mapper.position(1L, 100L), "删除采购单应反向冲销库存到 0");
        assertEquals("PURCHASE_REVERSE", mapper.inserted.get(1).getChangeType());
    }

    private FinStockLedger ledgerOf(Long productId) {
        for (FinStockLedger l : mapper.inserted) {
            if (productId.equals(l.getProductId())) {
                return l;
            }
        }
        return null;
    }

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