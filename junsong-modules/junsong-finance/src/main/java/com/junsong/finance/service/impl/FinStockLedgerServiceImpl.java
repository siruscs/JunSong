package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IFinStockLedgerService;
import com.junsong.finance.service.IStockCostService;

/**
 * 库存流水写入服务实现。
 *
 * 采用"当前库存表 fin_stock_position 行锁 + 差额对账"模型：
 * 1. INSERT IGNORE 保证 position 行存在（首笔并发安全）
 * 2. SELECT ... FOR UPDATE 长事务锁行（并发序列化）
 * 3. 计算 delta = target - 已记录净额
 * 4. delta != 0 才写流水，天然幂等
 * 5. 更新 position 到新结存
 * 6. 联动 IStockCostService（如果注入）更新移动加权平均成本
 *
 * @author junsong
 */
@Service
public class FinStockLedgerServiceImpl implements IFinStockLedgerService {

    private static final String PURCHASE_IN = "PURCHASE_IN";
    private static final String PURCHASE_REVERSE = "PURCHASE_REVERSE";
    private static final String SALE_OUT = "SALE_OUT";
    private static final String SALE_REVERSE = "SALE_REVERSE";
    private static final String REF_PURCHASE = "PURCHASE";
    private static final String REF_SALE = "SALE";
    private static final String ALLOW_NEGATIVE_SALE_OUT_KEY = "finance.stock.allowNegativeSaleOut";

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired(required = false)
    private RedisService redisService;

    @Autowired(required = false)
    private IStockCostService stockCostService;

    private Boolean allowNegativeSaleOutOverride;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                       String referenceNo, Integer targetQuantity, BigDecimal unitCost, String operator) {
        reconcilePurchaseStock(tenantId, deptId, productId, productName, referenceId, referenceNo,
                               targetQuantity, unitCost, null, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                       String referenceNo, Integer targetQuantity, BigDecimal unitCost,
                                       BigDecimal inboundAmount, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        assertNonNegative(targetQuantity);
        finStockLedgerMapper.insertPositionIfAbsent(tenantId, deptId, productId);
        int current = nz(finStockLedgerMapper.selectPositionQuantityForUpdate(tenantId, deptId, productId));
        int recorded = nz(finStockLedgerMapper.sumRecordedNet(tenantId, REF_PURCHASE, referenceId, productId));
        int delta = targetQuantity - recorded;
        if (delta == 0) {
            return;
        }

        String changeType = delta > 0 ? PURCHASE_IN : PURCHASE_REVERSE;
        int after = current + delta;
        // 先写库存流水，获取流水主键 ledgerId 作为成本流水的 source_ledger_id
        Long ledgerId = writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    unitCost, REF_PURCHASE, referenceId, referenceNo, operator);
        int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
        assertPositionUpdated(affected);

        // 联动成本层：source_ledger_id 关联 fin_stock_ledger.ledger_id 保证可追溯
        if (stockCostService != null) {
            if (delta > 0) {
                BigDecimal amount = inboundAmount != null
                        ? inboundAmount
                        : (unitCost != null ? unitCost.multiply(BigDecimal.valueOf(delta)) : BigDecimal.ZERO);
                stockCostService.applyPurchaseInbound(tenantId, deptId, productId, delta, amount, ledgerId, operator);
            } else {
                stockCostService.reversePurchaseInbound(tenantId, deptId, productId, -delta, ledgerId, operator);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileSaleStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                   String referenceNo, Integer targetQuantity, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        assertNonNegative(targetQuantity);
        finStockLedgerMapper.insertPositionIfAbsent(tenantId, deptId, productId);
        int current = nz(finStockLedgerMapper.selectPositionQuantityForUpdate(tenantId, deptId, productId));
        int recorded = nz(finStockLedgerMapper.sumRecordedNet(tenantId, REF_SALE, referenceId, productId));
        int targetNet = -targetQuantity;
        int delta = targetNet - recorded;
        if (delta == 0) {
            return;
        }

        int after = current + delta;
        if (after < 0 && !isAllowNegativeSaleOut()) {
            throw new ServiceException("库存不足，无法出库：当前库存 " + current
                    + "，需出库 " + (targetQuantity > 0 ? targetQuantity : Math.abs(delta)));
        }

        String changeType;
        BigDecimal solidifiedCost = null;
        if (delta < 0) {
            changeType = SALE_OUT;
            // 销售出库：先写流水（unit_cost 暂空），获取 ledgerId 后固化成本并回填 unit_cost
            Long ledgerId = writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    null, REF_SALE, referenceId, referenceNo, operator);
            int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
            assertPositionUpdated(affected);
            if (stockCostService != null) {
                solidifiedCost = stockCostService.applySaleOutbound(tenantId, deptId, productId,
                        -delta, isAllowNegativeSaleOut(), ledgerId, operator);
                // 回填固化成本到流水 unit_cost 字段，供后续冲销追溯
                int costAffected = finStockLedgerMapper.updateLedgerUnitCost(ledgerId, solidifiedCost);
                if (costAffected != 1) {
                    throw new ServiceException("销售出库固化成本回填失败，拒绝回写库存流水");
                }
            }
            return;
        } else {
            changeType = SALE_REVERSE;
            // 销售冲销：查询原 SALE_OUT 固化成本，按原成本回补成本层
            if (stockCostService != null) {
                BigDecimal originalCost = finStockLedgerMapper.selectSaleOutUnitCost(tenantId, referenceId, productId);
                if (originalCost == null) {
                    // fail-closed：找不到原固化成本则拒绝冲销，防止数量回补但成本金额未回补导致价值报表失真
                    throw new ServiceException("销售冲销找不到原 SALE_OUT 固化成本，拒绝回写（referenceId="
                            + referenceId + ", productId=" + productId + "）");
                }
                solidifiedCost = originalCost;
                // 先写流水获取 ledgerId，再以 ledgerId 作为 source_ledger_id 调用成本冲销
                Long ledgerId = writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                        solidifiedCost, REF_SALE, referenceId, referenceNo, operator);
                int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
                assertPositionUpdated(affected);
                stockCostService.reverseSaleOutbound(tenantId, deptId, productId, delta, originalCost,
                        ledgerId, operator);
                return;
            }
        }
        // stockCostService 未注入时走原有路径（Phase 1 兼容）
        writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    solidifiedCost, REF_SALE, referenceId, referenceNo, operator);
        int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
        assertPositionUpdated(affected);
    }

    private Long writeLedger(Long tenantId, Long deptId, Long productId, String productName, String changeType,
                             int delta, int before, int after, BigDecimal unitCost,
                             String refType, Long refId, String refNo, String operator) {
        FinStockLedger ledger = new FinStockLedger();
        ledger.setTenantId(tenantId);
        ledger.setDeptId(deptId);
        ledger.setProductId(productId);
        ledger.setProductName(productName);
        ledger.setChangeType(changeType);
        ledger.setChangeQuantity(delta);
        ledger.setBeforeQuantity(before);
        ledger.setAfterQuantity(after);
        ledger.setUnitCost(unitCost);
        ledger.setReferenceType(refType);
        ledger.setReferenceId(refId);
        ledger.setReferenceNo(refNo);
        ledger.setDelFlag("0");
        ledger.setCreateBy(resolveOperator(operator));
        finStockLedgerMapper.insertFinStockLedger(ledger);
        return ledger.getLedgerId();
    }

    private String resolveOperator(String operator) {
        if (StringUtils.isNotBlank(operator)) {
            return operator;
        }
        return SecurityUtils.getUsername();
    }

    private void assertNonNegative(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new ServiceException("目标数量不能为负数");
        }
    }

    private void assertTenantScope(Long tenantId, Long deptId, Long productId) {
        if (tenantId == null || deptId == null || productId == null) {
            throw new ServiceException("库存对账缺少租户/门店/商品上下文，拒绝处理");
        }
    }

    private void assertPositionUpdated(int affected) {
        if (affected != 1) {
            throw new ServiceException("库存结存更新影响行数异常（" + affected + "），事务回滚");
        }
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }

    private boolean isAllowNegativeSaleOut() {
        if (allowNegativeSaleOutOverride != null) {
            return allowNegativeSaleOutOverride;
        }
        if (redisService == null) {
            return false;
        }
        String value = redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + ALLOW_NEGATIVE_SALE_OUT_KEY);
        return "true".equalsIgnoreCase(value);
    }

    void setAllowNegativeSaleOutForTest(boolean allowNegativeSaleOut) {
        this.allowNegativeSaleOutOverride = allowNegativeSaleOut;
    }
}
