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
    private static final String SOURCE_ADJUST = "ADJUST";
    private static final String SOURCE_STOCKTAKE = "STOCKTAKE";
    private static final String COST_IN = "COST_IN";
    private static final String COST_OUT = "COST_OUT";
    private static final String COST_REVERSE_IN = "COST_REVERSE_IN";
    private static final String COST_REVERSE_OUT = "COST_REVERSE_OUT";
    private static final String COST_ADJUST = "COST_ADJUST";
    private static final String STOCKTAKE_LOSS_OUT = "STOCKTAKE_LOSS_OUT";
    private static final String STOCKTAKE_GAIN_IN = "STOCKTAKE_GAIN_IN";

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

    @Override
    public void applyCostAdjustment(Long tenantId, Long deptId, Long productId,
                                    BigDecimal amount, String reason, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (amount == null || amount.signum() == 0) {
            throw new ServiceException("成本调整金额不能为零");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ServiceException("成本调整必须填写原因");
        }
        BigDecimal adjustAmount = amount.setScale(AMOUNT_SCALE, ROUNDING);

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty = layer.getStockQuantity(); // 调整不改变数量
        BigDecimal newAmount = layer.getStockAmount().add(adjustAmount).setScale(AMOUNT_SCALE, ROUNDING);
        if (newAmount.signum() < 0) {
            newAmount = BigDecimal.ZERO.setScale(AMOUNT_SCALE, ROUNDING);
        }
        BigDecimal newAvg = computeAvg(newAmount, newQty);

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        writeAdjustCostLedger(tenantId, deptId, productId, adjustAmount, newAvg, reason, operator);
    }

    @Override
    public Long applyStocktakeLoss(Long tenantId, Long deptId, Long productId,
                                   int quantity, Long sourceLedgerId, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (quantity <= 0) {
            throw new ServiceException("盘亏数量必须为正数");
        }

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty = layer.getStockQuantity() - quantity;
        if (newQty < 0) {
            // 盘亏超过库存：说明盘点数据或库存数据异常，拒绝过账
            throw new ServiceException("盘亏数量超过库存（当前 " + layer.getStockQuantity()
                    + "，盘亏 " + quantity + "），拒绝过账");
        }

        BigDecimal solidifiedCost = layer.getAvgUnitCost();
        BigDecimal lossAmount = solidifiedCost.multiply(BigDecimal.valueOf(quantity))
                .setScale(AMOUNT_SCALE, ROUNDING);
        BigDecimal newAmount = layer.getStockAmount().subtract(lossAmount).setScale(AMOUNT_SCALE, ROUNDING);
        if (newAmount.signum() < 0) {
            newAmount = BigDecimal.ZERO.setScale(AMOUNT_SCALE, ROUNDING);
        }
        // 盘亏不改变平均成本（与销售出库一致）
        BigDecimal newAvg = solidifiedCost;

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        return writeCostLedger(tenantId, deptId, productId, SOURCE_STOCKTAKE, sourceLedgerId,
                               STOCKTAKE_LOSS_OUT, -quantity, solidifiedCost, lossAmount, operator);
    }

    @Override
    public Long applyStocktakeGain(Long tenantId, Long deptId, Long productId,
                                   int quantity, BigDecimal amount,
                                   Long sourceLedgerId, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (quantity <= 0) {
            throw new ServiceException("盘盈数量必须为正数");
        }

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        // amount=null 时按当前平均成本 × 数量计算入账金额（默认估值）
        BigDecimal gainAmount;
        if (amount == null) {
            gainAmount = layer.getAvgUnitCost().multiply(BigDecimal.valueOf(quantity))
                    .setScale(AMOUNT_SCALE, ROUNDING);
        } else {
            gainAmount = amount.setScale(AMOUNT_SCALE, ROUNDING);
        }
        if (gainAmount.signum() < 0) {
            throw new ServiceException("盘盈金额不能为负数");
        }

        int newQty = layer.getStockQuantity() + quantity;
        BigDecimal newAmount = layer.getStockAmount().add(gainAmount).setScale(AMOUNT_SCALE, ROUNDING);
        // 盘盈视同入库，重新计算平均成本
        BigDecimal newAvg = computeAvg(newAmount, newQty);

        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);
        return writeCostLedger(tenantId, deptId, productId, SOURCE_STOCKTAKE, sourceLedgerId,
                               STOCKTAKE_GAIN_IN, quantity, newAvg, gainAmount, operator);
    }

    @Override
    public Long reverseStocktakeAdjustment(Long tenantId, Long deptId, Long productId,
                                           int quantity, BigDecimal unitCost,
                                           Long sourceLedgerId, Long originalCostLedgerId,
                                           String operator) {
        assertTenantScope(tenantId, deptId, productId);
        if (quantity <= 0) {
            throw new ServiceException("冲销数量必须为正数");
        }
        if (unitCost == null || unitCost.signum() < 0) {
            throw new ServiceException("冲销缺少原固化成本，拒绝回补");
        }
        if (originalCostLedgerId == null) {
            throw new ServiceException("冲销必须提供原成本流水ID以追溯方向");
        }

        FinStockCostLedger original = costLayerMapper.selectCostLedgerById(originalCostLedgerId);
        if (original == null) {
            throw new ServiceException("原成本流水不存在: " + originalCostLedgerId);
        }
        if (!tenantId.equals(original.getTenantId())
                || !deptId.equals(original.getDeptId())
                || !productId.equals(original.getProductId())) {
            throw new ServiceException("原成本流水租户/门店/商品不匹配，拒绝冲销");
        }

        String originalType = original.getCostChangeType();
        boolean reverseLoss = STOCKTAKE_LOSS_OUT.equals(originalType);
        boolean reverseGain = STOCKTAKE_GAIN_IN.equals(originalType);
        if (!reverseLoss && !reverseGain) {
            throw new ServiceException("原成本流水类型非盘点调整: " + originalType + "，拒绝冲销");
        }

        BigDecimal originalCost = unitCost.setScale(UNIT_COST_SCALE, ROUNDING);
        BigDecimal reverseAmount = originalCost.multiply(BigDecimal.valueOf(quantity))
                .setScale(AMOUNT_SCALE, ROUNDING);

        costLayerMapper.insertCostLayerIfAbsent(tenantId, deptId, productId);
        FinStockCostLayer layer = costLayerMapper.selectCostLayerForUpdate(tenantId, deptId, productId);
        if (layer == null) {
            throw new ServiceException("成本层行创建失败，拒绝处理");
        }

        int newQty;
        BigDecimal newAmount;
        String changeType;

        if (reverseLoss) {
            // 原盘亏 → 冲销恢复库存与金额
            newQty = layer.getStockQuantity() + quantity;
            newAmount = layer.getStockAmount().add(reverseAmount).setScale(AMOUNT_SCALE, ROUNDING);
            changeType = COST_REVERSE_IN;
        } else {
            // 原盘盈 → 冲销扣减库存与金额
            newQty = layer.getStockQuantity() - quantity;
            if (newQty < 0) {
                throw new ServiceException("冲销盘盈后库存为负（当前 " + layer.getStockQuantity()
                        + "，冲销 " + quantity + "），拒绝冲销");
            }
            newAmount = layer.getStockAmount().subtract(reverseAmount).setScale(AMOUNT_SCALE, ROUNDING);
            if (newAmount.signum() < 0) {
                newAmount = BigDecimal.ZERO.setScale(AMOUNT_SCALE, ROUNDING);
            }
            changeType = COST_REVERSE_OUT;
        }

        BigDecimal newAvg = computeAvg(newAmount, newQty);
        updateLayerWithVersion(layer, newAvg, newQty, newAmount, operator);

        int signedQty = reverseLoss ? quantity : -quantity;
        return writeCostLedger(tenantId, deptId, productId, SOURCE_STOCKTAKE, sourceLedgerId,
                               changeType, signedQty, originalCost, reverseAmount, operator);
    }

    @Override
    public BigDecimal getCostLedgerUnitCost(Long tenantId, Long costLedgerId) {
        if (tenantId == null || costLedgerId == null) {
            return null;
        }
        FinStockCostLedger ledger = costLayerMapper.selectCostLedgerById(costLedgerId);
        if (ledger == null) {
            return null;
        }
        // 租户隔离校验：防止跨租户读取成本流水
        if (!tenantId.equals(ledger.getTenantId())) {
            return null;
        }
        // 类型校验：仅盘点调整流水可被冲销引用
        String type = ledger.getCostChangeType();
        if (!STOCKTAKE_LOSS_OUT.equals(type) && !STOCKTAKE_GAIN_IN.equals(type)) {
            return null;
        }
        return ledger.getUnitCost();
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

    private Long writeCostLedger(Long tenantId, Long deptId, Long productId,
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
        return cl.getCostLedgerId();
    }

    private void writeAdjustCostLedger(Long tenantId, Long deptId, Long productId,
                                       BigDecimal amount, BigDecimal avgUnitCost,
                                       String reason, String operator) {
        FinStockCostLedger cl = new FinStockCostLedger();
        cl.setTenantId(tenantId);
        cl.setDeptId(deptId);
        cl.setProductId(productId);
        cl.setSourceType(SOURCE_ADJUST);
        cl.setSourceLedgerId(null);
        cl.setCostChangeType(COST_ADJUST);
        cl.setQuantity(0); // 调整不改变数量
        cl.setUnitCost(avgUnitCost.setScale(UNIT_COST_SCALE, ROUNDING));
        cl.setAmount(amount.setScale(AMOUNT_SCALE, ROUNDING));
        cl.setAdjustReason(reason);
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
