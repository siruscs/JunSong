package com.junsong.finance.service.impl;

import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.domain.FinStockSnapshot;
import com.junsong.finance.domain.vo.DailyFlowView;
import com.junsong.finance.domain.vo.FinStockPositionView;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockSnapshotServiceImpl 单元测试。使用手写 fake mapper（无 Mockito）。
 */
class StockSnapshotServiceImplTest {

    private FakeFinStockLedgerMapper mapper;
    private StockSnapshotServiceImpl service;

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 7, 1);
    private static final Long DEPT_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new FakeFinStockLedgerMapper();
        service = new StockSnapshotServiceImpl();
        Field field = StockSnapshotServiceImpl.class.getDeclaredField("finStockLedgerMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @Test
    void rebuildCreatesSnapshotFromPosition() {
        // 两个 position
        mapper.addPosition(DEPT_ID, 10L, 100, "商品A");
        mapper.addPosition(DEPT_ID, 20L, 50, "商品B");
        // 商品A 当日入库30、出库10 → opening = 100 - 30 + 10 = 80
        mapper.putFlow(SNAPSHOT_DATE, DEPT_ID, 10L, 30, 10);
        // 商品B 当日无流水 → in/out=0，opening = 50
        mapper.putFlow(SNAPSHOT_DATE, DEPT_ID, 20L, 0, 0);

        int count = service.rebuildDailySnapshot(SNAPSHOT_DATE, DEPT_ID);

        assertEquals(2, count, "快照条数应等于 position 行数");
        assertEquals(2, mapper.snapshotStore.size(), "快照存储应有 2 条记录");
        assertEquals(2, mapper.insertCount, "两条均为新增");

        FinStockSnapshot a = mapper.snapshotStore.get(key(SNAPSHOT_DATE, DEPT_ID, 10L));
        assertNotNull(a);
        assertEquals(100, a.getQuantity(), "closing = position.quantity");
        assertEquals(30, a.getInQuantity());
        assertEquals(10, a.getOutQuantity());
        assertEquals(80, a.getOpeningQuantity(), "opening = closing - in + out");
        assertEquals("商品A", a.getProductName());

        FinStockSnapshot b = mapper.snapshotStore.get(key(SNAPSHOT_DATE, DEPT_ID, 20L));
        assertNotNull(b);
        assertEquals(50, b.getQuantity());
        assertEquals(0, b.getInQuantity());
        assertEquals(0, b.getOutQuantity());
        assertEquals(50, b.getOpeningQuantity(), "无流水时 opening = closing");
    }

    @Test
    void rebuildSameDateUpdatesExistingNoDuplicate() {
        mapper.addPosition(DEPT_ID, 10L, 100, "商品A");
        mapper.putFlow(SNAPSHOT_DATE, DEPT_ID, 10L, 20, 5);

        int firstRun = service.rebuildDailySnapshot(SNAPSHOT_DATE, DEPT_ID);
        int firstInserts = mapper.insertCount;
        int firstUpdates = mapper.updateCount;

        // 模拟当日又有变动：position 与流水变化后重跑
        mapper.updatePositionQuantity(DEPT_ID, 10L, 120);
        mapper.putFlow(SNAPSHOT_DATE, DEPT_ID, 10L, 40, 20);

        int secondRun = service.rebuildDailySnapshot(SNAPSHOT_DATE, DEPT_ID);

        assertEquals(1, firstRun, "首次生成 1 条快照");
        assertEquals(1, secondRun, "重跑仍返回 1 条（position 行数不变）");
        assertEquals(1, firstInserts, "首次全部为新增");
        assertEquals(0, firstUpdates, "首次无更新");
        assertEquals(1, mapper.snapshotStore.size(), "重跑不产生重复行，仍为 1 条");
        assertTrue(mapper.insertCount >= 1 && mapper.updateCount >= 1, "存在新增与更新两种 upsert 结果");

        FinStockSnapshot updated = mapper.snapshotStore.get(key(SNAPSHOT_DATE, DEPT_ID, 10L));
        assertNotNull(updated);
        assertEquals(120, updated.getQuantity(), "重跑后 closing 被更新为最新 position.quantity");
        assertEquals(40, updated.getInQuantity());
        assertEquals(20, updated.getOutQuantity());
        assertEquals(100, updated.getOpeningQuantity(), "opening = 120 - 40 + 20");
    }

    @Test
    void emptyPositionReturnsZero() {
        // 不添加任何 position
        int count = service.rebuildDailySnapshot(SNAPSHOT_DATE, DEPT_ID);

        assertEquals(0, count, "无 position 时返回 0 条快照");
        assertTrue(mapper.snapshotStore.isEmpty(), "不应写入任何快照");
        assertEquals(0, mapper.upsertCallCount, "不应调用 upsert");
    }

    private static String key(LocalDate date, Long deptId, Long productId) {
        return date + "|" + deptId + "|" + productId;
    }

    /**
     * 手写 fake：实现 FinStockLedgerMapper 全部方法，仅快照相关方法有真实逻辑。
     * upsertSnapshot 模拟唯一键 (snapshot_date, dept_id, product_id) 行为：
     * 不存在则插入（返回1），存在则更新（返回2），保证不产生重复行。
     */
    static class FakeFinStockLedgerMapper implements FinStockLedgerMapper {

        final List<FinStockPositionView> positions = new ArrayList<>();
        final Map<String, FinStockSnapshot> snapshotStore = new HashMap<>();
        final Map<String, DailyFlowView> flowStore = new HashMap<>();

        int insertCount = 0;
        int updateCount = 0;
        int upsertCallCount = 0;

        void addPosition(Long deptId, Long productId, int quantity, String productName) {
            FinStockPositionView p = new FinStockPositionView();
            p.setDeptId(deptId);
            p.setProductId(productId);
            p.setQuantity(quantity);
            p.setProductName(productName);
            positions.add(p);
        }

        void putFlow(LocalDate date, Long deptId, Long productId, int inQty, int outQty) {
            DailyFlowView v = new DailyFlowView();
            v.setInQuantity(inQty);
            v.setOutQuantity(outQty);
            flowStore.put(date + "|" + deptId + "|" + productId, v);
        }

        @Override
        public List<FinStockPositionView> selectPositionsByDept(Long deptId) {
            return positions;
        }

        @Override
        public DailyFlowView sumDailyFlow(LocalDate snapshotDate, Long deptId, Long productId) {
            return flowStore.get(snapshotDate + "|" + deptId + "|" + productId);
        }

        @Override
        public int upsertSnapshot(FinStockSnapshot snapshot) {
            upsertCallCount++;
            String k = snapshot.getSnapshotDate() + "|" + snapshot.getDeptId() + "|" + snapshot.getProductId();
            boolean existed = snapshotStore.containsKey(k);
            snapshotStore.put(k, snapshot);
            if (existed) {
                updateCount++;
                return 2;
            }
            insertCount++;
            return 1;
        }

        @Override
        public java.util.List<Long> selectAllDeptIdsWithPosition() {
            return java.util.Collections.emptyList();
        }

        // ---- 以下为非快照相关方法，提供桩实现 ----
        @Override
        public int insertPositionIfAbsent(Long deptId, Long productId) { return 0; }

        @Override
        public Integer selectPositionQuantityForUpdate(Long deptId, Long productId) {
            return positions.stream()
                    .filter(p -> p.getDeptId().equals(deptId) && p.getProductId().equals(productId))
                    .map(FinStockPositionView::getQuantity)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int updatePositionQuantity(Long deptId, Long productId, Integer quantity) {
            for (FinStockPositionView p : positions) {
                if (p.getDeptId().equals(deptId) && p.getProductId().equals(productId)) {
                    p.setQuantity(quantity);
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public Integer sumRecordedNet(String referenceType, Long referenceId, Long productId) { return 0; }

        @Override
        public List<Long> selectRecordedProductIds(String referenceType, Long referenceId) {
            return new ArrayList<>();
        }

        @Override
        public int insertFinStockLedger(FinStockLedger ledger) { return 0; }
    }
}
