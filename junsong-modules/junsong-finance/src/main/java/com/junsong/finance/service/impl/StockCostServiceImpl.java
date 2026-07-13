package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinStockCostLayer;
import com.junsong.finance.domain.FinStockCostLedger;
import com.junsong.finance.mapper.FinStockCostLayerMapper;
import com.junsong.finance.service.IStockCostService;

/**
 * 库存成本计价服务实现（移动加权平均法）。
 *
 * 所有成本读写与 fin_stock_position 使用相同租户键和确定锁顺序：
 * 1. INSERT IGNORE 保证成本层行存在
 * 2. SELECT ... FOR UPDATE 锁行
 * 3. 计算新平均成本/数量/金额
 * 4. 乐观锁更新（version + 1）
 * 5. 写入成本流水（不可变审计记录）
 *
 * BigDecimal 精度：单位成本 scale 6，金额 scale 2，均 HALF_UP。
 * 销售成本取出库瞬间固化成本，出库后平均成本不变；销售冲销按原固化成本回补。
 *
 * @author junsong
 */
@Service
public class StockCostServiceImpl implements IStockCostService {

    private static final int UNIT_COST_SCALE = 6;
    private static final int AMOUNT_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final String SOURCE_PURCHASE = "PURCHASE";
    private static final String SOURCE_SALE = "SALE";
    private static final String COST_IN = "COST_IN";
    private static final String COST_OUT = "COST_OUT";
    private static final String COST_REVERSE_IN = "COST_REVERSE_IN";
    private static final String COST_REVERSE_OUT = "COST_REVERSE_OUT";

    @Autowired
    private FinStockCostLayerMapper costLayerMapper;

    @Override
    public void applyPurchaseInbound(Long tenantId, Long deptId, Long productId,
                                     int quantity, BigDecimal amount,
                                     Long sourceLedgerId, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (quantity <= 0) {
            throw new ServiceException("采购入库数量必须为正数");
        }
        BigDecimal inboundAmount = nz(amount).setScale(AMOUNT_SCALE, ROUNDING);

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty = layer.getStockQuantity() + quantity;
        BigDecimal newAmount = layer.getStockAmount().add(inboundAmount).setScale(AMOUNT_SCALE, ROUNDING);
        BigDecimal newAvg = computeAvg(newAmount, newQty);

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        writeCostLedger(tenantId, deptId, productId, SOURCE_PURCHASE, sourceLedgerId, COST_IN,
                        quantity, newAvg, inboundAmount, operator);
    }

    @Override
    public void reversePurchaseInbound(Long tenantId, Long deptId, Long productId,
                                       int reverseQuantity,
                                       Long sourceLedgerId, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (reverseQuantity <= 0) {
            throw new ServiceException("采购冲销数量必须为正数");
        }

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty = layer.getStockQuantity() - reverseQuantity;
        if (newQty < 0) {
            throw new ServiceException("采购冲销量超过库存量，拒绝逆转成本层（当前 "
                    + layer.getStockQuantity() + "，冲销 " + reverseQuantity + "）");
        }
        BigDecimal reverseAmount = layer.getAvgUnitCost()
                .multiply(BigDecimal.valueOf(reverseQuantity))
                .setScale(AMOUNT_SCALE, ROUNDING);
        BigDecimal newAmount = layer.getStockAmount().subtract(reverseAmount).setScale(AMOUNT_SCALE, ROUNDING);
        if (newAmount.signum() < 0) {
            newAmount = BigDecimal.ZERO.setScale(AMOUNT_SCALE, ROUNDING);
        }
        BigDecimal newAvg = computeAvg(newAmount, newQty);

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        writeCostLedger(tenantId, deptId, productId, SOURCE_PURCHASE, sourceLedgerId, COST_REVERSE_OUT,
                        -reverseQuantity, layer.getAvgUnitCost(), reverseAmount, operator);
    }

    @Override
    public BigDecimal applySaleOutbound(Long tenantId, Long deptId, Long productId,
                                        int quantity, boolean allowNegative,
                                        Long sourceLedgerId, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (quantity <= 0) {
            throw new ServiceException("销售出库数量必须为正数");
        }

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty = layer.getStockQuantity() - quantity;
        if (newQty < 0 && !allowNegative) {
            throw new ServiceException("库存不足，无法出库：当前库存 " + layer.getStockQuantity()
                    + "，需出库 " + quantity);
        }

        BigDecimal solidifiedCost = layer.getAvgUnitCost();
        BigDecimal saleCost = solidifiedCost.multiply(BigDecimal.valueOf(quantity))
                .setScale(AMOUNT_SCALE, ROUNDING);
        BigDecimal newAmount = layer.getStockAmount().subtract(saleCost).setScale(AMOUNT_SCALE, ROUNDING);
        // 销售出库不改变平均成本（移动加权平均法）
        BigDecimal newAvg = solidifiedCost;

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        writeCostLedger(tenantId, deptId, productId, SOURCE_SALE, sourceLedgerId, COST_OUT,
                        -quantity, solidifiedCost, saleCost, operator);
        return solidifiedCost;
    }

    @Override
    public void reverseSaleOutbound(Long tenantId, Long deptId, Long productId,
                                    int quantity, BigDecimal originalUnitCost,
                                    Long sourceLedgerId, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (quantity <= 0) {
            throw new ServiceException("销售冲销数量必须为正数");
        }
        if (originalUnitCost == null || originalUnitCost.signum() < 0) {
            throw new ServiceException("销售冲销缺少原固化成本，拒绝回补");
        }
        BigDecimal originalCost = originalUnitCost.setScale(UNIT_COST_SCALE, ROUNDING);

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty = layer.getStockQuantity() + quantity;
        BigDecimal restoreAmount = originalCost.multiply(BigDecimal.valueOf(quantity))
                .setScale(AMOUNT_SCALE, ROUNDING);
        BigDecimal newAmount = layer.getStockAmount().add(restoreAmount).setScale(AMOUNT_SCALE, ROUNDING);
        // 销售冲销视同入库，重新计算平均成本
        BigDecimal newAvg = computeAvg(newAmount, newQty);

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        writeCostLedger(tenantId, deptId, productId, SOURCE_SALE, sourceLedgerId, COST_REVERSE_IN,
                        quantity, originalCost, restoreAmount, operator);
    }

    private BigDecimal computeAvg(BigDecimal amount, int qty) {
        if (qty == 0) {
            return BigDecimal.ZERO.setScale(UNIT_COST_SCALE, ROUNDING);
        }
        return amount.divide(BigDecimal.valueOf(qty), UNIT_COST_SCALE, ROUNDING);
    }

    private void updateLayerWithVersion(FinStockCostLayer layer, BigDecimal avg, int qty,
                                        BigDecimal amount, String operator) {
        int affected = costLayerMapper.updateCostLayer(
                layer.getTenantId(), layer.getDeptId(), layer.getProductId(),
                avg, qty, amount, layer.getVersion(), operator);
        if (affected != 1) {
            throw new ServiceException("成本层乐观锁冲突，事务回滚（当前版本 " + layer.getVersion() + "）");
        }
    }

    private void writeCostLedger(Long tenantId, Long deptId, Long productId,
                                 String sourceType, Long sourceLedgerId, String costChangeType,
                                 int quantity, BigDecimal unitCost, BigDecimal amount,
                                 String operator) {
        FinStockCostLedger cl = new FinStockCostLedger();
        cl.setTenantId(tenantId);
        cl.setDeptId(deptId);
        cl.setProductId(productId);
        cl.setSourceType(sourceType);
        cl.setSourceLedgerId(sourceLedgerId);
        cl.setCostChangeType(costChangeType);
        cl.setQuantity(quantity);
        cl.setUnitCost(unitCost.setScale(UNIT_COST_SCALE, ROUNDING));
        cl.setAmount(amount.setScale(AMOUNT_SCALE, ROUNDING));
        cl.setOperator(operator);
        cl.setDelFlag("0");
        cl.setCreateBy(operator);
        costLayerMapper.insertCostLedger(cl);
    }

    private void assertTenantScope(Long tenantId, Long deptId, Long productId) {
        if (tenantId == null || deptId == null || productId == null) {
            throw new ServiceException("成本计价缺少租户/门店/商品上下文，拒绝处理");
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
