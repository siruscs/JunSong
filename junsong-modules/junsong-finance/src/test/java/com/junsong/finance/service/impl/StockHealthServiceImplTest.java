package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.StockHealthVO;
import com.junsong.finance.domain.vo.StockReconciliationResultVO;
import com.junsong.finance.domain.vo.StockReconciliationRowVO;
import com.junsong.finance.mapper.StockHealthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockHealthServiceImpl 单元测试。使用手写 fake mapper（无 Mockito）。
 *
 * <p>覆盖两类能力：
 * <ul>
 *   <li>checkHealth - 库存底座健康检查（按租户+门店范围）</li>
 *   <li>reconcileStock - 只读存量对账（四类异常检测 + 租户隔离 + fail-closed）</li>
 * </ul></p>
 */
class StockHealthServiceImplTest {

    private static final Long T1 = 1L;
    private static final Long T2 = 2L;

    private FakeStockHealthMapper mapper;
    private StockHealthServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new FakeStockHealthMapper();
        service = new StockHealthServiceImpl();
        Field field = StockHealthServiceImpl.class.getDeclaredField("stockHealthMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    // ==================== checkHealth 已有测试（签名更新） ====================

    @Test
    void healthy_whenNoIssues() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("HEALTHY", vo.getStatus());
        assertEquals(10L, vo.getLedgerCount());
        assertEquals(3L, vo.getSnapshotCount());
        assertEquals(0L, vo.getNegativeStockProductCount());
        assertTrue(vo.getIssues().isEmpty());
    }

    @Test
    void blocked_whenNegativeStockExists() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 2L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("BLOCKED", vo.getStatus());
        assertEquals(2L, vo.getNegativeStockProductCount());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "NEGATIVE_STOCK".equals(i.getType())));
        assertTrue(vo.getIssues().stream().anyMatch(i -> "HIGH".equals(i.getSeverity())));
    }

    @Test
    void warn_whenSnapshotEmptyButLedgerHasData() {
        mapper.ledger = 10L;
        mapper.snapshot = 0L;
        mapper.negative = 0L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("WARN", vo.getStatus());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_EMPTY".equals(i.getType())));
    }

    @Test
    void healthy_whenBothEmpty() {
        mapper.ledger = 0L;
        mapper.snapshot = 0L;
        mapper.negative = 0L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("HEALTHY", vo.getStatus(), "流水为空时快照为空不算 WARN");
        assertTrue(vo.getIssues().isEmpty());
    }

    @Test
    void warn_whenPositionsWithoutLedger() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;
        mapper.positionsWithoutLedger = 4L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("WARN", vo.getStatus());
        assertEquals(4L, vo.getProductsWithoutLedgerCount());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "POSITION_WITHOUT_LEDGER".equals(i.getType())));
    }

    @Test
    void blockedTakesPriorityOverWarn() {
        mapper.ledger = 10L;
        mapper.snapshot = 0L;
        mapper.negative = 1L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("BLOCKED", vo.getStatus(), "有阻断项时状态为 BLOCKED，即使同时存在 WARN 项");
        assertTrue(vo.getIssues().size() >= 2);
    }

    @Test
    void warn_whenSnapshotMissing() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;
        mapper.snapshotMissing = 5L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("WARN", vo.getStatus());
        assertEquals(5L, vo.getSnapshotMissingCount());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_MISSING".equals(i.getType())));
        assertTrue(vo.getIssues().stream().anyMatch(i -> "MEDIUM".equals(i.getSeverity())));
    }

    @Test
    void warn_whenSnapshotPositionMismatch() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;
        mapper.snapshotMismatch = 2L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("WARN", vo.getStatus());
        assertEquals(2L, vo.getSnapshotMismatchCount());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_POSITION_MISMATCH".equals(i.getType())));
    }

    @Test
    void newSnapshotRulesDoNotBlock() {
        // 仅快照相关 WARN 项，不应升级为 BLOCKED
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;
        mapper.snapshotMissing = 3L;
        mapper.snapshotMismatch = 1L;

        StockHealthVO vo = service.checkHealth(T1, null);

        assertEquals("WARN", vo.getStatus(), "快照缺失/不一致属于 WARN，不应阻断");
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_MISSING".equals(i.getType())));
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_POSITION_MISMATCH".equals(i.getType())));
        assertFalse(vo.getIssues().stream().anyMatch(i -> "HIGH".equals(i.getSeverity())), "不应出现 HIGH 级别");
    }

    @Test
    void checkHealth_failsClosedWhenTenantIdNull() {
        assertThrows(ServiceException.class, () -> service.checkHealth(null, null),
                "tenantId 为 null 时必须 fail-closed");
    }

    // ==================== reconcileStock 新增测试 ====================

    @Test
    void reconcile_detectsPositionWithoutLedger() {
        StockReconciliationRowVO row = row(T1, 10L, "门店A", 100L, "可乐", 0, 5, 5,
                "POSITION_WITHOUT_LEDGER", "结存存在但无流水记录");
        mapper.positionsWithoutLedgerRows = new ArrayList<>(Arrays.asList(row));

        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertEquals("WARN", result.getStatus());
        assertEquals(1, result.getTotalAnomalyCount());
        assertEquals(1, result.getAnomalyCounts().get("POSITION_WITHOUT_LEDGER"));
        StockReconciliationRowVO r = result.getRows().get(0);
        assertEquals(T1, r.getTenantId());
        assertEquals(10L, r.getDeptId());
        assertEquals(100L, r.getProductId());
        assertEquals("POSITION_WITHOUT_LEDGER", r.getAnomalyCode());
        assertEquals(0, r.getExpectedQuantity());
        assertEquals(5, r.getActualQuantity());
        assertEquals(5, r.getDiffQuantity());
    }

    @Test
    void reconcile_detectsLedgerPositionMismatch() {
        StockReconciliationRowVO row = row(T1, 10L, "门店A", 100L, "可乐", 8, 10, 2,
                "LEDGER_POSITION_MISMATCH", "流水累计与结存不一致");
        mapper.ledgerPositionMismatchRows = new ArrayList<>(Arrays.asList(row));

        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertEquals("WARN", result.getStatus());
        assertEquals(1, result.getTotalAnomalyCount());
        assertEquals(1, result.getAnomalyCounts().get("LEDGER_POSITION_MISMATCH"));
        StockReconciliationRowVO r = result.getRows().get(0);
        assertEquals("LEDGER_POSITION_MISMATCH", r.getAnomalyCode());
        assertEquals(8, r.getExpectedQuantity());
        assertEquals(10, r.getActualQuantity());
        assertEquals(2, r.getDiffQuantity());
    }

    @Test
    void reconcile_detectsSnapshotEquationMismatch() {
        StockReconciliationRowVO row = row(T1, 10L, "门店A", 100L, "可乐", 10, 12, 2,
                "SNAPSHOT_EQUATION_MISMATCH", "快照恒等式不成立");
        mapper.snapshotEquationMismatchRows = new ArrayList<>(Arrays.asList(row));

        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertEquals("WARN", result.getStatus());
        assertEquals(1, result.getTotalAnomalyCount());
        assertEquals(1, result.getAnomalyCounts().get("SNAPSHOT_EQUATION_MISMATCH"));
        StockReconciliationRowVO r = result.getRows().get(0);
        assertEquals("SNAPSHOT_EQUATION_MISMATCH", r.getAnomalyCode());
        assertEquals(10, r.getExpectedQuantity());
        assertEquals(12, r.getActualQuantity());
        assertEquals(2, r.getDiffQuantity());
    }

    @Test
    void reconcile_detectsLatestSnapshotMismatch() {
        StockReconciliationRowVO row = row(T1, 10L, "门店A", 100L, "可乐", 8, 10, 2,
                "LATEST_SNAPSHOT_MISMATCH", "最新快照与当前结存不一致");
        mapper.latestSnapshotMismatchRows = new ArrayList<>(Arrays.asList(row));

        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertEquals("WARN", result.getStatus());
        assertEquals(1, result.getTotalAnomalyCount());
        assertEquals(1, result.getAnomalyCounts().get("LATEST_SNAPSHOT_MISMATCH"));
        StockReconciliationRowVO r = result.getRows().get(0);
        assertEquals("LATEST_SNAPSHOT_MISMATCH", r.getAnomalyCode());
        assertEquals(8, r.getExpectedQuantity());
        assertEquals(10, r.getActualQuantity());
        assertEquals(2, r.getDiffQuantity());
    }

    @Test
    void reconcile_respectsTenantIsolation() {
        // 两个租户拥有相同的 deptId 和 productId，但属于不同租户
        StockReconciliationRowVO t1Row = row(T1, 10L, "门店A", 100L, "可乐", 0, 5, 5,
                "POSITION_WITHOUT_LEDGER", "T1 异常");
        StockReconciliationRowVO t2Row = row(T2, 10L, "门店A", 100L, "可乐", 0, 3, 3,
                "POSITION_WITHOUT_LEDGER", "T2 异常");
        mapper.positionsWithoutLedgerRows = new ArrayList<>(Arrays.asList(t1Row, t2Row));

        // 对 T1 对账时只应返回 T1 的行
        StockReconciliationResultVO t1Result = service.reconcileStock(T1, null);
        assertEquals(1, t1Result.getTotalAnomalyCount(), "T1 对账不应包含 T2 的行");
        assertEquals(T1, t1Result.getRows().get(0).getTenantId());

        // 对 T2 对账时只应返回 T2 的行
        StockReconciliationResultVO t2Result = service.reconcileStock(T2, null);
        assertEquals(1, t2Result.getTotalAnomalyCount(), "T2 对账不应包含 T1 的行");
        assertEquals(T2, t2Result.getRows().get(0).getTenantId());
    }

    @Test
    void reconcile_failsClosedWhenTenantIdNull() {
        assertThrows(ServiceException.class, () -> service.reconcileStock(null, null),
                "tenantId 为 null 时必须 fail-closed");
    }

    @Test
    void reconcile_isReadOnly() {
        // 对账完成后 fake mapper 不应记录任何写操作
        StockReconciliationRowVO row = row(T1, 10L, "门店A", 100L, "可乐", 0, 5, 5,
                "POSITION_WITHOUT_LEDGER", "结存存在但无流水记录");
        mapper.positionsWithoutLedgerRows = new ArrayList<>(Arrays.asList(row));

        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertNotNull(result);
        assertTrue(mapper.callLog.stream().allMatch(call -> call.startsWith("find") || call.startsWith("count")),
                "对账只应调用只读查询方法，不应有任何写操作；实际调用: " + mapper.callLog);
    }

    @Test
    void reconcile_healthyWhenNoAnomalies() {
        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertEquals("HEALTHY", result.getStatus());
        assertEquals(0, result.getTotalAnomalyCount());
        assertTrue(result.getRows().isEmpty());
        assertTrue(result.getAnomalyCounts().isEmpty());
    }

    @Test
    void reconcile_aggregatesMultipleAnomalyTypes() {
        mapper.positionsWithoutLedgerRows = new ArrayList<>(Arrays.asList(
            row(T1, 10L, "门店A", 100L, "可乐", 0, 5, 5, "POSITION_WITHOUT_LEDGER", "异常1"),
            row(T1, 20L, "门店B", 200L, "雪碧", 0, 3, 3, "POSITION_WITHOUT_LEDGER", "异常2")
        ));
        mapper.ledgerPositionMismatchRows = new ArrayList<>(Arrays.asList(
            row(T1, 10L, "门店A", 300L, "芬达", 8, 10, 2, "LEDGER_POSITION_MISMATCH", "异常3")
        ));

        StockReconciliationResultVO result = service.reconcileStock(T1, null);

        assertEquals("WARN", result.getStatus());
        assertEquals(3, result.getTotalAnomalyCount());
        assertEquals(2, result.getAnomalyCounts().get("POSITION_WITHOUT_LEDGER"));
        assertEquals(1, result.getAnomalyCounts().get("LEDGER_POSITION_MISMATCH"));
    }

    // ==================== 辅助方法 ====================

    private StockReconciliationRowVO row(Long tenantId, Long deptId, String deptName,
            Long productId, String productName,
            int expected, int actual, int diff,
            String anomalyCode, String safetyNote) {
        StockReconciliationRowVO r = new StockReconciliationRowVO();
        r.setTenantId(tenantId);
        r.setDeptId(deptId);
        r.setDeptName(deptName);
        r.setProductId(productId);
        r.setProductName(productName);
        r.setExpectedQuantity(expected);
        r.setActualQuantity(actual);
        r.setDiffQuantity(diff);
        r.setAnomalyCode(anomalyCode);
        r.setSafetyNote(safetyNote);
        return r;
    }

    // ==================== Fake Mapper ====================

    static class FakeStockHealthMapper implements StockHealthMapper {
        Long ledger = 0L;
        Long snapshot = 0L;
        Long negative = 0L;
        Long positionsWithoutLedger = 0L;
        Long snapshotMissing = 0L;
        Long snapshotMismatch = 0L;

        List<StockReconciliationRowVO> positionsWithoutLedgerRows = Collections.emptyList();
        List<StockReconciliationRowVO> ledgerPositionMismatchRows = Collections.emptyList();
        List<StockReconciliationRowVO> snapshotEquationMismatchRows = Collections.emptyList();
        List<StockReconciliationRowVO> latestSnapshotMismatchRows = Collections.emptyList();

        final List<String> callLog = new ArrayList<>();

        @Override public Long countLedger(Long tenantId, List<Long> deptIds) { return ledger; }
        @Override public Long countSnapshot(Long tenantId, List<Long> deptIds) { return snapshot; }
        @Override public Long countNegativeStockProducts(Long tenantId, List<Long> deptIds) { return negative; }
        @Override public Long countPositionsWithoutLedger(Long tenantId, List<Long> deptIds) { return positionsWithoutLedger; }
        @Override public Long countPositionsMissingYesterdaySnapshot(Long tenantId, List<Long> deptIds) { return snapshotMissing; }
        @Override public Long countSnapshotPositionMismatchToday(Long tenantId, List<Long> deptIds) { return snapshotMismatch; }

        @Override
        public List<StockReconciliationRowVO> findPositionsWithoutLedger(Long tenantId, List<Long> deptIds) {
            callLog.add("findPositionsWithoutLedger");
            return positionsWithoutLedgerRows.stream()
                    .filter(r -> tenantId.equals(r.getTenantId()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<StockReconciliationRowVO> findLedgerPositionMismatch(Long tenantId, List<Long> deptIds) {
            callLog.add("findLedgerPositionMismatch");
            return ledgerPositionMismatchRows.stream()
                    .filter(r -> tenantId.equals(r.getTenantId()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<StockReconciliationRowVO> findSnapshotEquationMismatch(Long tenantId, List<Long> deptIds) {
            callLog.add("findSnapshotEquationMismatch");
            return snapshotEquationMismatchRows.stream()
                    .filter(r -> tenantId.equals(r.getTenantId()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<StockReconciliationRowVO> findLatestSnapshotMismatch(Long tenantId, List<Long> deptIds) {
            callLog.add("findLatestSnapshotMismatch");
            return latestSnapshotMismatchRows.stream()
                    .filter(r -> tenantId.equals(r.getTenantId()))
                    .collect(Collectors.toList());
        }
    }
}
