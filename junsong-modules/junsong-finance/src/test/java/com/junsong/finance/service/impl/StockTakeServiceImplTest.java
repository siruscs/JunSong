package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.StockTakeRequest;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockTakeServiceImpl 单元测试（Task 8：旧接口收口）。
 *
 * 收口策略：fail-closed 迁移响应。
 * 旧 POST /stockTake 直接改库存的通道已被关闭，所有调用必须抛出 ServiceException，
 * 提示使用新工作流 /stocktakes。不得调用 insertFinStockLedger / updatePositionQuantity。
 *
 * 消费者：junsong-miniprogram/src/api/stockTake.js（Task 10 替换为 stocktake.js）。
 */
@ExtendWith(MockitoExtension.class)
class StockTakeServiceImplTest {

    @Mock
    private FinStockLedgerMapper finStockLedgerMapper;

    @Mock
    private FinProductMapper finProductMapper;

    @InjectMocks
    private StockTakeServiceImpl stockTakeService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @Test
    void recordStockTake_alwaysThrows_migrationNotice() {
        StockTakeRequest req = buildRequest();
        ServiceException ex = assertThrows(ServiceException.class,
                () -> stockTakeService.recordStockTake(req));
        assertTrue(ex.getMessage().contains("/stocktakes") || ex.getMessage().contains("工作流"),
                "旧接口必须返回迁移提示，实际消息: " + ex.getMessage());
    }

    @Test
    void recordStockTake_nullRequest_throwsMigrationNotice() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> stockTakeService.recordStockTake(null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void recordStockTake_neverMutatesStock() {
        assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(buildRequest()));
        verify(finStockLedgerMapper, never()).insertFinStockLedger(any());
        verify(finStockLedgerMapper, never()).updatePositionQuantity(anyLong(), anyLong(), anyLong(), anyInt());
        verify(finStockLedgerMapper, never()).insertPositionIfAbsent(anyLong(), anyLong(), anyLong());
        verify(finStockLedgerMapper, never()).selectPositionQuantityForUpdate(anyLong(), anyLong(), anyLong());
    }

    @Test
    void recordStockTake_neverReadsProduct() {
        assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(buildRequest()));
        verify(finProductMapper, never()).selectFinProductByProductIdAndDeptId(anyLong(), anyLong());
    }

    @Test
    void recordStockTake_neverChecksDuplicate() {
        assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(buildRequest()));
        verify(finStockLedgerMapper, never()).countByReferenceNo(anyLong(), anyString());
    }

    private StockTakeRequest buildRequest() {
        StockTakeRequest req = new StockTakeRequest();
        req.setTakeNo("TK-001");
        req.setDeptId(100L);
        req.setProductId(10L);
        req.setActualQuantity(50);
        req.setUnitCost(new BigDecimal("12.50"));
        req.setReason("季度盘点差异");
        return req;
    }
}
