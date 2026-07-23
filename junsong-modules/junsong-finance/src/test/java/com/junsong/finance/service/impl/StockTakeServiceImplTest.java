package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.vo.StockTakeRequest;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void rejectsNullRequest() {
        assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(null));
    }

    @Test
    void rejectsMissingTakeNo() {
        StockTakeRequest req = buildRequest();
        req.setTakeNo(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> stockTakeService.recordStockTake(req));
        assertTrue(ex.getMessage().contains("盘点单号"));
    }

    @Test
    void rejectsNegativeQuantity() {
        StockTakeRequest req = buildRequest();
        req.setActualQuantity(-1);
        assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(req));
    }

    @Test
    void rejectsMissingTenantContext() {
        TenantContext.setTenantId(null);
        StockTakeRequest req = buildRequest();
        assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(req));
    }

    @Test
    void rejectsDuplicateTakeNo() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(1);
        StockTakeRequest req = buildRequest();
        ServiceException ex = assertThrows(ServiceException.class,
                () -> stockTakeService.recordStockTake(req));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    void rejectsUnauthorizedDeptForNonAdmin() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(false);
            mocked.when(SecurityUtils::getDeptId).thenReturn(200L);

            StockTakeRequest req = buildRequest();
            req.setDeptId(100L);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> stockTakeService.recordStockTake(req));
            assertTrue(ex.getMessage().contains("无权盘点"));
        }
    }

    @Test
    void adminCanTakeAnyAuthorizedDept() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L))
                .thenReturn(buildProduct());
        when(finStockLedgerMapper.insertPositionIfAbsent(1L, 100L, 10L)).thenReturn(1);
        when(finStockLedgerMapper.selectPositionQuantityForUpdate(1L, 100L, 10L)).thenReturn(50);
        when(finStockLedgerMapper.updatePositionQuantity(1L, 100L, 10L, 55)).thenReturn(1);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");

            StockTakeRequest req = buildRequest();
            req.setActualQuantity(55);
            stockTakeService.recordStockTake(req);
        }
        verify(finStockLedgerMapper).insertFinStockLedger(any());
        verify(finStockLedgerMapper).updatePositionQuantity(1L, 100L, 10L, 55);
    }

    @Test
    void rejectsProductNotInDept() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L)).thenReturn(null);

            StockTakeRequest req = buildRequest();
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> stockTakeService.recordStockTake(req));
            assertTrue(ex.getMessage().contains("商品不存在或无权访问"));
        }
    }

    @Test
    void rejectsGainWithoutReason() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L))
                .thenReturn(buildProduct());
        when(finStockLedgerMapper.insertPositionIfAbsent(1L, 100L, 10L)).thenReturn(1);
        when(finStockLedgerMapper.selectPositionQuantityForUpdate(1L, 100L, 10L)).thenReturn(50);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");

            StockTakeRequest req = buildRequest();
            req.setActualQuantity(55);
            req.setReason(null);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> stockTakeService.recordStockTake(req));
            assertTrue(ex.getMessage().contains("原因"));
        }
    }

    @Test
    void writesGainLedgerWhenActualExceedsCurrent() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L))
                .thenReturn(buildProduct());
        when(finStockLedgerMapper.insertPositionIfAbsent(1L, 100L, 10L)).thenReturn(1);
        when(finStockLedgerMapper.selectPositionQuantityForUpdate(1L, 100L, 10L)).thenReturn(50);
        when(finStockLedgerMapper.updatePositionQuantity(1L, 100L, 10L, 55)).thenReturn(1);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");

            StockTakeRequest req = buildRequest();
            req.setActualQuantity(55);
            stockTakeService.recordStockTake(req);
        }

        verify(finStockLedgerMapper).insertFinStockLedger(argThat(ledger ->
                "STOCK_TAKE_GAIN".equals(ledger.getChangeType())
                && ledger.getChangeQuantity() == 5
                && ledger.getBeforeQuantity() == 50
                && ledger.getAfterQuantity() == 55
                && "STOCK_TAKE".equals(ledger.getReferenceType())
                && "TK-001".equals(ledger.getReferenceNo())));
    }

    @Test
    void writesLossLedgerWhenActualLessThanCurrent() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L))
                .thenReturn(buildProduct());
        when(finStockLedgerMapper.insertPositionIfAbsent(1L, 100L, 10L)).thenReturn(1);
        when(finStockLedgerMapper.selectPositionQuantityForUpdate(1L, 100L, 10L)).thenReturn(50);
        when(finStockLedgerMapper.updatePositionQuantity(1L, 100L, 10L, 48)).thenReturn(1);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");

            StockTakeRequest req = buildRequest();
            req.setActualQuantity(48);
            stockTakeService.recordStockTake(req);
        }

        verify(finStockLedgerMapper).insertFinStockLedger(argThat(ledger ->
                "STOCK_TAKE_LOSS".equals(ledger.getChangeType())
                && ledger.getChangeQuantity() == -2
                && ledger.getBeforeQuantity() == 50
                && ledger.getAfterQuantity() == 48));
    }

    @Test
    void noOpWhenActualEqualsCurrent() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L))
                .thenReturn(buildProduct());
        when(finStockLedgerMapper.insertPositionIfAbsent(1L, 100L, 10L)).thenReturn(1);
        when(finStockLedgerMapper.selectPositionQuantityForUpdate(1L, 100L, 10L)).thenReturn(50);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);

            StockTakeRequest req = buildRequest();
            req.setActualQuantity(50);
            Long ledgerId = stockTakeService.recordStockTake(req);
            assertEquals(0L, ledgerId);
        }
        verify(finStockLedgerMapper, never()).insertFinStockLedger(any());
        verify(finStockLedgerMapper, never()).updatePositionQuantity(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void rollsBackWhenPositionUpdateFails() {
        when(finStockLedgerMapper.countByReferenceNo(1L, "TK-001")).thenReturn(0);
        when(finProductMapper.selectFinProductByProductIdAndDeptId(10L, 100L))
                .thenReturn(buildProduct());
        when(finStockLedgerMapper.insertPositionIfAbsent(1L, 100L, 10L)).thenReturn(1);
        when(finStockLedgerMapper.selectPositionQuantityForUpdate(1L, 100L, 10L)).thenReturn(50);
        when(finStockLedgerMapper.updatePositionQuantity(1L, 100L, 10L, 55)).thenReturn(0);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");

            StockTakeRequest req = buildRequest();
            req.setActualQuantity(55);
            assertThrows(ServiceException.class, () -> stockTakeService.recordStockTake(req));
        }
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

    private FinProduct buildProduct() {
        FinProduct p = new FinProduct();
        p.setProductId(10L);
        p.setProductName("测试商品");
        p.setDeptId(100L);
        return p;
    }
}
