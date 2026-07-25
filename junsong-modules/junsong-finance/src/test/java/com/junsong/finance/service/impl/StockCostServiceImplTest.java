package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinStockCostLayer;
import com.junsong.finance.domain.FinStockCostLedger;
import com.junsong.finance.mapper.FinStockCostLayerMapper;
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
 * StockCostServiceImpl 单元测试（移动加权平均法）。
 * 使用手写 fake mapper（无 Mockito），内存维护成本层与成本流水。
 *
 * 核心场景：
 * - 普通采购与赠品同单：10 件单价 20 + 2 件赠品金额 0 → 12 件 200 元，平均成本 16.666667
 * - 销售 8 + 赠品 2 共出库 10，销售成本按 10 件固化，赠品也消耗成本但不产生收入
 * - 舍入（单位成本 6 位，金额 2 位，HALF_UP）
 * - 负库存策略、销售冲销按原成本回补、采购冲销、租户隔离
 */
class StockCostServiceImplTest {

    private static final Long T1 = 1L;
    private static final Long T2 = 2L;

    private FakeCostLayerMapper mapper;
    private StockCostServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new FakeCostLayerMapper();
        service = new StockCostServiceImpl();
        Field field = StockCostServiceImpl.class.getDeclaredField("costLayerMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @Test
    void purchaseInboundWithGifts_computesWeightedAverage() {
        // 普通 10 件单价 20 + 赠品 2 件金额 0 = 12 件 200 元
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");

        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(12, layer.getStockQuantity());
        assertEquals(0, new BigDecimal("200.00").compareTo(layer.getStockAmount()));
        assertEquals(0, new BigDecimal("16.666667").compareTo(layer.getAvgUnitCost()),
                "移动加权平均成本 = 200 / 12 = 16.666667");
    }

    @Test
    void purchaseInboundTwice_recomputesWeightedAverage() {
        service.applyPurchaseInbound(T1, 1L, 100L, 10, new BigDecimal("100.00"), 9001L, "alice");
        service.applyPurchaseInbound(T1, 1L, 100L, 5, new BigDecimal("150.00"), 9002L, "alice");

        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(15, layer.getStockQuantity());
        assertEquals(0, new BigDecimal("250.00").compareTo(layer.getStockAmount()));
        assertEquals(0, new BigDecimal("16.666667").compareTo(layer.getAvgUnitCost()),
                "新平均成本 = (100 + 150) / 15 = 16.666667");
    }

    @Test
    void saleOutboundSolidifiesCostAtCurrentAvg() {
        // 准备：12 件 200 元，平均 16.666667
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");

        BigDecimal solidified = service.applySaleOutbound(T1, 1L, 100L, 10, false, 8001L, "bob");

        assertEquals(0, new BigDecimal("16.666667").compareTo(solidified),
                "销售出库固化当时平均成本");
        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(2, layer.getStockQuantity());
        // 销售成本 = 16.666667 * 10 = 166.67（2位 HALF_UP）
        // 剩余金额 = 200.00 - 166.67 = 33.33
        assertEquals(0, new BigDecimal("33.33").compareTo(layer.getStockAmount()));
        assertEquals(0, new BigDecimal("16.666667").compareTo(layer.getAvgUnitCost()),
                "销售出库不改变平均成本");
    }

    @Test
    void saleReversalRestoresAtOriginalCost() {
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");
        BigDecimal original = service.applySaleOutbound(T1, 1L, 100L, 10, false, 8001L, "bob");
        // 剩余：2 件 33.33 元

        service.reverseSaleOutbound(T1, 1L, 100L, 10, original, 8001L, "bob");

        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(12, layer.getStockQuantity());
        // 33.33 + (16.666667 * 10 = 166.67) = 200.00
        assertEquals(0, new BigDecimal("200.00").compareTo(layer.getStockAmount()),
                "销售冲销按原固化成本回补，金额恢复为 200.00");
    }

    @Test
    void purchaseReversalReversesAtCurrentAvg() {
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");

        service.reversePurchaseInbound(T1, 1L, 100L, 12, 9001L, "alice");

        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(0, layer.getStockQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(layer.getStockAmount()),
                "采购冲销后数量和金额归零");
    }

    @Test
    void saleExceedingStock_throws() {
        service.applyPurchaseInbound(T1, 1L, 100L, 5, new BigDecimal("100.00"), 9001L, "alice");

        assertThrows(ServiceException.class,
                () -> service.applySaleOutbound(T1, 1L, 100L, 10, false, 8001L, "bob"));
    }

    @Test
    void saleExceedingStockWhenAllowed_negativesOk() {
        service.applyPurchaseInbound(T1, 1L, 100L, 5, new BigDecimal("100.00"), 9001L, "alice");

        BigDecimal solidified = service.applySaleOutbound(T1, 1L, 100L, 10, true, 8001L, "bob");

        assertEquals(0, new BigDecimal("20.000000").compareTo(solidified));
        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(-5, layer.getStockQuantity(), "允许负库存时成本层数量可以为负");
    }

    @Test
    void roundingUnitCostScale6AmountScale2() {
        // 3 件 100 元 → 平均 33.333333
        service.applyPurchaseInbound(T1, 1L, 100L, 3, new BigDecimal("100.00"), 9001L, "alice");
        FinStockCostLayer layer = mapper.costLayer(T1, 1L, 100L);
        assertEquals(0, new BigDecimal("33.333333").compareTo(layer.getAvgUnitCost()));
        assertEquals(0, new BigDecimal("100.00").compareTo(layer.getStockAmount()));

        // 销售 1 件 → 成本 = 33.333333 * 1 = 33.33（2位）
        BigDecimal solidified = service.applySaleOutbound(T1, 1L, 100L, 1, false, 8001L, "bob");
        assertEquals(0, new BigDecimal("33.333333").compareTo(solidified));
        layer = mapper.costLayer(T1, 1L, 100L);
        // 剩余 = 100.00 - 33.33 = 66.67
        assertEquals(0, new BigDecimal("66.67").compareTo(layer.getStockAmount()));
        assertEquals(2, layer.getStockQuantity());
    }

    @Test
    void missingTenant_failsClosed() {
        assertThrows(ServiceException.class,
                () -> service.applyPurchaseInbound(null, 1L, 100L, 10, new BigDecimal("200.00"), 9001L, "a"));
        assertThrows(ServiceException.class,
                () -> service.applySaleOutbound(null, 1L, 100L, 5, false, 8001L, "b"));
        assertTrue(mapper.costLedgers.isEmpty(), "租户缺失必须失败关闭，不得写入任何成本流水");
    }

    @Test
    void twoTenantsIsolated() {
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");
        service.applyPurchaseInbound(T2, 1L, 100L, 10, new BigDecimal("150.00"), 9002L, "carol");

        assertEquals(0, new BigDecimal("16.666667").compareTo(mapper.costLayer(T1, 1L, 100L).getAvgUnitCost()));
        assertEquals(0, new BigDecimal("15.000000").compareTo(mapper.costLayer(T2, 1L, 100L).getAvgUnitCost()));

        // 租户1出库不影响租户2
        service.applySaleOutbound(T1, 1L, 100L, 5, false, 8001L, "bob");
        assertEquals(0, new BigDecimal("15.000000").compareTo(mapper.costLayer(T2, 1L, 100L).getAvgUnitCost()),
                "租户2平均成本不受租户1出库影响");
    }

    @Test
    void costLedgerWrittenForPurchaseInbound() {
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");

        assertEquals(1, mapper.costLedgers.size());
        FinStockCostLedger cl = mapper.costLedgers.get(0);
        assertEquals("PURCHASE", cl.getSourceType());
        assertEquals("COST_IN", cl.getCostChangeType());
        assertEquals(12, cl.getQuantity());
        assertEquals(0, new BigDecimal("16.666667").compareTo(cl.getUnitCost()));
        assertEquals(0, new BigDecimal("200.00").compareTo(cl.getAmount()));
        assertEquals("alice", cl.getOperator());
    }

    @Test
    void costLedgerWrittenForSaleOutbound() {
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");
        mapper.costLedgers.clear();

        service.applySaleOutbound(T1, 1L, 100L, 10, false, 8001L, "bob");

        assertEquals(1, mapper.costLedgers.size());
        FinStockCostLedger cl = mapper.costLedgers.get(0);
        assertEquals("SALE", cl.getSourceType());
        assertEquals("COST_OUT", cl.getCostChangeType());
        assertEquals(-10, cl.getQuantity(), "成本流水数量按正增负减记录");
        assertEquals(0, new BigDecimal("16.666667").compareTo(cl.getUnitCost()));
        assertEquals(0, new BigDecimal("166.67").compareTo(cl.getAmount()));
    }

    @Test
    void versionConflict_throws() {
        service.applyPurchaseInbound(T1, 1L, 100L, 12, new BigDecimal("200.00"), 9001L, "alice");
        // 模拟乐观锁冲突：下次 updateCostLayer 返回 0
        mapper.forceVersionConflict = true;

        assertThrows(ServiceException.class,
                () -> service.applyPurchaseInbound(T1, 1L, 100L, 5, new BigDecimal("100.00"), 9002L, "alice"),
                "乐观锁冲突应回滚并抛出异常");
    }

    @Test
    void purchaseReversalExceedingStock_throws() {
        service.applyPurchaseInbound(T1, 1L, 100L, 5, new BigDecimal("100.00"), 9001L, "alice");

        assertThrows(ServiceException.class,
                () -> service.reversePurchaseInbound(T1, 1L, 100L, 10, 9001L, "alice"),
                "采购冲销量超过库存量应拒绝");
    }

    /** 手写 fake mapper：内存维护成本层与成本流水，键含 tenantId。 */
    static class FakeCostLayerMapper implements FinStockCostLayerMapper {
        final Map<String, FinStockCostLayer> layers = new HashMap<>();
        final List<FinStockCostLedger> costLedgers = new ArrayList<>();
        boolean forceVersionConflict = false;
        private long idSeq = 0;

        private String key(Long tenantId, Long deptId, Long productId) {
            return tenantId + ":" + deptId + ":" + productId;
        }

        FinStockCostLayer costLayer(Long tenantId, Long deptId, Long productId) {
            return layers.get(key(tenantId, deptId, productId));
        }

        @Override
        public int insertCostLayerIfAbsent(Long tenantId, Long deptId, Long productId) {
            String k = key(tenantId, deptId, productId);
            if (!layers.containsKey(k)) {
                FinStockCostLayer layer = new FinStockCostLayer();
                layer.setCostLayerId(++idSeq);
                layer.setTenantId(tenantId);
                layer.setDeptId(deptId);
                layer.setProductId(productId);
                layer.setAvgUnitCost(BigDecimal.ZERO);
                layer.setStockQuantity(0);
                layer.setStockAmount(BigDecimal.ZERO);
                layer.setVersion(0);
                layers.put(k, layer);
            }
            return 1;
        }

        @Override
        public FinStockCostLayer selectCostLayerForUpdate(Long tenantId, Long deptId, Long productId) {
            // 返回内存对象的副本，模拟行锁后的快照
            FinStockCostLayer src = layers.get(key(tenantId, deptId, productId));
            if (src == null) return null;
            FinStockCostLayer copy = new FinStockCostLayer();
            copy.setCostLayerId(src.getCostLayerId());
            copy.setTenantId(src.getTenantId());
            copy.setDeptId(src.getDeptId());
            copy.setProductId(src.getProductId());
            copy.setAvgUnitCost(src.getAvgUnitCost());
            copy.setStockQuantity(src.getStockQuantity());
            copy.setStockAmount(src.getStockAmount());
            copy.setVersion(src.getVersion());
            return copy;
        }

        @Override
        public int updateCostLayer(Long tenantId, Long deptId, Long productId,
                                   BigDecimal avgUnitCost, Integer stockQuantity,
                                   BigDecimal stockAmount, Integer version, String updateBy) {
            if (forceVersionConflict) {
                return 0;
            }
            FinStockCostLayer layer = layers.get(key(tenantId, deptId, productId));
            if (layer == null || layer.getVersion() != version) {
                return 0;
            }
            layer.setAvgUnitCost(avgUnitCost);
            layer.setStockQuantity(stockQuantity);
            layer.setStockAmount(stockAmount);
            layer.setVersion(version + 1);
            return 1;
        }

        @Override
        public int insertCostLedger(FinStockCostLedger costLedger) {
            costLedger.setCostLedgerId(++idSeq);
            costLedgers.add(costLedger);
            return 1;
        }

        @Override
        public FinStockCostLedger selectCostLedgerById(Long costLedgerId) {
            return costLedgers.stream()
                    .filter(c -> costLedgerId.equals(c.getCostLedgerId()))
                    .findFirst().orElse(null);
        }
    }
}
