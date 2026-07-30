package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
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
 * FinSaleRecordServiceImpl 库存出库流水单元测试（对账模型 + 赠品合并出库）。
 * 使用手写 fake（无 Mockito）。
 * 关键约束：无商品/数量必须阻断，不允许造假扣减。
 */
class FinSaleRecordServiceImplTest {

    private FakeStockLedgerMapper mapper;
    private FinStockLedgerServiceImpl stockLedgerService;
    private FinSaleRecordServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        loginAsDept(1L);
        mapper = new FakeStockLedgerMapper();
        stockLedgerService = new FinStockLedgerServiceImpl();
        inject(FinStockLedgerServiceImpl.class, stockLedgerService, "finStockLedgerMapper", mapper);

        service = new FinSaleRecordServiceImpl();
        inject(FinSaleRecordServiceImpl.class, service, "finStockLedgerService", stockLedgerService);
        inject(FinSaleRecordServiceImpl.class, service, "finStockLedgerMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    private void loginAsDept(Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("sale-user");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername("sale-user");
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void inject(Class<?> clazz, Object target, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private FinSaleRecord sale(Long id, String no, Long deptId, Long productId, String name, Integer qty) {
        FinSaleRecord s = new FinSaleRecord();
        s.setSaleId(id);
        s.setSaleNo(no);
        s.setDeptId(deptId);
        s.setProductId(productId);
        s.setProductName(name);
        s.setSaleQuantity(qty);
        s.setGiftQuantity(0);
        return s;
    }

    private FinSaleRecord saleWithGift(Long id, String no, Long deptId, Long productId, String name,
                                        Integer qty, Integer giftQty) {
        FinSaleRecord s = sale(id, no, deptId, productId, name, qty);
        s.setGiftQuantity(giftQty);
        return s;
    }

    private void seedStock(Long deptId, Long productId, int qty) {
        stockLedgerService.reconcilePurchaseStock(1L, deptId, productId, "seed", 1L, "SEED", qty, new BigDecimal("1.00"), "sys");
        mapper.inserted.clear();
    }

    @Test
    void saleWithStock_generatesSaleOutLedger() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8001L, "SO-1", 1L, 100L, "可乐", 3);

        service.applySaleStockOut(s);

        assertEquals(1, mapper.inserted.size());
        FinStockLedger led = mapper.inserted.get(0);
        assertEquals("SALE_OUT", led.getChangeType());
        assertEquals(-3, led.getChangeQuantity());
        assertEquals(17, led.getAfterQuantity());
        assertEquals("SALE", led.getReferenceType());
        assertEquals(8001L, led.getReferenceId());
    }

    @Test
    void returnSaleWithNegativeQuantity_generatesInboundAdjustmentLedger() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8005L, "SO-RETURN-1", 1L, 100L, "可乐", -3);

        service.applySaleStockOut(s);

        assertEquals(1, mapper.inserted.size());
        assertEquals(3, mapper.inserted.get(0).getChangeQuantity());
        assertEquals(23, mapper.position(1L, 100L));
    }

    @Test
    void saleWithGift_deductsTotalQuantity() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = saleWithGift(8001L, "SO-1", 1L, 100L, "可乐", 3, 2);

        service.applySaleStockOut(s);

        assertEquals(1, mapper.inserted.size());
        assertEquals(-5, mapper.inserted.get(0).getChangeQuantity(), "出库量 = sale(3) + gift(2)");
        assertEquals(15, mapper.position(1L, 100L));
    }

    @Test
    void saleLedgerWithBlankOperator_fallsBackToCurrentUsername() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8001L, "SO-1", 1L, 100L, "可乐", 3);

        service.applySaleStockOut(s);

        assertEquals("sale-user", mapper.inserted.get(0).getCreateBy());
    }

    @Test
    void giftOnlySale_deductsGiftQuantity() {
        seedStock(1L, 100L, 10);
        FinSaleRecord s = saleWithGift(8001L, "SO-1", 1L, 100L, "可乐", 1, 5);

        service.applySaleStockOut(s);

        assertEquals(-6, mapper.inserted.get(0).getChangeQuantity(), "出库量 = sale(1) + gift(5)");
        assertEquals(4, mapper.position(1L, 100L));
    }

    @Test
    void saleWithoutProductId_isBlockedAndThrows() {
        FinSaleRecord s = sale(8002L, "SO-2", 1L, null, "未知", 3);

        assertThrows(ServiceException.class, () -> service.applySaleStockOut(s));
        assertTrue(mapper.inserted.isEmpty(), "无商品ID必须阻断，不允许造假扣减");
    }

    @Test
    void saleWithoutQuantity_isBlockedAndThrows() {
        FinSaleRecord s = sale(8003L, "SO-3", 1L, 100L, "可乐", null);

        assertThrows(ServiceException.class, () -> service.applySaleStockOut(s));
        assertTrue(mapper.inserted.isEmpty(), "无数量必须阻断，不允许造假扣减");
    }

    @Test
    void repeatedSale_doesNotDuplicateLedger() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8004L, "SO-4", 1L, 100L, "可乐", 3);

        service.applySaleStockOut(s);
        service.applySaleStockOut(s);

        assertEquals(1, mapper.inserted.size(), "重复出库同一销售单不应重复生成流水");
        assertEquals(17, mapper.position(1L, 100L));
    }

    @Test
    void modifySale_increasesQuantity_generatesOutDelta() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8005L, "SO-5", 1L, 100L, "可乐", 3);
        service.applySaleStockOut(s);

        FinSaleRecord s2 = sale(8005L, "SO-5", 1L, 100L, "可乐", 5);
        service.applySaleStockOut(s2);

        assertEquals(2, mapper.inserted.size());
        assertEquals(-2, mapper.inserted.get(1).getChangeQuantity(), "差额出库 2");
        assertEquals(15, mapper.position(1L, 100L));
    }

    @Test
    void modifySale_decreasesQuantity_generatesReverse() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8006L, "SO-6", 1L, 100L, "可乐", 5);
        service.applySaleStockOut(s);

        FinSaleRecord s2 = sale(8006L, "SO-6", 1L, 100L, "可乐", 2);
        service.applySaleStockOut(s2);

        assertEquals("SALE_REVERSE", mapper.inserted.get(1).getChangeType());
        assertEquals(3, mapper.inserted.get(1).getChangeQuantity(), "回补 3");
        assertEquals(18, mapper.position(1L, 100L));
    }

    @Test
    void modifySale_changesProduct_reversesOldAndDeductsNew() {
        seedStock(1L, 100L, 20);
        seedStock(1L, 200L, 20);
        FinSaleRecord s = sale(8008L, "SO-8", 1L, 100L, "可乐", 5);
        service.applySaleStockOut(s);
        assertEquals(15, mapper.position(1L, 100L));

        FinSaleRecord s2 = sale(8008L, "SO-8", 1L, 200L, "雪碧", 3);
        service.applySaleStockOut(s2);

        assertEquals(20, mapper.position(1L, 100L), "旧商品(可乐)历史出库应反向回补");
        assertEquals(17, mapper.position(1L, 200L), "新商品(雪碧)按新数量出库");
    }

    @Test
    void deleteSale_restoresStock() {
        seedStock(1L, 100L, 20);
        FinSaleRecord s = sale(8007L, "SO-7", 1L, 100L, "可乐", 5);
        service.applySaleStockOut(s);

        FinSaleRecord old = sale(8007L, "SO-7", 1L, 100L, "可乐", 5);
        old.setUpdateBy("admin");
        service.reverseSaleStock(old);

        assertEquals("SALE_REVERSE", mapper.inserted.get(1).getChangeType());
        assertEquals(5, mapper.inserted.get(1).getChangeQuantity(), "删除销售单回补全部库存");
        assertEquals(20, mapper.position(1L, 100L));
    }

    @Test
    void modifyingOnlySaleGift_writesDeltaAndDeleteRestoresGiftInclusiveTotal() {
        seedStock(1L, 100L, 30);
        FinSaleRecord original = saleWithGift(8009L, "SO-GIFT-1", 1L, 100L, "可乐", 8, 2);
        service.applySaleStockOut(original);

        FinSaleRecord modified = saleWithGift(8009L, "SO-GIFT-1", 1L, 100L, "可乐", 8, 5);
        service.applySaleStockOut(modified);

        assertEquals(-3, mapper.inserted.get(1).getChangeQuantity(), "赠品 2 -> 5 只应追加出库 3");
        assertEquals(17, mapper.position(1L, 100L));

        modified.setUpdateBy("admin");
        service.reverseSaleStock(modified);

        assertEquals(13, mapper.inserted.get(2).getChangeQuantity(), "删除应回补销售 8 + 赠品 5");
        assertEquals(30, mapper.position(1L, 100L));
    }

    @Test
    void saleAndGiftQuantityOverflow_failsClosedWithoutLedger() {
        seedStock(1L, 100L, 30);
        FinSaleRecord sale = saleWithGift(8010L, "SO-GIFT-OVERFLOW", 1L, 100L, "可乐",
                Integer.MAX_VALUE, 1);

        ServiceException error = assertThrows(ServiceException.class, () -> service.applySaleStockOut(sale));

        assertEquals("销售出库失败：销售数量与赠品数量合计超出允许范围", error.getMessage());
        assertTrue(mapper.inserted.isEmpty(), "整数溢出必须在写流水前失败关闭");
    }

    @Test
    void saleFromUnauthorizedDept_failsClosedWithoutLedger() {
        seedStock(1L, 100L, 20);
        mapper.inserted.clear();
        FinSaleRecord sale = saleWithGift(8011L, "SO-OTHER-DEPT", 2L, 100L, "可乐", 3, 2);

        ServiceException error = assertThrows(ServiceException.class, () -> service.applySaleStockOut(sale));

        assertEquals("无权操作该门店的销售库存", error.getMessage());
        assertTrue(mapper.inserted.isEmpty());
    }

    @Test
    void saleFromAnotherAuthorizedDept_isAllowed() throws Exception {
        inject(FinSaleRecordServiceImpl.class, service, "authorizedDeptIdsOverride", java.util.Arrays.asList(1L, 2L));
        seedStock(2L, 100L, 20);
        mapper.inserted.clear();
        FinSaleRecord sale = saleWithGift(8012L, "SO-AUTHORIZED-DEPT", 2L, 100L, "可乐", 3, 2);

        service.applySaleStockOut(sale);

        assertEquals(15, mapper.position(2L, 100L));
    }

    @Test
    void adminSaleWithMissingDept_failsClosed() {
        SecurityContextHolder.setUserId("1");
        FinSaleRecord sale = saleWithGift(8013L, "SO-NO-DEPT", null, 100L, "可乐", 3, 2);

        ServiceException error = assertThrows(ServiceException.class, () -> service.applySaleStockOut(sale));

        assertEquals("销售库存缺少门店上下文", error.getMessage());
    }

    private int position() {
        return mapper.position(1L, 100L);
    }

    // ==================== R9-D: member_id direct link ====================

    @Test
    void insert_sale_keeps_memberId_memberNo_memberName() {
        FinSaleRecord s = new FinSaleRecord();
        s.setSaleId(9001L);
        s.setSaleNo("SO-M1");
        s.setDeptId(1L);
        s.setProductId(100L);
        s.setProductName("可乐");
        s.setSaleQuantity(3);
        s.setGiftQuantity(0);
        s.setMemberId(42L);
        s.setMemberNo("MEM-001");
        s.setMemberName("张三");

        assertEquals(42L, s.getMemberId(), "memberId must be preserved");
        assertEquals("MEM-001", s.getMemberNo(), "memberNo must be preserved");
        assertEquals("张三", s.getMemberName(), "memberName must be preserved");
    }

    @Test
    void update_sale_keeps_member_fields() {
        FinSaleRecord s = new FinSaleRecord();
        s.setSaleId(9002L);
        s.setMemberId(10L);
        s.setMemberNo("MEM-010");
        s.setMemberName("李四");

        // Simulate update: change member fields
        s.setMemberId(20L);
        s.setMemberNo("MEM-020");
        s.setMemberName("王五");

        assertEquals(20L, s.getMemberId(), "memberId must update correctly");
        assertEquals("MEM-020", s.getMemberNo(), "memberNo must update correctly");
        assertEquals("王五", s.getMemberName(), "memberName must update correctly");

        // Null out member fields (unlinking member from sale)
        s.setMemberId(null);
        s.setMemberNo(null);
        s.setMemberName(null);

        assertNull(s.getMemberId(), "memberId can be set to null");
        assertNull(s.getMemberNo(), "memberNo can be set to null");
        assertNull(s.getMemberName(), "memberName can be set to null");
    }

    @Test
    void mapper_XML_includes_member_id_member_no_member_name() throws Exception {
        // Static check: verify the mapper XML contains member column mappings
        java.nio.file.Path xmlPath = java.nio.file.Paths.get(
                "src/main/resources/mapper/finance/FinSaleRecordMapper.xml");
        String xmlContent = new String(java.nio.file.Files.readAllBytes(xmlPath), "UTF-8");

        assertTrue(xmlContent.contains("member_id"),
                "Mapper XML must contain member_id column reference");
        assertTrue(xmlContent.contains("member_no"),
                "Mapper XML must contain member_no column reference");
        assertTrue(xmlContent.contains("member_name"),
                "Mapper XML must contain member_name column reference");

        // Verify resultMap has the mapping
        assertTrue(xmlContent.contains("property=\"memberId\""),
                "ResultMap must map memberId property");
        assertTrue(xmlContent.contains("property=\"memberNo\""),
                "ResultMap must map memberNo property");
        assertTrue(xmlContent.contains("property=\"memberName\""),
                "ResultMap must map memberName property");
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

        @Override
        public FinStockLedger selectByIdempotencyKey(Long tenantId, String idempotencyKey) {
            return null;
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
            for (FinStockLedger l : inserted) {
                if (tenantId.equals(l.getTenantId())
                        && "SALE".equals(l.getReferenceType())
                        && referenceId.equals(l.getReferenceId())
                        && productId.equals(l.getProductId())
                        && "SALE_OUT".equals(l.getChangeType())) {
                    return l.getUnitCost();
                }
            }
            return null;
        }

        @Override
        public int updateLedgerUnitCost(Long ledgerId, java.math.BigDecimal unitCost) {
            for (FinStockLedger l : inserted) {
                if (ledgerId != null && ledgerId.equals(l.getLedgerId())) {
                    l.setUnitCost(unitCost);
                    return 1;
                }
            }
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


    @Override
    public int countDownstreamLedgersAfterTime(Long tenantId, Long deptId, Long productId, java.util.Date afterTime) {
        return 0;
    }

}
}
