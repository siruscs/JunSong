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
                                       String referenceNo, BigDecimal targetQuantity, BigDecimal unitCost, String operator) {
        reconcilePurchaseStock(tenantId, deptId, productId, productName, referenceId, referenceNo,
                               targetQuantity, unitCost, null, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                       String referenceNo, BigDecimal targetQuantity, BigDecimal unitCost,
        BigDecimal inboundAmount, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        assertQuantityNotNull(targetQuantity);
        finStockLedgerMapper.insertPositionIfAbsent(tenantId, deptId, productId);
        BigDecimal current = nz(finStockLedgerMapper.selectPositionQuantityForUpdate(tenantId, deptId, productId));
        BigDecimal recorded = nz(finStockLedgerMapper.sumRecordedNet(tenantId, REF_PURCHASE, referenceId, productId));
        BigDecimal delta = targetQuantity.subtract(recorded);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String changeType = delta.compareTo(BigDecimal.ZERO) > 0 ? PURCHASE_IN : PURCHASE_REVERSE;
        BigDecimal after = current.add(delta);
        Long ledgerId = writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    unitCost, REF_PURCHASE, referenceId, referenceNo, String.valueOf(targetQuantity), operator);
        int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
        assertPositionUpdated(affected);

        if (stockCostService != null) {
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal amount = inboundAmount != null
                        ? inboundAmount
                        : (unitCost != null ? unitCost.multiply(delta) : BigDecimal.ZERO);
                stockCostService.applyPurchaseInbound(tenantId, deptId, productId, delta, amount, ledgerId, operator);
            } else {
                stockCostService.reversePurchaseInbound(tenantId, deptId, productId, delta.negate(), ledgerId, operator);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileSaleStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                   String referenceNo, BigDecimal targetQuantity, String operator) {
        assertTenantScope(tenantId, deptId, productId);
        assertQuantityPresent(targetQuantity);
        finStockLedgerMapper.insertPositionIfAbsent(tenantId, deptId, productId);
        BigDecimal current = nz(finStockLedgerMapper.selectPositionQuantityForUpdate(tenantId, deptId, productId));
        BigDecimal recorded = nz(finStockLedgerMapper.sumRecordedNet(tenantId, REF_SALE, referenceId, productId));
        BigDecimal targetNet = targetQuantity.negate();
        BigDecimal delta = targetNet.subtract(recorded);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal after = current.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0 && !isAllowNegativeSaleOut()) {
            throw new ServiceException("库存不足，无法出库：当前库存 " + current
                    + "，需出库 " + (targetQuantity.compareTo(BigDecimal.ZERO) > 0 ? targetQuantity : delta.abs()));
        }

        String changeType;
        BigDecimal solidifiedCost = null;
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            changeType = SALE_OUT;
            Long ledgerId = writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    null, REF_SALE, referenceId, referenceNo, String.valueOf(targetNet), operator);
            int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
            assertPositionUpdated(affected);
            if (stockCostService != null) {
                solidifiedCost = stockCostService.applySaleOutbound(tenantId, deptId, productId,
                        delta.negate(), isAllowNegativeSaleOut(), ledgerId, operator);
                int costAffected = finStockLedgerMapper.updateLedgerUnitCost(ledgerId, solidifiedCost);
                if (costAffected != 1) {
                    throw new ServiceException("销售出库固化成本回填失败，拒绝回写库存流水");
                }
            }
            return;
        } else {
            changeType = SALE_REVERSE;
            if (stockCostService != null) {
                BigDecimal originalCost = finStockLedgerMapper.selectSaleOutUnitCost(tenantId, referenceId, productId);
                if (originalCost == null) {
                    if (targetQuantity != null && targetQuantity.compareTo(BigDecimal.ZERO) < 0) {
                        originalCost = BigDecimal.ZERO;
                    } else {
                        throw new ServiceException("销售冲销找不到原 SALE_OUT 固化成本，拒绝回写（referenceId="
                                + referenceId + ", productId=" + productId + "）");
                    }
                }
                solidifiedCost = originalCost;
                Long ledgerId = writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                        solidifiedCost, REF_SALE, referenceId, referenceNo, String.valueOf(targetNet), operator);
                int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
                assertPositionUpdated(affected);
                stockCostService.reverseSaleOutbound(tenantId, deptId, productId, delta, originalCost,
                        ledgerId, operator);
                return;
            }
        }
        writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    solidifiedCost, REF_SALE, referenceId, referenceNo, String.valueOf(targetNet), operator);
        int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
        assertPositionUpdated(affected);
    }

    private Long writeLedger(Long tenantId, Long deptId, Long productId, String productName, String changeType,
                             BigDecimal delta, BigDecimal before, BigDecimal after, BigDecimal unitCost,
                             String refType, Long refId, String refNo, String targetNetKey, String operator) {
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
        if (refType != null && refId != null && productId != null) {
            ledger.setIdempotencyKey(refType + ":" + refId + ":" + productId + ":" + targetNetKey);
        }
        ledger.setDelFlag("0");
        ledger.setCreateBy(resolveOperator(operator));
        int inserted = finStockLedgerMapper.insertFinStockLedger(ledger);
        if (inserted == 1 && ledger.getLedgerId() != null) {
            return ledger.getLedgerId();
        }
        if (StringUtils.isNotBlank(ledger.getIdempotencyKey())) {
            FinStockLedger existing = finStockLedgerMapper.selectByIdempotencyKey(tenantId, ledger.getIdempotencyKey());
            if (existing != null && existing.getLedgerId() != null) {
                return existing.getLedgerId();
            }
        }
        throw new ServiceException("库存流水写入失败，且未找到已存在幂等流水");
    }

    private String resolveOperator(String operator) {
        if (StringUtils.isNotBlank(operator)) {
            return operator;
        }
        return SecurityUtils.getUsername();
    }

    private void assertQuantityNotNull(BigDecimal quantity) {
        if (quantity == null) {
            throw new ServiceException("对账目标数量不能为空");
        }
    }

    private void assertQuantityPresent(BigDecimal quantity) {
        if (quantity == null) {
            throw new ServiceException("目标数量不能为空");
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

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
