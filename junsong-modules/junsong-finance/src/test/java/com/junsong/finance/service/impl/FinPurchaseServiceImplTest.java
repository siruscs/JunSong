package com.junsong.finance.service.impl;

import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinPurchaseDetail;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
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
        loginAsDept(1L);
        mapper = new FakeStockLedgerMapper();
        stockLedgerService = new FinStockLedgerServiceImpl();
        inject(FinStockLedgerServiceImpl.class, stockLedgerService, "finStockLedgerMapper", mapper);

        service = new FinPurchaseServiceImpl();
        inject(FinPurchaseServiceImpl.class, service, "finStockLedgerService", stockLedgerService);
        inject(FinPurchaseServiceImpl.class, service, "finStockLedgerMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    private void loginAsDept(Long deptId) {
        SecurityContextHolder.setUserId("2");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
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

    private FinPurchaseDetail giftDetail(Long productId, String name, int quantity) {
        FinPurchaseDetail detail = detail(productId, name, quantity, "0.00");
        detail.setIsGift("1");
        return detail;
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

    @Test
    void purchaseGift_increasesStockWithoutOverwritingPaidUnitCost() {
        FinPurchase p = purchase(9007L, "PO-GIFT-1", 1L, "1",
                detail(100L, "可乐", 10, "3.00"),
                giftDetail(100L, "可乐", 2));

        service.applyPurchaseStockIn(p);

        assertEquals(12, mapper.position(1L, 100L), "普通 10 + 赠品 2 应共同入库");
        assertEquals(new BigDecimal("3.00"), mapper.inserted.get(0).getUnitCost(),
                "赠品零价不得覆盖同商品正常采购单价");
    }

    @Test
    void modifyingOnlyPurchaseGift_writesDeltaAndDeleteReversesGiftInclusiveTotal() {
        FinPurchase original = purchase(9008L, "PO-GIFT-2", 1L, "1",
                detail(100L, "可乐", 10, "3.00"), giftDetail(100L, "可乐", 2));
        service.applyPurchaseStockIn(original);

        FinPurchase modified = purchase(9008L, "PO-GIFT-2", 1L, "1",
                detail(100L, "可乐", 10, "3.00"), giftDetail(100L, "可乐", 5));
        service.applyPurchaseStockIn(modified);

        assertEquals(3, mapper.inserted.get(1).getChangeQuantity(), "赠品 2 -> 5 只应追加 +3");
        assertEquals(15, mapper.position(1L, 100L));

        service.reversePurchaseStock(9008L, "PO-GIFT-2", 1L, "admin");

        assertEquals(-15, mapper.inserted.get(2).getChangeQuantity(), "删除应反向恢复含赠品的 15 件");
        assertEquals(0, mapper.position(1L, 100L));
    }

    @Test
    void acceptedGiftAlias_doesNotIncreasePurchaseAmount() throws Exception {
        FinPurchaseDetail gift = detail(100L, "可乐", 2, "3.00");
        gift.setIsGift("yes");
        FinPurchase purchase = purchase(9009L, "PO-GIFT-ALIAS", 1L, "1", gift);

        java.lang.reflect.Method calculate = FinPurchaseServiceImpl.class
                .getDeclaredMethod("calculatePurchaseAmountAndQuantity", FinPurchase.class);
        calculate.setAccessible(true);
        calculate.invoke(service, purchase);

        assertEquals("1", gift.getIsGift());
        assertEquals(BigDecimal.ZERO, purchase.getTotalAmount(), "yes 赠品不得计入采购金额");
        assertEquals(2, purchase.getTotalQuantity(), "赠品仍须计入实物总数量");
    }

    @Test
    void purchaseQuantityOverflow_failsClosedWithoutLedger() {
        FinPurchase purchase = purchase(9010L, "PO-GIFT-OVERFLOW", 1L, "1",
                detail(100L, "可乐", Integer.MAX_VALUE, "3.00"), giftDetail(100L, "可乐", 1));

        ServiceException error = assertThrows(ServiceException.class, () -> service.applyPurchaseStockIn(purchase));

        assertEquals("采购入库失败：同商品普通数量与赠品数量合计超出允许范围", error.getMessage());
        assertTrue(mapper.inserted.isEmpty());
    }

    @Test
    void purchaseFromUnauthorizedDept_failsClosedWithoutLedger() {
        FinPurchase purchase = purchase(9011L, "PO-OTHER-DEPT", 2L, "1",
                detail(100L, "可乐", 10, "3.00"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.applyPurchaseStockIn(purchase));

        assertEquals("无权操作该门店的进货库存", error.getMessage());
        assertTrue(mapper.inserted.isEmpty());
    }

    @Test
    void purchaseFromAnotherAuthorizedDept_isAllowed() throws Exception {
        inject(FinPurchaseServiceImpl.class, service, "authorizedDeptIdsOverride", Arrays.asList(1L, 2L));
        FinPurchase purchase = purchase(9012L, "PO-AUTHORIZED-DEPT", 2L, "1",
                detail(100L, "可乐", 10, "3.00"));

        service.applyPurchaseStockIn(purchase);

        assertEquals(10, mapper.position(2L, 100L));
    }

    @Test
    void adminPurchaseWithMissingDept_failsClosed() {
        SecurityContextHolder.setUserId("1");
        FinPurchase purchase = purchase(9013L, "PO-NO-DEPT", null, "1",
                detail(100L, "可乐", 10, "3.00"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.applyPurchaseStockIn(purchase));

        assertEquals("进货库存缺少门店上下文", error.getMessage());
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
            return 1L + ":" + deptId + ":" + productId;
        }

        int position(Long deptId, Long productId) {
            return positions.getOrDefault(key(deptId, productId), 0);
        }

        @Override
        public int insertPositionIfAbsent(Long tenantId, Long deptId, Long productId) {
            positions.putIfAbsent(tenantId + ":" + deptId + ":" + productId, 0);
            return 1;
        }

        @Override
        public Integer selectPositionQuantityForUpdate(Long tenantId, Long deptId, Long productId) {
            return positions.get(tenantId + ":" + deptId + ":" + productId);
        }

        @Override
        public Integer selectPositionQuantity(Long tenantId, Long deptId, Long productId) {
            return positions.get(tenantId + ":" + deptId + ":" + productId);
        }

        @Override
        public int updatePositionQuantity(Long tenantId, Long deptId, Long productId, Integer quantity) {
            positions.put(tenantId + ":" + deptId + ":" + productId, quantity);
            return 1;
        }

        @Override
        public Integer sumRecordedNet(Long tenantId, String referenceType, Long referenceId, Long productId) {
            int sum = 0;
            for (FinStockLedger l : inserted) {
                if (tenantId.equals(l.getTenantId())
                        && referenceType.equals(l.getReferenceType())
                        && referenceId.equals(l.getReferenceId())
                        && productId.equals(l.getProductId())) {
                    sum += l.getChangeQuantity();
                }
            }
            return sum;
        }

        @Override
        public List<Long> selectRecordedProductIds(Long tenantId, String referenceType, Long referenceId) {
            List<Long> ids = new ArrayList<>();
            for (FinStockLedger l : inserted) {
                if (tenantId.equals(l.getTenantId())
                        && referenceType.equals(l.getReferenceType())
                        && referenceId.equals(l.getReferenceId())
                        && !ids.contains(l.getProductId())) {
                    ids.add(l.getProductId());
                }
            }
            return ids;
        }

        @Override
        public int insertFinStockLedger(FinStockLedger ledger) {
            ledger.setLedgerId((long) (inserted.size() + 1));
            inserted.add(ledger);
            return 1;
        }

        // ---- R7-E 快照相关方法桩实现（本测试不涉及，仅为满足接口契约） ----
        @Override public com.junsong.finance.domain.vo.DailyFlowView sumDailyFlow(Long tenantId, java.time.LocalDate date, Long deptId, Long productId) { return null; }
        @Override public java.util.List<Long> selectSnapshotProductIds(Long tenantId, java.time.LocalDate date, Long deptId) { return java.util.List.of(); }
        @Override public com.junsong.finance.domain.FinStockSnapshot selectPreviousSnapshot(Long tenantId, java.time.LocalDate date, Long deptId, Long productId) { return null; }
        @Override public FinStockLedger selectFirstDailyLedger(Long tenantId, java.time.LocalDate date, Long deptId, Long productId) { return null; }
        @Override public FinStockLedger selectLastLedgerBeforeDate(Long tenantId, java.time.LocalDate date, Long deptId, Long productId) { return null; }
        @Override public java.util.List<com.junsong.finance.domain.vo.FinStockPositionView> selectAllTenantDeptScopesWithPosition() { return java.util.List.of(); }

        @Override
        public int upsertSnapshot(com.junsong.finance.domain.FinStockSnapshot snapshot) {
            return 0;
        }

        @Override
        public java.math.BigDecimal selectSaleOutUnitCost(Long tenantId, Long referenceId, Long productId) {
            return null;
        }

        @Override
        public int updateLedgerUnitCost(Long ledgerId, java.math.BigDecimal unitCost) {
            return 0;
        }

        @Override
        public int countByReferenceNo(Long tenantId, String referenceNo) {
            return 0;
        }

        @Override
        public Integer sumMovementAfterFreeze(Long tenantId, Long deptId, Long productId, java.util.Date freezeTime) {
            return 0;
        }

    }
}
