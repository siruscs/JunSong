package com.junsong.finance.service.impl;

import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.domain.FinStockSnapshot;
import com.junsong.finance.domain.vo.DailyFlowView;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StockSnapshotServiceImplTest {
    private static final long TENANT_1 = 1L;
    private static final long TENANT_2 = 2L;
    private static final long DEPT = 10L;
    private static final long PRODUCT = 100L;
    private static final LocalDate DAY = LocalDate.of(2026, 7, 1);

    private FakeMapper mapper;
    private StockSnapshotServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new FakeMapper();
        service = new StockSnapshotServiceImpl();
        Field field = StockSnapshotServiceImpl.class.getDeclaredField("finStockLedgerMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @Test
    void isolatesTenantsWithSameDepartmentAndProduct() {
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "PURCHASE_IN", 12, 0, 12);
        mapper.addLedger(TENANT_2, DAY, DEPT, PRODUCT, "PURCHASE_IN", 5, 0, 5);

        service.rebuildDailySnapshot(TENANT_1, DAY, DEPT);
        service.rebuildDailySnapshot(TENANT_2, DAY, DEPT);

        assertEquals(12, mapper.snapshot(TENANT_1, DAY, DEPT, PRODUCT).getQuantity());
        assertEquals(5, mapper.snapshot(TENANT_2, DAY, DEPT, PRODUCT).getQuantity());
        assertEquals(2, mapper.snapshots.size());
    }

    @Test
    void carriesPreviousClosingAcrossNoFlowDayAndRebuildsChronologically() {
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "PURCHASE_IN", 12, 0, 12);
        service.rebuildDailySnapshot(TENANT_1, DAY, DEPT);
        service.rebuildDailySnapshot(TENANT_1, DAY.plusDays(1), DEPT);
        mapper.addLedger(TENANT_1, DAY.plusDays(2), DEPT, PRODUCT, "SALE_OUT", -10, 12, 2);
        service.rebuildDailySnapshot(TENANT_1, DAY.plusDays(2), DEPT);

        FinStockSnapshot noFlow = mapper.snapshot(TENANT_1, DAY.plusDays(1), DEPT, PRODUCT);
        assertEquals(12, noFlow.getOpeningQuantity());
        assertEquals(12, noFlow.getQuantity());
        FinStockSnapshot third = mapper.snapshot(TENANT_1, DAY.plusDays(2), DEPT, PRODUCT);
        assertEquals(12, third.getOpeningQuantity());
        assertEquals(10, third.getOutQuantity());
        assertEquals(2, third.getQuantity());
    }

    @Test
    void includesGiftPhysicalQuantitiesAndClassifiesReversals() {
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "PURCHASE_IN", 12, 0, 12); // 10 + gift 2
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "SALE_OUT", -10, 12, 2); // 8 + gift 2
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "SALE_REVERSE", 3, 2, 5);
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "PURCHASE_REVERSE", -2, 5, 3);

        service.rebuildDailySnapshot(TENANT_1, DAY, DEPT);

        FinStockSnapshot snapshot = mapper.snapshot(TENANT_1, DAY, DEPT, PRODUCT);
        assertEquals(0, snapshot.getOpeningQuantity());
        assertEquals(15, snapshot.getInQuantity(), "purchase in and sale reversal are physical inbound");
        assertEquals(12, snapshot.getOutQuantity(), "sale out and purchase reversal are physical outbound");
        assertEquals(3, snapshot.getQuantity());
    }

    @Test
    void historicalDateNeverUsesCurrentPositionAsClosing() {
        mapper.currentPosition = 999;
        mapper.addLedger(TENANT_1, DAY, DEPT, PRODUCT, "PURCHASE_IN", 12, 0, 12);

        service.rebuildDailySnapshot(TENANT_1, DAY, DEPT);

        assertEquals(12, mapper.snapshot(TENANT_1, DAY, DEPT, PRODUCT).getQuantity());
        assertFalse(mapper.positionRead, "historical rebuild must replay ledger, not current position");
    }

    @Test
    void usesLastLedgerClosingWhenPriorDayHasLedgerButNoSnapshotAndTargetDayHasNoFlow() {
        mapper.addLedger(TENANT_1, DAY.minusDays(1), DEPT, PRODUCT, "PURCHASE_IN", 12, 0, 12);

        service.rebuildDailySnapshot(TENANT_1, DAY, DEPT);

        FinStockSnapshot snapshot = mapper.snapshot(TENANT_1, DAY, DEPT, PRODUCT);
        assertEquals(12, snapshot.getOpeningQuantity());
        assertEquals(12, snapshot.getQuantity());
    }

    @Test
    void rejectsMissingScope() {
        assertThrows(IllegalArgumentException.class, () -> service.rebuildDailySnapshot(null, DAY, DEPT));
        assertThrows(IllegalArgumentException.class, () -> service.rebuildDailySnapshot(TENANT_1, null, DEPT));
        assertThrows(IllegalArgumentException.class, () -> service.rebuildDailySnapshot(TENANT_1, DAY, null));
    }

    static class FakeMapper implements FinStockLedgerMapper {
        final List<FinStockLedger> ledgers = new ArrayList<>();
        final Map<String, FinStockSnapshot> snapshots = new HashMap<>();
        boolean positionRead;
        int currentPosition;

        void addLedger(long tenant, LocalDate date, long dept, long product, String type, int change, int before, int after) {
            FinStockLedger row = new FinStockLedger();
            row.setTenantId(tenant); row.setDeptId(dept); row.setProductId(product); row.setProductName("商品");
            row.setChangeType(type); row.setChangeQuantity(change); row.setBeforeQuantity(before); row.setAfterQuantity(after);
            row.setCreateTime(java.sql.Timestamp.valueOf(date.atTime(12, 0)));
            ledgers.add(row);
        }

        FinStockSnapshot snapshot(long tenant, LocalDate date, long dept, long product) {
            return snapshots.get(key(tenant, date, dept, product));
        }

        @Override public List<Long> selectSnapshotProductIds(Long tenantId, LocalDate date, Long deptId) {
            Set<Long> ids = new TreeSet<>();
            ledgers.stream().filter(l -> Objects.equals(tenantId,l.getTenantId()) && Objects.equals(deptId,l.getDeptId())
                    && !((java.sql.Timestamp) l.getCreateTime()).toLocalDateTime().toLocalDate().isAfter(date)).forEach(l -> ids.add(l.getProductId()));
            snapshots.values().stream().filter(s -> Objects.equals(tenantId,s.getTenantId()) && Objects.equals(deptId,s.getDeptId())
                    && s.getSnapshotDate().isBefore(date)).forEach(s -> ids.add(s.getProductId()));
            return new ArrayList<>(ids);
        }
        @Override public FinStockSnapshot selectPreviousSnapshot(Long tenantId, LocalDate date, Long deptId, Long productId) {
            return snapshots.values().stream().filter(s -> Objects.equals(tenantId,s.getTenantId()) && Objects.equals(deptId,s.getDeptId())
                    && Objects.equals(productId,s.getProductId()) && s.getSnapshotDate().isBefore(date))
                    .max(Comparator.comparing(FinStockSnapshot::getSnapshotDate)).orElse(null);
        }
        @Override public FinStockLedger selectFirstDailyLedger(Long tenantId, LocalDate date, Long deptId, Long productId) {
            return ledgers.stream().filter(l -> Objects.equals(tenantId,l.getTenantId()) && Objects.equals(deptId,l.getDeptId())
                    && Objects.equals(productId,l.getProductId()) && ((java.sql.Timestamp) l.getCreateTime()).toLocalDateTime().toLocalDate().equals(date)).findFirst().orElse(null);
        }
        @Override public FinStockLedger selectLastLedgerBeforeDate(Long tenantId, LocalDate date, Long deptId, Long productId) {
            return ledgers.stream().filter(l -> Objects.equals(tenantId,l.getTenantId()) && Objects.equals(deptId,l.getDeptId())
                    && Objects.equals(productId,l.getProductId()) && ((java.sql.Timestamp) l.getCreateTime()).toLocalDateTime().toLocalDate().isBefore(date))
                    .max(Comparator.comparing(FinStockLedger::getCreateTime)).orElse(null);
        }
        @Override public DailyFlowView sumDailyFlow(Long tenantId, LocalDate date, Long deptId, Long productId) {
            int in = 0, out = 0;
            for (FinStockLedger l : ledgers) if (Objects.equals(tenantId,l.getTenantId()) && Objects.equals(deptId,l.getDeptId())
                    && Objects.equals(productId,l.getProductId()) && ((java.sql.Timestamp) l.getCreateTime()).toLocalDateTime().toLocalDate().equals(date)) {
                if (Set.of("PURCHASE_IN","SALE_REVERSE").contains(l.getChangeType())) in += l.getChangeQuantity();
                if (Set.of("SALE_OUT","PURCHASE_REVERSE").contains(l.getChangeType())) out += -l.getChangeQuantity();
            }
            DailyFlowView result = new DailyFlowView(); result.setInQuantity(in); result.setOutQuantity(out); return result;
        }
        @Override public int upsertSnapshot(FinStockSnapshot s) { snapshots.put(key(s.getTenantId(),s.getSnapshotDate(),s.getDeptId(),s.getProductId()),s); return 1; }
        private static String key(long t, LocalDate d, long dept, long product) { return t+"|"+d+"|"+dept+"|"+product; }

        @Override public int insertPositionIfAbsent(Long a,Long b,Long c){return 0;}
        @Override public Integer selectPositionQuantityForUpdate(Long a,Long b,Long c){positionRead=true;return currentPosition;}
        @Override public Integer selectPositionQuantity(Long a,Long b,Long c){return currentPosition;}
        @Override public int updatePositionQuantity(Long a,Long b,Long c,Integer d){return 0;}
        @Override public Integer sumRecordedNet(Long a,String b,Long c,Long d){return 0;}
        @Override public List<Long> selectRecordedProductIds(Long a,String b,Long c){return List.of();}
        @Override public int insertFinStockLedger(FinStockLedger l){return 0;}
        @Override public List<com.junsong.finance.domain.vo.FinStockPositionView> selectAllTenantDeptScopesWithPosition(){return List.of();}
        @Override public java.math.BigDecimal selectSaleOutUnitCost(Long a, Long b, Long c){return null;}
        @Override public int updateLedgerUnitCost(Long a, java.math.BigDecimal b){return 0;}
        @Override public int countByReferenceNo(Long a, String b){return 0;}
    }
}
