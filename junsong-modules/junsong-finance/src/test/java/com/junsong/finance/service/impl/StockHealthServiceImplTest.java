package com.junsong.finance.service.impl;

import com.junsong.finance.domain.vo.StockHealthVO;
import com.junsong.finance.mapper.StockHealthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockHealthServiceImpl 单元测试。使用手写 fake mapper（无 Mockito）。
 */
class StockHealthServiceImplTest {

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

    @Test
    void healthy_whenNoIssues() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;

        StockHealthVO vo = service.checkHealth();

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

        StockHealthVO vo = service.checkHealth();

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

        StockHealthVO vo = service.checkHealth();

        assertEquals("WARN", vo.getStatus());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_EMPTY".equals(i.getType())));
    }

    @Test
    void healthy_whenBothEmpty() {
        mapper.ledger = 0L;
        mapper.snapshot = 0L;
        mapper.negative = 0L;

        StockHealthVO vo = service.checkHealth();

        assertEquals("HEALTHY", vo.getStatus(), "流水为空时快照为空不算 WARN");
        assertTrue(vo.getIssues().isEmpty());
    }

    @Test
    void warn_whenPositionsWithoutLedger() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;
        mapper.positionsWithoutLedger = 4L;

        StockHealthVO vo = service.checkHealth();

        assertEquals("WARN", vo.getStatus());
        assertEquals(4L, vo.getProductsWithoutLedgerCount());
        assertTrue(vo.getIssues().stream().anyMatch(i -> "POSITION_WITHOUT_LEDGER".equals(i.getType())));
    }

    @Test
    void blockedTakesPriorityOverWarn() {
        mapper.ledger = 10L;
        mapper.snapshot = 0L;
        mapper.negative = 1L;

        StockHealthVO vo = service.checkHealth();

        assertEquals("BLOCKED", vo.getStatus(), "有阻断项时状态为 BLOCKED，即使同时存在 WARN 项");
        assertTrue(vo.getIssues().size() >= 2);
    }

    @Test
    void warn_whenSnapshotMissing() {
        mapper.ledger = 10L;
        mapper.snapshot = 3L;
        mapper.negative = 0L;
        mapper.snapshotMissing = 5L;

        StockHealthVO vo = service.checkHealth();

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

        StockHealthVO vo = service.checkHealth();

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

        StockHealthVO vo = service.checkHealth();

        assertEquals("WARN", vo.getStatus(), "快照缺失/不一致属于 WARN，不应阻断");
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_MISSING".equals(i.getType())));
        assertTrue(vo.getIssues().stream().anyMatch(i -> "SNAPSHOT_POSITION_MISMATCH".equals(i.getType())));
        assertFalse(vo.getIssues().stream().anyMatch(i -> "HIGH".equals(i.getSeverity())), "不应出现 HIGH 级别");
    }

    static class FakeStockHealthMapper implements StockHealthMapper {
        Long ledger = 0L;
        Long snapshot = 0L;
        Long negative = 0L;
        Long positionsWithoutLedger = 0L;
        Long snapshotMissing = 0L;
        Long snapshotMismatch = 0L;

        @Override public Long countLedger() { return ledger; }
        @Override public Long countSnapshot() { return snapshot; }
        @Override public Long countNegativeStockProducts() { return negative; }
        @Override public Long countPositionsWithoutLedger() { return positionsWithoutLedger; }
        @Override public Long countPositionsMissingYesterdaySnapshot() { return snapshotMissing; }
        @Override public Long countSnapshotPositionMismatchToday() { return snapshotMismatch; }
    }
}