package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.redis.service.RedisService;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IFinStockLedgerService;

/**
 * 库存流水写入服务实现。
 *
 * 采用"当前库存表 fin_stock_position 行锁 + 差额对账"模型：
 * 1. INSERT IGNORE 保证 position 行存在（首笔并发安全）
 * 2. SELECT ... FOR UPDATE 长事务锁行（并发序列化）
 * 3. 计算 delta = target - 已记录净额
 * 4. delta != 0 才写流水，天然幂等
 * 5. 更新 position 到新结存
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

    private Boolean allowNegativeSaleOutOverride;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                       String referenceNo, Integer targetQuantity, BigDecimal unitCost, String operator) {
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
        writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    unitCost, REF_PURCHASE, referenceId, referenceNo, operator);
        int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
        assertPositionUpdated(affected);
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
        if (delta < 0) {
            changeType = SALE_OUT;
        } else {
            changeType = SALE_REVERSE;
        }
        writeLedger(tenantId, deptId, productId, productName, changeType, delta, current, after,
                    null, REF_SALE, referenceId, referenceNo, operator);
        int affected = finStockLedgerMapper.updatePositionQuantity(tenantId, deptId, productId, after);
        assertPositionUpdated(affected);
    }

    private void writeLedger(Long tenantId, Long deptId, Long productId, String productName, String changeType,
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
        ledger.setCreateBy(operator);
        finStockLedgerMapper.insertFinStockLedger(ledger);
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
